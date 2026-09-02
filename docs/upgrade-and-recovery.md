# 升级与恢复

## 升级容器

FileMaid 的配置、管理员账号、历史和缓存都位于 `/config/filemaid.db`。升级容器时保留宿主机的 `config/` 目录即可。

```bash
docker compose pull
docker compose up -d
docker compose ps
```

固定版本时，在 `.env` 写入明确版本号（例如 `FILEMAID_VERSION=0.1.3`）；确认新版本正常后再更新该值。不要删除或重新创建 `config/`。

## 忘记管理员密码

当前版本不提供绕过登录的在线重置接口，避免任何能远程接管管理员账号的后门。若确实忘记密码，只能先停止服务，把 `config/filemaid.db` 移出原位置，再启动并重新进行首次设置。这会同时清除原有设置和历史记录；确认新实例正常前不要删除移出的数据库文件。

## API 密钥

发布镜像不包含任何第三方 API 密钥。TMDB、TheTVDB 和 OMDb 密钥由部署者在设置页或私有 `.env` 中自行填写；`.env` 与 `config/` 都不应提交。TVMaze 与 AniDB 无需密钥。
