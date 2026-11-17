# 实现与部署

## Agent 与校验

生成请求创建异步任务。Java 查询行政区，按关键词检索必去点。模型通过受限的 `search_places` 函数查找更多候选地点，只能搜索已选城市。模型输出地点 ID、开始时间、停留时长及估算。服务端确认 ID 来自真实高德记录、日期齐全、地点不重复、包含必去点且费用在预算内。之后查询相邻点的步行或驾车路线，按实查耗时加五分钟缓冲重新检查时间。失败时将约束反馈给模型，允许修正一次。超过调用预算、无法查到地点或路线、仍然冲突时均返回错误。

修订使用用户选择的日期和当前版本作为输入。其他日期直接沿用，锁定站点的日期、时间、预计活动费用和备注保持原样。此次修改涉及的路线重新查询。同一行程上的并发操作通过 PostgreSQL 版本条件更新，防止后完成的任务覆盖新版本。

## HTTP 接口

写入请求需带 `X-Travel-Client: web`。生成和修订请求需提供 UUID 格式的 `Idempotency-Key`。跟随任务 ID 读取任务状态。API 包含应用层会话、账户和输入约束。

| 方法与路径 | 行为 |
| --- | --- |
| GET /api/status | 配置状态 |
| GET /api/auth/me | 当前账户 |
| POST /api/auth/register、/login、/logout | 建立或销毁 Cookie 会话 |
| GET /api/trips?offset=0 | 已保存行程，每页 25 条 |
| POST /api/trips | 提交生成任务 |
| GET /api/trips/{id} | 行程详情 |
| POST /api/trips/{id}/revisions | 自然语言调整 |
| PATCH /api/trips/{id}/stops/{stopId} | 锁定或解锁地点 |
| DELETE /api/trips/{id}?revision=N | 删除指定版本的行程 |
| GET /api/jobs/{id} | 任务步骤、结果或错误 |
| GET /actuator/health | 进程和数据库探活，仅由后端提供 |

## 部署边界

当前 Compose 拓扑用于单实例运行。任务和限流计数保存在进程内，进程重启会清除待处理任务及重试索引，已写入 PostgreSQL 的行程持续保留。多副本部署需要持久任务队列、共享限流和跨实例幂等键。模型和地图配额需在各供应商控制台限制。公网上线时在 HTTPS 反向代理后开启安全 Cookie，配置代理可信地址并保护上游服务，按需关闭自助注册。定期备份 PostgreSQL。

前端和后端分别开发时，启动本地 PostgreSQL，设置 `DATABASE_URL`、`DATABASE_USER`、`DATABASE_PASSWORD` 和 `COOKIE_SECURE=false`，再运行 `mvn -f backend/pom.xml spring-boot:run` 与 `npm --prefix web run dev`。Vite 默认将 `/api` 转发至 8080。

## 旧版本

旧版 SQLite 行程使用不同的结构，部分数据缺少可核实的地点 ID、路线证据或当前可用日期。重写版从新数据库启动，如需保留旧记录，可从自己的 SQLite 备份离线导出归档，并使用新系统重新规划。旧仓库的配置与数据库从工作树移除。若历史提交里的密钥仍然有效，应在供应商处撤销并重新签发。已提交的历史数据需另行处理。
