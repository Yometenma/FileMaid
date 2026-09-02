<div align="center">

# FileMaid

Safely organize movies, series, anime, and companion files from a Web UI.

[简体中文](README.md) · [Quick start](#quick-start) · [User guide](docs/usage.md) · [Configuration](docs/configuration.md) · [Changelog](CHANGELOG.md)

[![Release](https://img.shields.io/github/v/release/Yometenma/FileMaid)](https://github.com/Yometenma/FileMaid/releases/latest)
[![CI](https://github.com/Yometenma/FileMaid/actions/workflows/ci.yml/badge.svg)](https://github.com/Yometenma/FileMaid/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/GHCR-linux%2Famd64%20%7C%20arm64-blue)](https://github.com/Yometenma/FileMaid/pkgs/container/filemaid)

</div>

FileMaid is a self-hosted media file organizer. It scans server directories, identifies media, matches metadata, and presents a clear source-to-destination preview. Files are moved, copied, or hard-linked only after explicit confirmation.

It is designed for occasional cleanup of download folders before Jellyfin, Emby, or Plex scans the resulting library.

## Highlights

- Browse and scan media directories mounted into Docker.
- Identify movies, series, anime, subtitles, artwork, and NFO files.
- Search TMDB, TheTVDB, OMDb, TVMaze, and AniDB.
- Use Jellyfin, Emby, Plex, or custom safe naming templates.
- Validate traversal, collisions, permissions, and file changes before execution.
- Move, copy, or hard-link files, with undo when it remains safe.
- Optionally generate Kodi NFO files, posters, and fanart.
- Background tasks, cancellation, history, and live logs in the Web UI.
- Single-user authentication. No API key is embedded in the source or image.

## Quick start

### 1. Download FileMaid

```bash
git clone https://github.com/Yometenma/FileMaid.git
cd FileMaid
mkdir -p config data/media
```

Place a few files in `data/media`, or replace that host path in `compose.yaml` with your actual media directory.

### 2. Start safely in read-only mode

```bash
docker compose up -d
```

Open <http://localhost:8081> and create the administrator account. The password must contain at least 12 characters.

The media mount is read-only by default. You can browse, scan, match, and preview paths without allowing FileMaid to modify files.

### 3. Review the result

On the Organize page, select a directory, scan it, review metadata and destination paths, then run dry validation. See the [user guide](docs/usage.md) for the complete workflow.

### 4. Enable writes when ready

Edit `compose.yaml`:

```yaml
environment:
  FILEMAID_ROOT_WRITABLE: "true"
volumes:
  - ./data/media:/media:rw
```

Recreate the container:

```bash
docker compose up -d
```

Start with a small set of disposable test files. Existing targets are not overwritten by default.

## Common deployment changes

Mount another directory with `/your/media/path:/media:ro`. Pin a release with:

```bash
FILEMAID_VERSION=1.0.0 docker compose up -d
```

To update a pinned deployment, change the version and run `docker compose pull` followed by `docker compose up -d`.

Configure HTTPS before exposing FileMaid outside your LAN. See [reverse proxy and HTTPS](docs/reverse-proxy.md).

## Metadata and API keys

TVMaze and AniDB do not require API keys. TMDB, TheTVDB, and OMDb credentials can be entered under Settings → Metadata providers. Local scanning, parsing, and naming previews work without them.

Never commit `.env`, `config/`, or secrets. See the [metadata provider policy](docs/metadata-provider-policy.md) for attribution, caching, and rate-limit notes.

## Documentation

| Document | Contents |
| --- | --- |
| [User guide](docs/usage.md) | Scan, match, execute, logs, and undo |
| [Configuration](docs/configuration.md) | Mounts, environment variables, runtime settings, and templates |
| [Reverse proxy and HTTPS](docs/reverse-proxy.md) | Nginx, Caddy, and secure cookies |
| [Upgrade and recovery](docs/upgrade-and-recovery.md) | Container upgrades and lost-password handling |
| [Metadata provider policy](docs/metadata-provider-policy.md) | API keys, caching, limits, and attribution |
| [Architecture](docs/architecture.md) | Module boundaries, safety model, and development |
| [HTTP API](docs/api.md) | Authentication, tasks, organization, and settings endpoints |

## Platforms

The official `ghcr.io/yometenma/filemaid` image supports `linux/amd64` and `linux/arm64`. FFmpeg and FFprobe are included.

## Support and contributions

Check the Logs page and Settings → System diagnostics first. Then open a [GitHub Issue](https://github.com/Yometenma/FileMaid/issues) with the version, reproduction steps, and sanitized logs. Issues and pull requests are welcome.

## License

FileMaid is licensed under the [MIT License](LICENSE).
