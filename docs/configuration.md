# 配置说明

大多数选项应在 Web UI 的“设置”页面修改并即时生效。环境变量主要用于容器启动、存储路径和首次部署。

## Docker 挂载

```yaml
volumes:
  - ./config:/config
  - /your/media/path:/media:ro
```

`/config` 保存 SQLite 数据库。`/media` 是浏览器中可访问的媒体根目录。初次使用保持 `:ro`；需要执行文件操作时改为 `:rw`，并同时设置 `FILEMAID_ROOT_WRITABLE=true`。

## 环境变量

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `FILEMAID_PORT` | `8080` | 容器内 HTTP 端口 |
| `FILEMAID_MEDIA_ROOT` | `./data/media` | 媒体根目录 |
| `FILEMAID_ROOT_WRITABLE` | `false` | 允许文件写操作 |
| `FILEMAID_DB_PATH` | `./config/filemaid.db` | SQLite 文件位置 |
| `FILEMAID_AUTH_ENABLED` | `true` | 启用单用户登录；仅受控开发环境建议关闭 |
| `FILEMAID_SECURE_COOKIES` | `false` | HTTPS 反代后设为 `true` |
| `FILEMAID_SCAN_MAX_DEPTH` | `16` | 默认扫描深度 |
| `FILEMAID_SCAN_MAX_FILES` | `10000` | 默认单次文件上限 |
| `FILEMAID_FFPROBE_PATH` | `ffprobe` | FFprobe 可执行文件 |
| `FILEMAID_TMDB_API_KEY` | 空 | TMDB Key |
| `FILEMAID_TVDB_API_KEY` | 空 | TheTVDB Key |
| `FILEMAID_TVDB_PIN` | 空 | TheTVDB PIN |
| `FILEMAID_OMDB_API_KEY` | 空 | OMDb Key |
| `FILEMAID_TVMAZE_ENABLED` | `true` | 启用 TVMaze |
| `FILEMAID_ANIDB_ENABLED` | `true` | 启用 AniDB |

命名模板也可通过 `FILEMAID_NAMING_SERIES`、`FILEMAID_NAMING_MOVIE` 和 `FILEMAID_NAMING_UNKNOWN` 提供初始值。日常调整建议使用 Web UI。

## Web 设置分类

- **网络**：HTTP 代理、超时和重试。
- **元数据源**：开关、Key、自定义端点和连接测试。
- **命名**：媒体库预设、标题偏好和模板。
- **匹配**：语言优先级、候选数量、阈值和默认匹配方式。
- **后处理**：NFO、封面和空目录清理。
- **文件操作**：默认操作、冲突策略和历史保留时间。
- **扫描**：深度、数量、忽略规则、最小大小和扩展名。
- **系统**：时区、日志级别和运行诊断。

## 命名模板

模板只能生成媒体根目录内的相对路径，不执行脚本。绝对路径、路径越界和未知变量都会被拒绝。

常用变量：`{title}`、`{year}`、`{season:02}`、`{episodes}`、`{extension}`、`{original}`。

FFprobe 成功后还可使用：`{resolution}`、`{videoCodec}`、`{audioCodec}`、`{audioLanguage}`、`{subtitleLanguage}`、`{width}`、`{height}`、`{frameRate}`、`{bitRate}`、`{duration}` 和 `{fileSize}`。

## 密钥安全

发布源码与镜像没有内置第三方 Key。设置接口返回密钥时会脱敏，但 `/config/filemaid.db` 仍应视为敏感文件。不要提交 `.env`、`config/` 或日志中的私有信息。
