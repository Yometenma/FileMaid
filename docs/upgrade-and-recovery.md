# 升级、备份与恢复

## 升级容器

FileMaid 的配置、管理员账号、历史和缓存都位于 `/config/filemaid.db`。升级前先在设置页创建数据库备份，并保留宿主机的 `config/` 目录。

```bash
docker compose pull
docker compose up -d
docker compose ps
```

固定版本时，在 `.env` 写入 `FILEMAID_VERSION=0.1.1`；确认新版本正常后再更新该值。不要删除或重新创建 `config/`。

## 恢复数据库

为避免运行中的 SQLite 连接覆盖恢复内容，恢复必须在容器停止时进行：

```bash
docker compose down
cp config/backups/filemaid-YYYYMMDD-HHMMSS.db config/filemaid.db
docker compose up -d
```

恢复前建议再复制一份当前 `config/filemaid.db`。备份目录与数据库位于同一个宿主机 `config/` 挂载中，不会进入镜像或 Git 仓库。

## 忘记管理员密码

当前版本不提供绕过登录的在线重置接口，避免任何能远程接管管理员账号的后门。恢复方式是停止服务，恢复到仍记得密码的数据库备份；若没有可用备份，只能移走数据库并重新首次设置，这会同时失去设置和历史记录。

## API 密钥

发布镜像不包含任何第三方 API 密钥。TMDB、TheTVDB 和 OMDb 密钥由部署者在设置页或私有 `.env` 中自行填写；`.env` 与 `config/` 都不应提交。TVMaze 与 AniDB 无需密钥。

