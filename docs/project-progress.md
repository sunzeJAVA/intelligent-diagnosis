# 项目进度跟踪

**更新日期**: 2026-07-31
**当前版本**: v0.3 Beta 阶段
**对照架构文档**: v2.1 受控工程

---

## 里程碑总览

| 版本 | 主题 | 状态 | 完成度 |
|------|------|------|--------|
| v0.1 MVP | 可观察性基础 | ✅ 已完成 | 90% |
| v0.2 Alpha | 可控制 | ✅ 已完成 | 85% |
| v0.3 Beta | 可验证 | ✅ 已完成 | 90% |
| v0.5 RC | 可恢复 | ⬜ 未开始 | 0% |
| v0.8 GA | 安全默认 | ⬜ 未开始 | 0% |
| v1.0 Stable | 渐进发布 | ⬜ 未开始 | 0% |

---

## v0.1 MVP — 可观察性基础

| 功能 | 状态 | 说明 |
|------|------|------|
| 结构化日志 | ✅ 完成 | SLF4J + Logback |
| API 端点暴露 | ✅ 完成 | 控制平面 + 数据平面 REST API |
| 前端 UI 基础 | ✅ 完成 | Vue 3 + Tailwind CSS |
| 诊断功能 | ✅ 完成 | Mock LLM + RAG 检索 |
| 仓库管理 | ✅ 完成 | CRUD + Git 同步 |
| Metrics 指标 | ⚠️ 部分 | 静态 Mock 数据，待接入 Micrometer |
| OpenTelemetry Trace | ⬜ 未开始 | 待接入 |

---

## v0.2 Alpha — 可控制

| 功能 | 状态 | 说明 |
|------|------|------|
| Temporal 工作流引擎 | ✅ 完成 | 集成 Temporal Java SDK |
| IndexUpdateWorkflow | ✅ 完成 | 数据平面索引更新工作流定义 |
| 变更检测 Activity | ✅ 完成 | Git diff 变更检测 |
| 安全扫描 Activity | ✅ 完成 | 代码安全扫描 |
| 风险分类 Activity | ✅ 完成 | 高/中/低风险分级 |
| 解析 Activity | ✅ 完成 | gRPC 调用 Parse Workers |
| 验证 Activity | ✅ 完成 | 索引结果验证 |
| 索引提升 Activity | ✅ 完成 | 索引进生产环境 |
| 审批信号 | ✅ 完成 | approve / reject 信号 |
| 暂停/恢复信号 | ✅ 完成 | pause / resume 信号 |
| 回滚信号 | ✅ 完成 | rollback 信号 |
| WorkflowController | ✅ 完成 | 工作流查询和信号发送 REST API |
| Workflow 启动参数修复 | ✅ 完成 | 修复 TemporalWorkflowServiceImpl 与工作流接口参数不匹配 |
| Workflow 真实状态查询 | ✅ 完成 | getWorkflow 通过 WorkflowStub.query 获取真实状态 |
| ApprovalApplicationService | ✅ 完成 | 审批应用服务 |
| 工作流监控 UI | ✅ 完成 | WorkflowsView 页面（暂停/恢复/回滚） |
| 审批工作台 UI | ✅ 完成 | ApprovalsView 页面（按状态分类） |
| Temporal 优雅降级 | ✅ 完成 | 服务不可用时 null 安全处理 |
| AuditService 审计 | ✅ 完成 | PostgreSQL 持久化 + SHA-256 签名 |
| HealthCheckService | ✅ 完成 | Socket 端口健康检查 |
| AdminController | ✅ 完成 | 系统指标 + 基础设施健康 API |
| 自动刷新 | ✅ 完成 | 30 秒轮询 |
| 熔断降级 | ⬜ 未开始 | 待实现 |
| 限流防护 | ⬜ 未开始 | 待实现 |
| RBAC 权限 | ⬜ 未开始 | 待实现 |
| 安全扫描引擎（完整） | ⬜ 未开始 | 待实现 |

### 已实现的工作流编排

```
IndexUpdateWorkflow
├── detectChanges          (Activity)  Git diff 变更检测
├── scanSecurity            (Activity)  代码安全扫描 → 风险分级
├── [if 高风险] waitApproval (Signal)   等待人工审批
│   ├── approve             (Signal)   批准 → 继续
│   └── reject              (Signal)   拒绝 → 终止
├── parseInSandbox          (Activity)  gRPC 调用 Parse Workers
├── validateIndex           (Activity)  索引结果验证
├── promoteIndex            (Activity)  索引进生产
└── [on any failure] rollback (Signal) 自动回滚
```

### 已实现的 API 端点

**控制平面** (http://localhost:8081):
| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/control/workflows` | GET | 查询工作流列表 |
| `/api/control/workflows/{id}/pause` | POST | 暂停工作流 |
| `/api/control/workflows/{id}/resume` | POST | 恢复工作流 |
| `/api/control/workflows/{id}/rollback` | POST | 回滚工作流 |
| `/api/control/workflows/{id}/approve` | POST | 批准工作流 |
| `/api/control/workflows/{id}/reject` | POST | 拒绝工作流 |
| `/api/control/approvals` | GET | 查询审批列表 |
| `/api/control/audits` | GET | 查询审计记录 |
| `/api/control/audits/{id}/integrity` | GET | 校验审计记录完整性 |
| `/api/control/admin/metrics` | GET | 系统指标 |
| `/api/control/admin/infrastructures` | GET | 基础设施健康状态 |
| `/api/control/admin/configurations` | GET/PUT | 系统配置管理 |

**数据平面** (http://localhost:8082):
| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/data/diagnosis` | POST | 智能诊断 |
| `/api/data/repositories` | GET/POST | 仓库管理 |
| `/api/data/repositories/{id}/sync` | POST | 仓库同步 |
| `/api/data/snapshots` | GET | 按仓库查询快照 |
| `/api/data/snapshots/{id}` | GET | 快照详情 |
| `/api/data/snapshots/{left}/diff/{right}` | GET | 快照差异报告 |

### 已实现的前端页面

| 页面 | 路由 | 功能 |
|------|------|------|
| 智能诊断 | `/` | 错误信息输入 + 诊断结果展示 |
| 仓库管理 | `/repositories` | 仓库 CRUD + 同步 |
| 审批工作台 | `/approvals` | 审批列表（待审批/已批准/已拒绝） |
| 工作流监控 | `/workflows` | 工作流列表 + 暂停/恢复/回滚 |
| 快照管理 | `/snapshots` | 快照列表 + 校验状态 + 版本差异 |
| 系统管理 | `/admin` | 指标 + 基础设施状态 + 配置管理 |

### 基础设施健康检查

| 服务 | 端口 | 检查方式 |
|------|------|----------|
| PostgreSQL | 5432 | TCP Socket |
| Qdrant | 6333 | TCP Socket |
| Neo4j | 7687 | TCP Socket |
| Temporal | 7233 | TCP Socket |
| Redis | 6379 | TCP Socket |

---

## v0.3 Beta — 可验证

| 功能 | 状态 | 说明 |
|------|------|------|
| 索引不可变快照 | ✅ 完成 | PostgreSQL 元数据 + Qdrant/Neo4j 逻辑快照 |
| 版本对比 | ✅ 完成 | `/api/data/snapshots/{left}/diff/{right}` |
| 沙箱验证环境 | ✅ 完成 | `code-elements-sandbox` 集合 + `:CodeElementSandbox` 标签 |
| 快照状态监控 | ✅ 完成 | 定时校验向量/图节点数与快照元数据一致性 |

---

## 技术债务与已知限制

| 项目 | 影响 | 计划 |
|------|------|------|
| AdminController 指标为静态数据 | 指标数据不准确 | 接入 Micrometer 实时指标 |
| WorkflowService.listWorkflows 返回空列表 | 无法列出历史工作流 | 基于快照表或 Temporal 列表 API 实现 |
| 安全扫描引擎为占位实现 | 无实际安全检测能力 | v0.2 后续迭代实现 |
| 熔断/限流未实现 | 高流量下无保护 | v0.2 后续迭代实现 |
| RBAC 未实现 | 任意用户可访问所有功能 | v0.2 后续迭代实现 |
| 快照回滚仅删除当前索引 | MVP 回滚不恢复完整历史数据 | v0.5 RC 引入物理快照备份 |

---

## 服务启动命令

```bash
# 1. 基础设施
cd infrastructure/docker && docker-compose up -d postgres qdrant neo4j temporal temporal-db

# 2. 控制平面
cd control-plane/control-boot && mvn spring-boot:run

# 3. 数据平面
cd data-plane/data-boot && mvn spring-boot:run

# 4. 前端
cd frontend && pnpm dev
```

访问地址：
- 前端: http://localhost:5173
- 控制平面 API: http://localhost:8081
- 数据平面 API: http://localhost:8082
- Temporal: http://localhost:7233
- PostgreSQL: localhost:5432
- Qdrant: localhost:6333
- Neo4j: localhost:7687

---

## Git 提交历史

| 提交 | 描述 | 日期 |
|------|------|------|
| `bf3db09` | Initial commit | 2026-07-16 |
| `594fb22` | 完成多项功能开发与优化 | 2026-07-22 |
| `d9d1908` | 完善控制平面工作流编排与前端管理界面 | 2026-07-30 |
| `当前工作区` | 实现 v0.3 Beta 可验证性：快照、沙箱、版本对比、审计持久化 | 2026-07-31 |
