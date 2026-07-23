# 数据平面运行手册

本手册覆盖 data-plane 及 java-parser 的本地启动、仓库同步、触发解析、验证数据等日常操作。

## 1. 环境要求

- JDK 21
- Maven 3.9+（项目使用 Maven Wrapper 也可）
- Docker + Docker Compose
- `REPOSITORY_ENCRYPTION_KEY` 环境变量（32 字符，例如 `change-me-in-production-32chars!`）

## 2. 启动基础设施

```bash
docker-compose -f infrastructure/docker/docker-compose.yml up -d postgres qdrant neo4j temporal temporal-db
```

确认端口：

| 服务 | 端口 | 说明 |
|---|---|---|
| PostgreSQL | 5432 | control-plane / data-plane 共享 |
| Qdrant REST | 6333 | 健康检查、Web UI |
| Qdrant gRPC | 6334 | data-plane 向量客户端使用 |
| Neo4j Bolt | 7687 | data-plane 图客户端使用 |
| Neo4j Browser | 7474 | 可视化查询 |

## 3. 启动 Parse Worker

```bash
cd "/Users/sunze/git/intelligent-diagnosis"
nohup mvn -pl parse-workers/java-parser spring-boot:run > java-parser.log 2>&1 &
```

确认日志出现 `gRPC Server started, listening on address: *, port: 9093`。

## 4. 启动 data-plane

```bash
cd "/Users/sunze/git/intelligent-diagnosis/data-plane"
export REPOSITORY_ENCRYPTION_KEY="change-me-in-production-32chars!"
nohup mvn -pl data-boot spring-boot:run > ../data-plane.log 2>&1 &
```

确认日志出现 `Tomcat started on port 8082`。

> 如果修改了 `data-infrastructure` 等依赖模块，先执行编译：
> ```bash
> mvn -pl data-boot -am install -DskipTests
> ```

## 5. 添加并同步仓库

### 5.1 创建仓库

```http
POST /api/data/repositories
Content-Type: application/json

{
  "name": "skykiwi-news-server",
  "displayName": "Skykiwi News Server",
  "type": "GITHUB",
  "url": "https://github.com/skymedialtd/skykiwi-news-server.git",
  "branch": "master",
  "localPath": "/tmp/repos/skykiwi-news-server",
  "enabled": true,
  "authType": "NONE"
}
```

记录返回的 `id`。

### 5.2 触发同步

```http
POST /api/data/repositories/{id}/sync
```

同步成功后，代码会落在 `localPath` 指定目录，并在 PostgreSQL 中记录同步状态。

## 6. 触发代码解析与索引

### 6.1 少量文件测试

```bash
curl -s -X POST http://localhost:8082/api/data/parse \
  -H 'Content-Type: application/json' \
  -d '{
    "repository": "skykiwi-news-server",
    "commitHash": "bfbc7a9eac9b6a465320238bf9764b7f32e05102",
    "repoPath": "/tmp/repos/skykiwi-news-server",
    "changedFiles": [
      "information-presentation-api/src/main/java/com/skykiwi/information/presentation/page/HtmlController.java"
    ],
    "language": "java"
  }'
```

### 6.2 全量解析

```bash
# 生成文件列表
find "/tmp/repos/skykiwi-news-server" -name "*.java" \
  | sed 's|/tmp/repos/skykiwi-news-server/||' \
  | jq -R -s -c 'split("\n") | map(select(. != ""))' > /tmp/java_files.json

# 生成请求体
jq -n \
  --arg repo "skykiwi-news-server" \
  --arg commit "bfbc7a9eac9b6a465320238bf9764b7f32e05102" \
  --arg path "/tmp/repos/skykiwi-news-server" \
  --slurpfile files /tmp/java_files.json \
  '{repository: $repo, commitHash: $commit, repoPath: $path, changedFiles: $files[0], language: "java"}' \
  > /tmp/parse_request.json

# 触发解析
curl -s -X POST http://localhost:8082/api/data/parse \
  -H 'Content-Type: application/json' \
  -d @/tmp/parse_request.json \
  -o /tmp/parse_response.json

jq 'length' /tmp/parse_response.json
```

## 7. 验证数据

### 7.1 验证 Neo4j 节点与关系

```bash
# 节点数
curl -s -X POST http://localhost:7474/db/neo4j/tx/commit \
  -u neo4j:password \
  -H 'Content-Type: application/json' \
  -d '{"statements":[{"statement":"MATCH (n:CodeElement) RETURN count(n) AS count"}]}' \
  | jq '.results[0].data[0].row[0]'

# 关系数
curl -s -X POST http://localhost:7474/db/neo4j/tx/commit \
  -u neo4j:password \
  -H 'Content-Type: application/json' \
  -d '{"statements":[{"statement":"MATCH ()-[r:RELATES_TO]->() RETURN count(r) AS count"}]}' \
  | jq '.results[0].data[0].row[0]'
```

### 7.2 验证 Qdrant 集合

```bash
# 集合点数
curl -s http://localhost:6333/collections/code-elements \
  | jq '{status: .status, points_count: .result.points_count}'

# 抽样检查 point 的 payload 与向量维度
curl -s -X POST http://localhost:6333/collections/code-elements/points/scroll \
  -H 'Content-Type: application/json' \
  -d '{"limit": 1, "with_vectors": true}' \
  | jq '.result.points[0] | {id: .id, payload_keys: (.payload | keys), vector_len: (.vector | length)}'
```

## 8. 按仓库清理数据

当前实现中，`VectorStoreClient.deleteByRepository(repository)` 与 `GraphStoreClient.deleteByRepository(repository)` 已提供，但尚未暴露独立 REST 接口。如需清理，可通过以下 Cypher 和 Qdrant API：

```bash
# Neo4j
curl -s -X POST http://localhost:7474/db/neo4j/tx/commit \
  -u neo4j:password \
  -H 'Content-Type: application/json' \
  -d '{
    "statements": [{
      "statement": "MATCH (e:CodeElement {repository: $repo}) OPTIONAL MATCH (e)-[r]-() DELETE r, e",
      "parameters": {"repo": "skykiwi-news-server"}
    }]
  }'

# Qdrant
curl -s -X POST http://localhost:6333/collections/code-elements/points/delete \
  -H 'Content-Type: application/json' \
  -d '{
    "filter": {"must": [{"key": "repository", "match": {"value": "skykiwi-news-server"}}]}
  }'
```

## 9. 故障排查

| 现象 | 排查方向 |
|---|---|
| data-plane 启动报 `REPOSITORY_ENCRYPTION_KEY` 相关错误 | 检查环境变量是否为 32 字符 |
| Qdrant `http2 exception` / `First received frame was not SETTINGS` | 确认 `qdrant.port=6334`（gRPC），不是 6333 |
| 解析大仓库超时 | 检查 `ParseWorkerClient` 超时是否为 120s，java-parser 消息上限是否足够 |
| Neo4j 关系数明显偏少 | 关系只在源、目标节点都已存在时建立；确保相关文件已在同一次解析请求中 |
| 页面刷新无数据 | 先确认同步成功，再确认 `/api/data/parse` 已触发并返回元素 |

## 10. 相关文档

- [向量与图存储功能文档](./data-plane-vector-graph-storage.md)
- [项目架构设计文档](./智能代码诊断系统_架构文档_v2.1_受控工程.md)
- [项目 README](../README.md)
