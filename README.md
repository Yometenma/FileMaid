<div align="center">

# FileMaid 🧹✨

**自托管的媒体文件整理工具**

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Status](https://img.shields.io/badge/status-early%20development-ff69b4)

</div>

FileMaid 运行在你自己的服务器上，通过 Web UI 帮你扫描媒体目录、解析文件名、匹配 TMDB 元数据，并预览电影、剧集、动漫和字幕的目标路径，以 Docker 作为首选部署方式。

---

## ✨ 核心功能

| 功能 | 说明 |
| --- | --- |
| 🗂️ **浏览目录** | 只读浏览挂载的媒体目录 |
| 🔍 **扫描识别** | 识别电影、剧集、动漫、字幕和关联文件 |
| 🏷️ **解析文件名** | 从文件名提取标题、年份、季数、集数 |
| 🎞️ **媒体探测** | 用 ffprobe 读取分辨率、编解码器、音轨与字幕轨信息 |
| 🎬 **元数据匹配** | 接入 TMDB / TVDB / OMDb / TVMaze / AniDB，聚合搜索候选、自动匹配排序并绑定到源文件 |
| 📐 **命名预览** | 用模板生成电影 / 剧集 / 未分类的目标路径 |
| 🔗 **媒体分组** | 自动把字幕、封面、NFO 等伴随文件关联到对应视频，孤立文件会给出提醒 |
| 🛡️ **安全边界** | 路径越界保护、只读根目录，服务器绝对路径不外泄 |

## ⚠️ 当前状态：早期开发（只读）

FileMaid 目前处于**只读预览**阶段，只会扫描和生成预览清单，**不会修改任何文件**。

重命名、移动、复制、硬链接、操作历史、撤销等功能都在路线图中，尚未上线。
在此之前，你可以放心扫描、预览、核对匹配结果，不会弄乱你的媒体库。

## 🚀 快速开始（Docker）

```bash
mkdir -p data/media   # 放入你的测试媒体文件
docker compose up --build
```

打开 <http://localhost:8080>，在网页工作台里选择根目录、扫描媒体、预览目标路径。

> 示例 Compose 会把媒体目录以**只读**方式挂载。在写操作功能上线之前，请保持只读。

## ⚙️ 配置

通过环境变量调整，`.env` 里定义的变量会被 `compose.yaml` 透传：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `FILEMAID_PORT` | `8080` | HTTP 服务端口 |
| `FILEMAID_MEDIA_ROOT` | `./data/media` | 媒体根目录（仅暴露根目录内相对路径） |
| `FILEMAID_SCAN_MAX_DEPTH` | `16` | 扫描最大目录深度 |
| `FILEMAID_SCAN_MAX_FILES` | `10000` | 单次扫描最大文件数 |
| `FILEMAID_TMDB_API_KEY` | *(空)* | TMDB API 密钥，未配置时扫描与预览仍可用 |
| `FILEMAID_TVDB_API_KEY` | *(空)* | TVDB v4 API 密钥 |
| `FILEMAID_TVDB_PIN` | *(空)* | TVDB v4 订阅者 PIN（可选） |
| `FILEMAID_OMDB_API_KEY` | *(空)* | OMDb API 密钥 |
| `FILEMAID_TVMAZE_ENABLED` | `true` | 是否启用 TVMaze（无需密钥） |
| `FILEMAID_ANIDB_ENABLED` | `true` | 是否启用 AniDB 索引（无需密钥） |
| `FILEMAID_FFPROBE_PATH` | `ffprobe` | ffprobe 可执行文件路径（媒体探测） |
| `FILEMAID_NAMING_SERIES` | *(内置)* | 剧集命名模板 |
| `FILEMAID_NAMING_MOVIE` | *(内置)* | 电影命名模板 |
| `FILEMAID_NAMING_UNKNOWN` | *(内置)* | 未分类文件命名模板 |

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

当前 API 全部为只读，根路径 `/api/v1`：

| 方法 & 路径 | 用途 |
| --- | --- |
| `GET /api/v1/system/health` | 服务健康检查 |
| `GET /api/v1/roots` | 列出已配置的存储根目录 |
| `GET /api/v1/roots/{rootId}/scan?path=` | 扫描根目录下的相对路径 |
| `GET /api/v1/roots/{rootId}/probe?path=` | 探测单个文件的媒体信息（编码 / 分辨率 / 音轨字幕轨） |
| `POST /api/v1/media/parse` | 解析文件名（`{ "names": [...] }`） |
| `POST /api/v1/media/groups/analyze` | 媒体分组 + 字幕关联分析 |
| `POST /api/v1/rename-plans/preview` | 生成改名预览（可携带已选元数据；传 `rootId` 触发媒体探测并标记重名冲突） |
| `POST /api/v1/rename-plans` | 生成不可变执行计划（含确认令牌） |
| `POST /api/v1/rename-plans/validate` | 执行前重新校验计划（源存在、目标无冲突、路径不越界） |
| `GET /api/v1/metadata/providers` | 元数据提供器状态 |
| `GET /api/v1/metadata/search?query=&type=&locale=&limit=` | TMDB 候选搜索（`type` = `MOVIE` / `SERIES`） |
| `POST /api/v1/metadata/match` | 解析文件名并自动匹配排序候选（标题/年份相似度） |
| `GET /api/v1/naming/templates` | 查看当前生效的命名模板 |
| `GET /actuator/health` | Spring Actuator 健康端点 |

## 🛠️ 本地开发

需要 JDK 17。旧引擎（601 个 `net.filemaid` 源文件）是独立兼容模块，通过适配器接入。

```powershell
# 运行测试
.\gradlew.bat :modules:application:test :modules:infrastructure:test :modules:server:test

# 启动服务（默认媒体根 ./data/media）
.\gradlew.bat :modules:server:bootRun
```

## 🏗️ 项目结构

模块化单体，依赖单向：`server → infrastructure → application → core`。

| 模块 | 职责 |
| --- | --- |
| `core` | 无框架领域模型与约束 |
| `application` | 用例、端口与流程编排 |
| `infrastructure` | 文件系统、解析器、元数据、媒体探测与命名模板适配器 |
| `server` | Spring Boot HTTP 服务与配置入口 |
| `legacy-engine` | 现有整理引擎（隔离，通过适配器逐步接入） |

## 🗺️ 路线图

- [x] 只读扫描 / 解析 / 命名预览 / TMDB 匹配 / 媒体分组
- [x] 接入 FFprobe / 媒体信息（分辨率、编解码器、音轨字幕轨）
- [ ] 执行写操作：重命名、移动、复制、硬链接（默认不覆盖）
- [ ] 操作历史 + 任务日志 + 撤销
- [ ] SQLite 持久化配置与匹配决策
- [ ] Vue 3 前端 + 单用户登录与安全会话
- [ ] 反向代理 / HTTPS 部署说明

## 🔒 安全说明

- 未经确认，不会对媒体目录执行任何写操作。
- 所有输入路径都会规范化，并校验仍在允许的根目录内。
- 改名预览与实际执行之间会重新校验源文件、目标与冲突（待实现）。
- 密钥只通过环境变量注入，不写入源码或版本库。

## 📜 License

待定（TBD）。
