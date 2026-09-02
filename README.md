<div align="center">

# FileMaid

通过 Web UI 安全地整理电影、剧集、动漫和关联文件。

[English](README_EN.md) · [快速开始](#快速开始) · [使用指南](docs/usage.md) · [配置说明](docs/configuration.md) · [更新记录](CHANGELOG.md)

[![Release](https://img.shields.io/github/v/release/Yometenma/FileMaid)](https://github.com/Yometenma/FileMaid/releases/latest)
[![CI](https://github.com/Yometenma/FileMaid/actions/workflows/ci.yml/badge.svg)](https://github.com/Yometenma/FileMaid/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/GHCR-linux%2Famd64%20%7C%20arm64-blue)](https://github.com/Yometenma/FileMaid/pkgs/container/filemaid)

</div>

FileMaid 是一个面向自托管媒体库的文件整理工具。它会扫描服务器目录、识别媒体、匹配元数据，并在 Web 页面中展示清晰的“源文件 → 目标路径”预览。只有在你确认后，它才会执行移动、复制或硬链接。

适合偶尔集中整理下载目录，再交给 Jellyfin、Emby 或 Plex 扫描的使用方式。

## 功能亮点

- 浏览和扫描 Docker 挂载的媒体目录。
- 识别电影、剧集、动漫、字幕、封面和 NFO。
- 支持 TMDB、TheTVDB、OMDb、TVMaze 和 AniDB 元数据。
- 使用 Jellyfin、Emby、Plex 预设或自定义安全命名模板。
- 执行前检查越界、重名、权限和文件变化，默认不覆盖已有文件。
- 支持移动、复制、硬链接，以及安全条件满足时的撤销。
- 可选生成 Kodi NFO、海报和背景图。
- 后台任务、进度、取消、历史记录和 Web 实时日志。
- 单用户登录；API 密钥由部署者自行配置，仓库和镜像均不内置密钥。

## 快速开始

### 1. 下载项目

```bash
git clone https://github.com/Yometenma/FileMaid.git
cd FileMaid
mkdir -p config data/media
```

把准备整理的文件放入 `data/media`，或者稍后把 Compose 中的该目录替换为你的真实媒体路径。

### 2. 先以只读方式启动

```bash
docker compose up -d
```

浏览器打开 <http://localhost:8081>，创建管理员账号。密码至少 12 个字符。

默认媒体挂载为只读。此时可以放心完成目录浏览、扫描、匹配和目标路径预览，但不能修改文件。

### 3. 检查预览

在“整理”页面：

1. 选择媒体目录并扫描。
2. 检查识别结果，必要时手动选择元数据。
3. 检查每一行的源文件和新路径。
4. 运行“干跑校验”，确认没有冲突。

更完整的界面操作见 [使用指南](docs/usage.md)。

### 4. 确认后启用写操作

编辑 `compose.yaml`：

```yaml
environment:
  FILEMAID_ROOT_WRITABLE: "true"
volumes:
  - ./data/media:/media:rw
```

然后重新创建容器：

```bash
docker compose up -d
```

建议先用少量测试文件验证命名结果。FileMaid 默认不会覆盖目标文件。

## 常用部署调整

使用其他媒体目录：

```yaml
volumes:
  - /your/media/path:/media:ro
```

固定版本，避免自动跟随 `latest`：

```bash
FILEMAID_VERSION=1.0.0 docker compose up -d
```

更新到新的固定版本时，修改版本号并运行：

```bash
docker compose pull
docker compose up -d
```

需要局域网之外访问时，请先配置 HTTPS，参阅 [反向代理与 HTTPS](docs/reverse-proxy.md)。

## 元数据与 API Key

TVMaze 和 AniDB 无需 API Key。TMDB、TheTVDB、OMDb 需要由你在“设置 → 元数据源”中填写自己的凭据；不配置它们也不影响本地扫描、解析和命名预览。

不要把 `.env`、`config/` 或任何密钥提交到 Git。各提供器的许可、署名和限流说明见 [元数据提供器策略](docs/metadata-provider-policy.md)。

## 文档

| 文档 | 内容 |
| --- | --- |
| [使用指南](docs/usage.md) | 从扫描到执行、日志与撤销 |
| [配置说明](docs/configuration.md) | 挂载、环境变量、动态设置和命名模板 |
| [反向代理与 HTTPS](docs/reverse-proxy.md) | Nginx、Caddy 和安全 Cookie |
| [升级与恢复](docs/upgrade-and-recovery.md) | 容器升级和忘记密码处理 |
| [元数据提供器策略](docs/metadata-provider-policy.md) | API Key、缓存、限流和署名 |
| [架构说明](docs/architecture.md) | 模块边界、安全模型和开发入口 |
| [HTTP API](docs/api.md) | 认证、任务、整理和设置接口 |

## 支持的平台

官方容器发布到 `ghcr.io/yometenma/filemaid`，支持 `linux/amd64` 与 `linux/arm64`。容器已经包含 FFmpeg/FFprobe。

## 问题反馈与贡献

遇到问题时，请先查看 Web UI 的“日志”页面和“设置 → 系统”诊断，然后在 [GitHub Issues](https://github.com/Yometenma/FileMaid/issues) 提交复现步骤、版本号和已脱敏的日志。

欢迎提交 Issue 和 Pull Request。开发环境与模块说明见 [架构说明](docs/architecture.md)。

## License

FileMaid 使用 [MIT License](LICENSE)。
