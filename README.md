# 智能代码诊断与知识图谱系统

基于受控工程（Controlled Engineering）原则构建的智能代码诊断系统，支持多语言代码解析、知识图谱构建、RAG 检索和 LLM 诊断。

## 技术栈

- **前端**：Vue 3 + Vite + TypeScript
- **控制平面**：Java 21 + Spring Boot 3.4 + Maven 4 + Temporal
- **数据平面**：Java 21 + Spring Boot 3.4 + Maven 4 + Qdrant + Neo4j
- **Parse Worker**：JavaParser（Java）、Roslyn（C#）
- **部署**：Docker Compose（本地开发）

## 项目结构

```
intelligent-diagnosis/
├── frontend/                  # Vue 前端
├── control-plane/             # 控制平面后端（策略、审计、工作流）
├── data-plane/                # 数据平面后端（诊断、图谱、索引、LLM）
├── parse-workers/             # 多语言代码解析 Worker
│   ├── java-parser/           # Java 解析 Worker（JavaParser）
│   └── csharp-parser/         # C# 解析 Worker（Roslyn）
├── shared-contracts/          # 统一 Protobuf / gRPC 契约
├── infrastructure/docker/     # Docker Compose 本地部署
└── e2e-tests/                 # Playwright E2E 测试
```

## 快速开始

### 环境要求

- Docker + Docker Compose
- JDK 21
- Maven Wrapper（项目已包含，自动下载 Maven 4，无需本地单独安装）
- Node.js 20
- .NET 8 SDK（用于 C# Parse Worker）

### 本地启动

```bash
# 一键启动全部服务（基础设施、后端、Parse Workers、前端）
docker-compose -f infrastructure/docker/docker-compose.yml up -d --build
```

或分步开发启动：

```bash
# 1. 启动基础设施服务
docker-compose -f infrastructure/docker/docker-compose.yml up -d postgres qdrant neo4j temporal temporal-db

# 2. 构建后端
./mvnw clean install

# 3. 启动 control-plane（依赖 PostgreSQL/Temporal）
./mvnw -pl control-plane/control-boot -am spring-boot:run

# 4. 启动 data-plane（依赖 Qdrant/Neo4j，会自动连接配置的 Parse Workers）
./mvnw -pl data-plane/data-boot -am spring-boot:run

# 5. 启动 Parse Workers
./mvnw -pl parse-workers/java-parser spring-boot:run
cd parse-workers/csharp-parser && dotnet run

# 6. 启动前端
cd frontend && pnpm install && pnpm dev
```

访问 http://localhost:5173

## 核心 API

### 代码解析

`POST /api/data/parse`

请求体示例：

```json
{
  "repository": "my-repo",
  "commitHash": "abc123",
  "repoPath": "/tmp/my-repo",
  "changedFiles": ["src/Main.java"],
  "language": "java"
}
```

响应为解析后的 `CodeElement` 列表；data-plane 会同步调用对应语言的 Parse Worker，并将结果写入 Qdrant 向量索引与 Neo4j 知识图谱。

### 仓库管理

- `POST /api/data/repositories` — 创建仓库配置
- `GET /api/data/repositories` — 列出仓库配置
- `POST /api/data/repositories/{id}/sync` — 触发仓库同步
- `POST /api/data/diagnosis` — 基于检索到的代码片段调用 LLM 诊断

**本地验证数据**：曾对 `skykiwi-news-server` 全部 567 个 Java 文件执行全量解析，生成约 1849 个 `CodeElement` 节点、1285 条 `RELATES_TO` 关系，并同步写入 Qdrant 与 Neo4j。

支持的 `language`：

- `java` → Java Parser（gRPC 端口 9093）
- `csharp` → C# Parser（gRPC 端口 9094）

## 开发规范

- 遵循 TDD：先写测试，再写实现
- 单元测试覆盖率 ≥ 80%
- 优先使用不可变对象（record / readonly）
- 任何变更走 Temporal 工作流审批

## 文档

- [架构设计文档](/Users/sunze/git/intelligent-diagnosis/docs/智能代码诊断系统_架构文档_v2.1_受控工程.md)
- [项目进度跟踪](/Users/sunze/git/intelligent-diagnosis/docs/project-progress.md)
