# 数据平面：向量与图存储

本文档说明 data-plane 如何将代码解析结果分别持久化到 Qdrant（向量检索）和 Neo4j（知识图谱），以及相关的配置、接口与验证方法。

## 1. 功能概述

`ParseApplicationService.parseAndIndex(...)` 在调用 Parse Worker 拿到 `CodeElement` 列表后，会同步完成两件事：

1. **向量存储**：把每个 `CodeElement` 的嵌入向量写入 Qdrant，payload 携带元信息（仓库、类型、名称、路径、行号、源码、文档等）。
2. **图存储**：把每个 `CodeElement` 作为节点写入 Neo4j，并在元素之间建立 `RELATES_TO` 关系。

两个存储相互独立，失败时互不影响；但任意一方失败都会让本次解析接口返回错误。

## 2. 架构与数据流

```
+---------------+     gRPC     +----------------+     +------------------+
|  data-plane   |  --------->  |  java-parser   |     |  csharp-parser   |
|  /api/data/parse|            |   (port 9093)  |     |   (port 9094)    |
+---------------+              +----------------+     +------------------+
        |
        | List<CodeElement>
        v
+-------------------+      +--------------------+
|  VectorStoreClient| ---> |  Qdrant (gRPC 6334)|
|  (embedding +     |      +--------------------+
|   upsert)         |
+-------------------+
        |
        v
+-------------------+      +--------------------+
|  GraphStoreClient | ---> |  Neo4j (Bolt 7687) |
|  (MERGE nodes/rel)|      +--------------------+
+-------------------+
```

## 3. 核心组件

### 3.1 向量存储

| 文件 | 职责 |
|---|---|
| `data-plane/data-infrastructure/.../vector/EmbeddingGenerator.java:3` | 嵌入生成器接口：`embed(String)` / `dimension()` |
| `data-plane/data-infrastructure/.../vector/TokenHashEmbeddingGenerator.java:12` | 基于 token SHA-256 hash 的 fallback 实现，维度 384，输出已归一化 |
| `data-plane/data-infrastructure/.../vector/VectorStoreClient.java:23` | 封装 Qdrant upsert / delete / 自动建 collection |
| `data-plane/data-infrastructure/.../vector/QdrantClientConfig.java:11` | 构造 `QdrantClient` Bean |
| `data-plane/data-infrastructure/.../vector/QdrantProperties.java:6` | `qdrant.*` 配置属性 |
| `data-plane/data-infrastructure/.../vector/VectorStoreException.java:3` | 向量存储异常 |

**嵌入文本构成**（`VectorStoreClient.buildEmbeddingText`，`VectorStoreClient.java:134`）：

- `qualifiedName`（优先）
- `documentation`
- `sourceCode`

**Point ID**：使用 `UUID.nameUUIDFromBytes(elementId.getBytes(UTF_8))`，保证同一元素多次解析时幂等更新。

### 3.2 图存储

| 文件 | 职责 |
|---|---|
| `data-plane/data-infrastructure/.../graph/GraphStoreClient.java:16` | Neo4j 节点/关系写入与按仓库删除 |

**节点属性**（`GraphStoreClient.toNodeParameters`，`GraphStoreClient.java:85`）：
`id`、`repository`、`commitHash`、`kind`、`name`、`qualifiedName`、`filePath`、`startLine`、`endLine`、`sourceCode`、`documentation`、`modifiers`。

**关系**：`MERGE (source)-[r:RELATES_TO]->(target) SET r.kind = rel.kind`，仅在源节点和目标节点都已存在时建立。

### 3.3 编排

| 文件 | 职责 |
|---|---|
| `data-plane/data-application/.../ParseApplicationService.java:14` | 调用 Parse Worker，再分别调用向量/图存储 |
| `data-plane/data-infrastructure/.../parse/ParseWorkerClient.java:19` | gRPC 客户端，含 120s 超时和 100MB 消息上限 |

## 4. 配置项

### 4.1 data-plane `application.yml`

```yaml
qdrant:
  host: ${QDRANT_HOST:localhost}
  port: ${QDRANT_PORT:6334}        # gRPC 端口，REST 端口为 6333
  collection-name: ${QDRANT_COLLECTION:code-elements}
  create-collection-if-missing: true
```

### 4.2 java-parser `application.yml`

```yaml
grpc:
  server:
    port: 9093
    max-inbound-message-size: 104857600   # 100MB，支持全仓库一次性解析
```

### 4.3 环境变量

| 变量 | 说明 |
|---|---|
| `REPOSITORY_ENCRYPTION_KEY` | 仓库凭据 AES-GCM 加密密钥，长度 32 字符 |

## 5. 核心 API

### 5.1 触发解析并索引

```http
POST /api/data/parse
Content-Type: application/json
```

请求体示例：

```json
{
  "repository": "skykiwi-news-server",
  "commitHash": "bfbc7a9eac9b6a465320238bf9764b7f32e05102",
  "repoPath": "/tmp/repos/skykiwi-news-server",
  "changedFiles": [
    "information-presentation-api/src/main/java/com/skykiwi/information/presentation/page/HtmlController.java"
  ],
  "language": "java"
}
```

响应为解析后的 `CodeElement` 列表；data-plane 会在返回前完成向量/图写入。

### 5.2 触发仓库同步

```http
POST /api/data/repositories/{id}/sync
```

同步仅拉取代码，不自动触发解析。解析需要额外调用 `/api/data/parse`。

## 6. 验证结果

在本地对 `skykiwi-news-server` 全部 567 个 Java 文件执行 `/api/data/parse`：

| 指标 | 数值 |
|---|---|
| 解析返回 CodeElement | 1879 |
| Neo4j `CodeElement` 节点 | 1849 |
| Neo4j `RELATES_TO` 关系 | 1285 |
| Qdrant `code-elements` points | 1849 |

> 解析返回数（1879）大于实际写入数（1849），是因为部分元素 `id` 重复（如内部类、同名构造器等），通过 `MERGE` 与 deterministic UUID 实现了幂等去重。

## 7. 常见问题

### 7.1 Qdrant 报错 `First received frame was not SETTINGS`

**原因**：gRPC 客户端连到了 Qdrant 的 REST 端口 `6333`。
**解决**：确认配置为 gRPC 端口 `6333` → `6334`。

### 7.2 `REPOSITORY_ENCRYPTION_KEY` 缺失

data-plane 启动时需要该环境变量用于解密仓库 token，否则读写仓库配置会失败。

### 7.3 解析大仓库超时或消息过大

已在 `ParseWorkerClient` 和 `java-parser` 分别设置 120s 超时与 100MB 入站消息上限。

## 8. 后续扩展点

- 将 `TokenHashEmbeddingGenerator` 替换为真实 Embedding 模型（如 OpenAI / 本地 ONNX）。
- 在 `ParseApplicationService` 中引入异步消息或 Temporal 工作流，解耦解析与索引。
- 为图存储增加按 `commitHash` 的历史版本管理。
- 补充单元测试与集成测试，确保覆盖率 ≥ 80%。
