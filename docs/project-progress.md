# 项目进度跟踪

**更新日期**: 2026-08-01
**当前版本**: v0.8 GA 阶段
**对照架构文档**: v2.1 受控工程

---

## 里程碑总览

| 版本 | 主题 | 状态 | 完成度 |
|------|------|------|--------|
| v0.1 MVP | 可观察性基础 | ✅ 已完成 | 90% |
| v0.2 Alpha | 可控制 | ✅ 已完成 | 85% |
| v0.3 Beta | 可验证 | ✅ 已完成 | 90% |
| v0.5 RC | 可恢复 | ✅ 已完成 | 90% |
| v0.8 GA | 安全默认 | 🟡 进行中 | 40% |
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
| 熔断降级 | ✅ 完成 | v0.5 RC 接入 Resilience4j |
| 限流防护 | ✅ 完成 | v0.5 RC 接入 Resilience4j |
| RBAC 权限 | ✅ 完成 | v0.8 GA Phase A/B/C 实现 JWT + Spring Security + 方法级权限 |
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
| `/api/control/auth/login` | POST | JWT 登录 |

**数据平面** (http://localhost:8082):
| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/data/diagnosis` | POST | 智能诊断 |
| `/api/data/repositories` | GET/POST | 仓库管理 |
| `/api/data/repositories/{id}/sync` | POST | 仓库同步 |
| `/api/data/snapshots` | GET | 按仓库查询快照 |
| `/api/data/snapshots/{id}` | GET | 快照详情 |
| `/api/data/snapshots/{left}/diff/{right}` | GET | 快照差异报告 |
| `/api/data/snapshots/{id}/rollback` | POST | 物理回滚到指定快照 |
| `/api/data/metrics` | GET | 实时系统指标（Qdrant/Neo4j/诊断次数） |

### 已实现的前端页面

| 页面 | 路由 | 功能 |
|------|------|------|
| 智能诊断 | `/` | 错误信息输入 + 诊断结果展示 |
| 仓库管理 | `/repositories` | 仓库 CRUD + 同步 |
| 审批工作台 | `/approvals` | 审批列表（待审批/已批准/已拒绝） |
| 工作流监控 | `/workflows` | 工作流列表 + 暂停/恢复/回滚 |
| 快照管理 | `/snapshots` | 快照列表 + 校验状态 + 版本差异 + 物理备份状态 + 回滚 |
| 系统管理 | `/admin` | 指标 + 基础设施状态 + 配置管理 |
| 登录 | `/login` | JWT 登录 + 路由守卫 |

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

## v0.5 RC — 可恢复

| 功能 | 状态 | 说明 |
|------|------|------|
| Resilience4j 熔断 | ✅ 完成 | LLM、Parse Worker 熔断 + fallback |
| Resilience4j 限流 | ✅ 完成 | Parse / Diagnosis / Workflow 启动限流 |
| 前端降级提示 | ✅ 完成 | 统一拦截 429/503，Toast 提示“服务繁忙” |
| Qdrant 物理快照 | ✅ 完成 | point-level 快照（`qdrant-points.jsonl`），按仓库恢复 |
| Neo4j 物理备份 | ✅ 完成 | APOC 流式导出，未安装时降级为手动 Cypher |
| 本地备份存储 | ✅ 完成 | `BackupStorage` 管理 `backups/{repo}/{snapshotId}/` |
| 自动备份任务 | ✅ 完成 | `SnapshotBackupJob` 每 6 小时补全缺失物理备份 |
| 回滚 API | ✅ 完成 | `POST /api/data/snapshots/{id}/rollback` 物理恢复 |
| 前端回滚/预览 | ✅ 完成 | 快照页面显示备份状态、差异预览、回滚确认 |

---

## v0.8 GA — 安全默认（进行中）

| 功能 | 状态 | 说明 |
|------|------|------|
| RBAC 领域模型 | ✅ 完成 | `User` / `Role` / `Permission` 领域模型 |
| 用户表与默认账号 | ✅ 完成 | Flyway V4 + ADMIN/OPERATOR/VIEWER 默认账号 |
| JWT 签发与校验 | ✅ 完成 | `shared-security` 模块 + jjwt 0.12.6 |
| Spring Security 集成 | ✅ 完成 | 无状态过滤器链 + 方法级 `@PreAuthorize` |
| 控制器权限注解 | ✅ 完成 | control-plane / data-plane 全部端点授权 |
| 前端登录与路由守卫 | ✅ 完成 | LoginView + axios 拦截器 + beforeEach 守卫 |
| 数据加密（敏感配置） | ✅ 完成 | JWT_SECRET 强制配置；仓库凭证 AES/GCM 加密 |
| 登录审计与账户锁定 | ✅ 完成 | 登录成功/失败/锁定审计 + 5 次失败锁定 30 分钟 |
| Micrometer 实时指标 | ✅ 完成 | `/api/data/metrics` + Micrometer Counter/Gauge，AdminController 调用实时数据 |
| 安全事件监控告警 | ✅ 完成 | Micrometer Counter + Gauge + 定时异常检测（暴力破解） |
| 修复 WorkflowService.listWorkflows 空列表 | ✅ 完成 | 基于快照表派生 `/api/data/workflows`，控制平面聚合 |
| 安全扫描引擎 | ✅ 完成 | 基于正则的本地静态扫描：硬编码凭证、SQL 注入、弱哈希等 |

---

## 技术债务与已知限制

| 项目 | 影响 | 计划 |
|------|------|------|
| RBAC + 审计 + 安全扫描已实现 | 方法级权限、登录审计、账户锁定、静态安全扫描已上线 | v0.8 GA 已完成 |
| Neo4j 物理备份依赖 APOC 插件 | 未安装 APOC 时降级为手动 Cypher，性能略低 | 生产环境建议预装 APOC |

---

## 服务启动命令

```bash
# 0. 环境变量（必须）
export JWT_SECRET="your-32-bytes-or-longer-secret-key-here"
export SECURITY_LOCKOUT_MAX_ATTEMPTS=5
export SECURITY_LOCKOUT_DURATION_MINUTES=30

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

内部调用说明：
- 控制平面调用数据平面（`/api/data/metrics`、`/api/data/workflows`）时使用服务账号 JWT，需在两端配置相同的 `JWT_SECRET`。
- 默认管理员账号：`admin` / `admin`。

---

## Git 提交历史

| 提交 | 描述 | 日期 |
|------|------|------|
| `bf3db09` | Initial commit | 2026-07-16 |
| `594fb22` | 完成多项功能开发与优化 | 2026-07-22 |
| `d9d1908` | 完善控制平面工作流编排与前端管理界面 | 2026-07-30 |
| `87845d6` | v0.5 RC：可恢复性 + 熔断限流 + 物理快照回滚 | 2026-08-01 |
| `c1a9f26` | v0.8 GA Phase A/B/C：RBAC + JWT 认证授权 + 前端登录 | 2026-08-01 |
| `540b2c1` | docs: 更新 v0.8 GA Phase A/B/C 进度 | 2026-08-01 |
| `5296c57` | v0.8 GA Phase D：数据加密与审计加固 | 2026-08-01 |
| `53381c3` | v0.8 GA Phase E：Micrometer 实时指标替换 AdminController 静态数据 | 2026-08-01 |
| `9ba3617` | v0.8 GA Phase F：安全事件监控与告警指标 | 2026-08-01 |
| `9598dc3` | fix: v0.8 GA Phase H 修复 WorkflowService.listWorkflows 空列表 | 2026-08-01 |
| `e315b45` | fix: 修复控制平面调用数据平面认证与默认管理员密码 | 2026-08-01 |
| `当前工作区` | feat: v0.8 GA Phase G 安全扫描引擎替换占位实现 | 2026-08-01 |
