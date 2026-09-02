# 架构与开发

FileMaid 是一个模块化单体，正式运行模块保持单向依赖：

```text
Web UI → server → infrastructure → application → core
                  └──────────────────────────────→
```

| 模块 | 职责 |
| --- | --- |
| `core` | 无框架领域模型和安全约束 |
| `application` | 用例、端口和流程编排 |
| `infrastructure` | 文件系统、SQLite、解析、元数据、命名和 FFprobe 适配器 |
| `server` | Spring Boot HTTP 服务、认证、后台任务和静态 Web UI |
| `web` | Vue 3、TypeScript、Pinia 和 Element Plus 前端 |

项目不包含旧引擎运行时或兼容模块。媒体识别、相似度、命名、文件操作和元数据能力均由当前模块实现。

## 安全边界

- 客户端只提交存储根 ID 和相对路径；服务端规范化后再次验证路径仍位于允许根目录内。
- 写操作采用“预览 → 校验 → 一次性确认令牌 → 执行前重校验”流程。
- 确认令牌保存不可变操作列表和后处理选项，15 分钟有效且只能消费一次。
- 默认禁止覆盖；每个文件都有独立的结果和历史记录。
- 正式部署默认启用登录和 CSRF 防护，媒体卷默认只读。

## 本地开发

需要 JDK 17 和 Node.js 22。

```bash
# 后端测试
./gradlew test

# 前端测试与构建
cd web
npm ci
npm test
npm run build

# 启动服务
cd ..
./gradlew :modules:server:bootRun
```

Windows 使用 `gradlew.bat`。前端构建结果会写入 `modules/server/src/main/resources/static`。

`bootRun` 的工作目录是 `modules/server`，相对媒体路径和数据库路径会相对该目录解析。本地测试建议使用明确的绝对路径，并且不要指向真实媒体库。
