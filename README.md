<div align="center">

# FileMaid 🧹✨

**自托管的媒体文件整理工具**

当前测试版本：**0.1.2** · [查看变更记录](CHANGELOG.md)

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs&logoColor=white)
![Status](https://img.shields.io/badge/status-early%20development-ff69b4)

</div>

FileMaid 运行在你自己的服务器上，通过 Web UI 帮你扫描媒体目录、解析文件名、匹配元数据、预览目标路径，并在确认后批量重命名、移动、复制或创建硬链接，同时生成 Kodi NFO 与封面供 Jellyfin / Emby / Plex 直接读取。首选 Docker 部署。

---

## ✨ 核心功能

| 功能 | 说明 |
| --- | --- |
| 🗂️ **浏览目录** | 只读浏览挂载的媒体目录，支持面包屑与递归搜索 |
| 🔍 **扫描识别** | 识别电影、剧集、动漫、字幕及关联文件 |
| 🏷️ **解析文件名** | 从文件名提取标题、年份、季数、集数，剥离发布组与格式标签 |
| 🎞️ **媒体探测** | 用 ffprobe 读取分辨率、编解码器、音轨与字幕轨信息 |
| 🎬 **元数据匹配** | 聚合 TMDB / TVDB / OMDb / TVMaze / AniDB 五个提供器，自动匹配排序并绑定到源文件 |
| 📐 **命名预览** | 用安全模板生成电影 / 剧集 / 未分类的目标路径（不暴露脚本引擎） |
| 🔗 **媒体分组** | 自动把字幕、封面、NFO 等伴随文件关联到对应视频 |
| ✅ **安全执行** | 一次性确认令牌 + 不可变计划 + 执行前重新校验，默认不覆盖 |
| ↩️ **历史与撤销** | SQLite 记录逐文件结果，MOVE 移回、COPY/HARDLINK 删除目标 |
| 🖼️ **后处理** | 可选生成 Kodi NFO 与下载封面，固化进确认计划，历史可撤销 |
| 🔐 **单用户登录** | 首次设置管理员账号、BCrypt、会话与 CSRF 防护 |
| ⚙️ **动态设置** | 网络代理、API 密钥、命名、匹配、扫描等 49 项设置存 SQLite，改即生效 |

## 当前状态

FileMaid 已打通「扫描 → 匹配 → 预览 → 校验 → 执行 → 历史/撤销」的完整闭环，后端能力全部自研（无反射、无旧引擎运行时依赖）。Web UI 覆盖整理、历史、设置三页。

媒体目录默认以只读方式挂载；要启用写操作，需同时把 Docker 卷改为 `:rw` 并设置 `FILEMAID_ROOT_WRITABLE=true`。首次访问需创建单用户管理员账号，密钥读取始终脱敏。

## 🚀 快速开始（Docker）

```bash
mkdir -p data/media config   # data/media 放入你的媒体文件
docker compose up --build
```

示例 Compose 当前映射到 <http://localhost:8081>（容器内仍为 8080），完成首次设置并登录后，选择根目录、扫描媒体、匹配元数据、预览并确认执行。

> 示例 Compose 默认把媒体目录以**只读**方式挂载（`:ro`），首次试用建议保持只读，只做扫描、匹配和校验。确认目标路径符合预期后，再把卷改为 `:rw` 并在 `.env` 里设置 `FILEMAID_ROOT_WRITABLE=true`。

远程访问请参阅 [反向代理与 HTTPS](docs/reverse-proxy.md)。元数据服务的许可、署名、限流与缓存原则见 [元数据提供器策略](docs/metadata-provider-policy.md)。
升级、数据库恢复和忘记密码的处理见 [升级、备份与恢复](docs/upgrade-and-recovery.md)。

## ⚙️ 配置

通过环境变量调整，`.env` 里定义的变量会被 `compose.yaml` 透传。运行时更细的设置（代理、元数据密钥、命名模板、匹配阈值、后处理开关、扫描规则等 49 项）在 Web 设置页修改，存入 SQLite，改即生效。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `FILEMAID_PORT` | `8080` | HTTP 服务端口 |
| `FILEMAID_MEDIA_ROOT` | `./data/media` | 媒体根目录（仅暴露根目录内相对路径） |
| `FILEMAID_ROOT_WRITABLE` | `false` | 是否允许写操作（重命名/移动/复制/硬链接） |
| `FILEMAID_DB_PATH` | `./config/filemaid.db` | SQLite 数据库文件路径 |
| `FILEMAID_AUTH_ENABLED` | `true` | 是否启用单用户登录；受控开发环境可设 `false` |
| `FILEMAID_SECURE_COOKIES` | `false` | HTTPS 反代后设 `true` 让会话 Cookie 带 Secure |
| `FILEMAID_SCAN_MAX_DEPTH` | `16` | 扫描最大目录深度 |
| `FILEMAID_SCAN_MAX_FILES` | `10000` | 单次扫描最大文件数 |
| `FILEMAID_FFPROBE_PATH` | `ffprobe` | ffprobe 可执行文件路径（媒体探测） |
| `FILEMAID_TMDB_API_KEY` | *(空)* | TMDB API 密钥，未配置时扫描与预览仍可用 |
| `FILEMAID_TVDB_API_KEY` | *(空)* | TVDB v4 API 密钥 |
| `FILEMAID_TVDB_PIN` | *(空)* | TVDB v4 订阅者 PIN（可选） |
| `FILEMAID_OMDB_API_KEY` | *(空)* | OMDb API 密钥 |
| `FILEMAID_TVMAZE_ENABLED` | `true` | 是否启用 TVMaze（无需密钥） |
| `FILEMAID_ANIDB_ENABLED` | `true` | 是否启用 AniDB 索引（无需密钥） |
| `FILEMAID_NAMING_SERIES` | *(内置)* | 剧集命名模板 |
| `FILEMAID_NAMING_MOVIE` | *(内置)* | 电影命名模板 |
| `FILEMAID_NAMING_UNKNOWN` | *(内置)* | 未分类文件命名模板 |

> 网络代理在设置页配置，支持 HTTP 代理（`java.net.http.HttpClient` 不支持 SOCKS，故未提供 SOCKS 选项）。
>
> GitHub 源码与发布镜像**不包含任何 API 密钥**。上表中的密钥变量默认均为空，仅供部署者在自己的 `.env` 中注入；也可登录后在设置页填写。不要提交 `.env`、`config/` 或数据库备份。

## 📐 命名模板

模板只生成媒体根目录内的相对路径，**不执行 Groovy 或任何脚本**，绝对路径 / 越界 / 未知变量一律拒绝。

| 类型 | 默认模板 |
| --- | --- |
| 剧集 | `TV Shows/{title}/Season {season:02}/{title} - S{season:02}{episodes}{extension}` |
| 电影 | `Movies/{title} ({year})/{title} ({year}){extension}` |
| 未分类 | `Unsorted/{original}` |

可用变量：`{title}` · `{year}` · `{season:02}` · `{episodes}` · `{extension}` · `{original}`

媒体信息变量（探测到后可用）：`{resolution}` · `{videoCodec}` · `{videoProfile}` · `{audioCodec}` · `{audioLanguage}` · `{subtitleCodec}` · `{subtitleLanguage}` · `{width}` · `{height}` · `{frameRate}` · `{bitRate}` · `{duration}` · `{fileSize}`

## 🔌 HTTP API

API 根路径为 `/api/v1`。除健康检查、认证状态、首次设置和登录外，默认都需要已认证会话；写操作需要 CSRF Token（从 `XSRF-TOKEN` Cookie 读取后放入 `X-XSRF-TOKEN` 请求头）。

**认证**

| 方法 & 路径 | 用途 |
| --- | --- |
| `GET /api/v1/auth/status` | 查看认证启用、是否已配置账号与当前登录状态 |
| `POST /api/v1/auth/setup` | 首次设置管理员账号（`{ "username", "password" }`，密码 12–128 字符） |
| `POST /api/v1/auth/login` | 表单登录（`username` + `password`） |
| `POST /api/v1/auth/logout` | 退出登录 |

**整理流程**

| 方法 & 路径 | 用途 |
| --- | --- |
| `GET /api/v1/roots` | 列出已配置的存储根目录 |
| `GET /api/v1/roots/{rootId}/directories?path=&query=` | 安全浏览及搜索根目录内的文件夹 |
| `POST /api/v1/roots/{rootId}/scan?path=` | 创建后台扫描任务，返回任务 ID |
| `GET /api/v1/roots/{rootId}/probe?path=` | 探测单个文件的媒体信息（编码 / 分辨率 / 音轨字幕轨） |
| `POST /api/v1/media/parse` | 解析文件名（`{ "names": [...] }`） |
| `POST /api/v1/media/groups/analyze` | 媒体分组 + 字幕/封面/NFO 关联分析 |
| `POST /api/v1/rename-plans/preview` | 生成改名预览（可携带已选元数据；传 `rootId` 触发媒体探测并标记重名冲突） |
| `POST /api/v1/rename-plans` | 生成执行计划（由源/目标/操作类型组成） |
| `POST /api/v1/rename-plans/validate` | 校验计划并发放 15 分钟一次性确认令牌；同时固化可选后处理选项（NFO/封面） |
| `POST /api/v1/rename-plans/execute` | 消费确认令牌，重新校验后执行（MOVE / COPY / HARDLINK + 可选后处理），逐文件返回结果 |

**元数据与匹配**

| 方法 & 路径 | 用途 |
| --- | --- |
| `GET /api/v1/metadata/providers` | 元数据提供器状态 |
| `GET /api/v1/metadata/search?query=&type=&locale=&limit=` | 聚合搜索五个提供器候选（`type` = `MOVIE` / `SERIES`） |
| `POST /api/v1/metadata/match` | 解析文件名并自动匹配排序候选（标题/年份相似度） |
| `POST /api/v1/metadata/providers/{providerId}/test` | 使用当前动态设置测试提供器连接 |
| `PUT /api/v1/match-decisions` | 保存用户确认的匹配决策 |
| `GET /api/v1/match-decisions` | 查询匹配决策（可选 `source` 参数） |

**历史、命名与设置**

| 方法 & 路径 | 用途 |
| --- | --- |
| `GET /api/v1/operations` | 查询操作历史（含 NFO/封面后处理记录） |
| `POST /api/v1/operations/{id}/undo` | 撤销某个操作（MOVE 移回；COPY/HARDLINK 删除目标） |
| `GET /api/v1/naming/templates` | 查看当前生效的命名模板 |
| `GET /api/v1/settings` / `PUT /api/v1/settings` | 读取和保存设置（密钥读取时脱敏） |
| `GET /api/v1/settings/schema` | 设置目录（分类、类型、默认值、范围、枚举及运行时接入状态） |
| `GET /api/v1/system/health` | 服务健康检查 |
| `GET /api/v1/system/diagnostics` | 检查数据库、FFprobe 和媒体根目录状态（不暴露绝对路径） |
| `GET /actuator/health` | Spring Actuator 健康端点 |

## 🛠️ 本地开发

后端需要 JDK 17，前端需要 Node.js 22。旧引擎只作为隔离的算法参考模块，不进入运行时。

```powershell
# 运行测试
.\gradlew.bat :modules:application:test :modules:infrastructure:test :modules:server:test

# 启动服务（默认媒体根 ./data/media）
.\gradlew.bat :modules:server:bootRun

# 构建 Vue 前端（输出到 server 静态资源目录）
cd web
npm install
npm run build
```

> `bootRun` 的工作目录是 `modules/server`，本地开发用相对路径配置媒体根目录或数据库时，会相对该目录解析；建议本地冒烟使用正斜杠绝对路径（如 `D:/path/media`）。

## 🏗️ 项目结构

模块化单体，依赖单向：`server → infrastructure → application → core`。

| 模块 | 职责 |
| --- | --- |
| `core` | 无框架领域模型与约束 |
| `application` | 用例、端口与流程编排 |
| `infrastructure` | 文件系统、解析器、元数据、媒体探测、命名模板与 SQLite 适配器 |
| `server` | Spring Boot HTTP 服务、安全与配置入口 |
| `legacy-engine` | 旧引擎源码，**仅作算法参考**，不被依赖、不进入镜像 |

## 🗺️ 路线图

- [x] 只读扫描 / 解析 / 命名预览 / 元数据匹配 / 媒体分组
- [x] 接入 FFprobe / 媒体信息（分辨率、编解码器、音轨字幕轨）
- [x] 执行写操作：重命名、移动、复制、硬链接（默认不覆盖，确认令牌 + 执行前重校验）
- [x] 操作历史与撤销
- [x] SQLite 设置仓库（49 项）、匹配决策与元数据缓存持久化
- [x] 后处理：Kodi NFO + 封面下载（固化进确认计划，历史可撤销）
- [x] Vue 3 前端 + 单用户登录 / 会话 / CSRF
- [x] 反向代理 / HTTPS 部署说明
- [x] 持久化后台任务、断线恢复、任务取消与多标签页编辑锁
- [x] 批量编辑、伴随文件折叠展示、刷新保留页面与前端 API 自动化测试
- [x] 数据库备份、密码修改/离线恢复说明与整理完成 Webhook 通知

## 🔒 安全说明

- 未经一次性确认令牌（15 分钟有效、一次性消费），不会对媒体目录执行任何写操作。
- 所有输入路径都会规范化，并校验仍在允许的根目录内。
- 改名预览与实际执行之间会重新校验源文件、目标与冲突；默认不覆盖已有文件。
- 后处理选项（NFO/封面）在校验时固化进确认计划，执行阶段无法再改动。
- 登录使用 BCrypt 密码、会话 Cookie（HttpOnly + SameSite）与 CSRF Token 防护。
- API 密钥通过环境变量或 SQLite 设置注入，读取接口始终脱敏；数据库文件不应公开或提交到版本库。

## 📜 License

待定（TBD）。
