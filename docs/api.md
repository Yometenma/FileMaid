# HTTP API

API 根路径为 `/api/v1`。除健康检查和认证入口外，默认需要登录会话。修改数据的请求还需要把 `XSRF-TOKEN` Cookie 值放入 `X-XSRF-TOKEN` 请求头。

Web UI 是推荐入口；本页用于集成和排错。

## 认证

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/auth/status` | 认证、初始化和登录状态 |
| POST | `/auth/setup` | 创建首个管理员账号 |
| POST | `/auth/login` | 表单登录 |
| POST | `/auth/logout` | 退出 |
| POST | `/auth/change-password` | 修改密码 |

## 存储与任务

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/roots` | 存储根列表 |
| GET | `/roots/{rootId}/directories` | 浏览或搜索目录 |
| GET | `/roots/{rootId}/scan` | 兼容的同步扫描 |
| POST | `/roots/{rootId}/scan` | 创建后台扫描任务 |
| GET | `/roots/{rootId}/probe` | 探测单个媒体文件 |
| GET | `/tasks` | 任务列表 |
| GET | `/tasks/{id}` | 任务状态与结果 |
| POST | `/tasks/{id}/cancel` | 请求取消任务 |

## 整理流程

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/media/parse` | 批量解析文件名 |
| POST | `/media/groups/analyze` | 分组视频和伴随文件 |
| POST | `/rename-plans/preview` | 同步生成预览 |
| POST | `/rename-plans/preview-task` | 后台生成含 FFprobe 的预览 |
| POST | `/rename-plans` | 构建操作计划 |
| POST | `/rename-plans/validate` | 校验并获取确认令牌 |
| POST | `/rename-plans/execute` | 消费令牌并创建执行任务 |

## 元数据与设置

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/metadata/providers` | 提供器状态 |
| GET | `/metadata/search` | 聚合搜索候选 |
| POST | `/metadata/match` | 自动匹配 |
| POST | `/metadata/providers/{id}/test` | 测试连接 |
| GET / PUT | `/match-decisions` | 查询或保存匹配决策 |
| GET / PUT | `/settings` | 查询或保存动态设置 |
| GET | `/settings/schema` | 设置字段、类型和运行状态 |
| GET | `/naming/templates` | 当前命名模板 |

## 历史、日志和诊断

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/operations` | 操作历史 |
| POST | `/operations/{id}/undo` | 安全撤销 |
| GET | `/logs` | 内存日志；支持 `after`、`level`、`query`、`limit` |
| GET | `/system/health` | 简单健康状态 |
| GET | `/system/diagnostics` | SQLite、FFprobe 和存储根诊断 |
| GET | `/actuator/health` | 容器健康检查 |

请求和响应模型以当前控制器与 `/settings/schema` 为准。写操作集成必须完整实现 CSRF、确认令牌和后台任务轮询，不能绕过 Web UI 使用的安全流程。
