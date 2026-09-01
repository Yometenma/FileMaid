# FileMaid

FileMaid is a self-hosted media-file organizer. The project is currently in its read-only foundation phase: it can start an HTTP service, expose configured storage roots, and scan mounted directories without changing files.

## Modules

- `core`: framework-free models and invariants.
- `application`: use cases and ports.
- `infrastructure`: filesystem and future database/provider adapters.
- `server`: Spring Boot HTTP application.
- `legacy-engine`: the existing media organization engine, isolated for gradual integration.

See [docs/architecture.md](docs/architecture.md) and [AGENTS.md](AGENTS.md) for the current design and development status.

## Local verification

```powershell
.\gradlew.bat :modules:application:test :modules:infrastructure:test :modules:server:test
.\gradlew.bat :modules:server:bootRun
```

To include the existing engine's season/episode matcher during local development:

```powershell
.\gradlew.bat :modules:server:bootRun -PwithLegacyEngine
```

The default media root is `./data/media`. Override it with `FILEMAID_MEDIA_ROOT`.

## HTTP API

- `GET /api/v1/system/health`
- `GET /api/v1/roots`
- `GET /api/v1/roots/{rootId}/scan?path=relative/path`
- `POST /api/v1/media/parse`
- `POST /api/v1/media/groups/analyze`
- `POST /api/v1/rename-plans/preview`
- `GET /api/v1/metadata/providers`
- `GET /api/v1/metadata/search`
- `GET /api/v1/naming/templates`
- `GET /actuator/health`

The current API is intentionally read-only.

## Docker

Create `data/media`, place test media inside it, then run:

```text
docker compose up --build
```

The example Compose file mounts the media directory read-only. Do not change it to read-write until preview, confirmation, journaling, and undo have been implemented.
# TMDB 元数据搜索

启动时设置 `FILEMAID_TMDB_API_KEY`，Web 工作台即可搜索电影和剧集候选。带旧引擎构建会优先复用原有 TMDB 客户端，精简 Docker 构建则使用轻量兼容客户端。密钥不会写入源码；未配置时扫描和预览功能仍可正常使用。

## 命名模板

可通过 `FILEMAID_NAMING_SERIES`、`FILEMAID_NAMING_MOVIE`、`FILEMAID_NAMING_UNKNOWN` 覆盖默认模板。支持 `{title}`、`{year}`、`{season:02}`、`{episodes}`、`{extension}`、`{original}`。模板只生成媒体根目录内的相对路径，不执行 Groovy 或其他脚本。
