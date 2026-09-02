# FileMaid 反向代理与 HTTPS

FileMaid 默认监听 `8080`。公网或家庭网络远程访问时，应只让反向代理对外提供 HTTPS，不要直接暴露 8080 端口。

## Nginx

```nginx
server {
    listen 443 ssl http2;
    server_name filemaid.example.com;

    ssl_certificate     /etc/letsencrypt/live/filemaid.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/filemaid.example.com/privkey.pem;

    client_max_body_size 1m;
    proxy_read_timeout 300s;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## Caddy

```caddyfile
filemaid.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

通过 HTTPS 部署时设置：

```yaml
environment:
  FILEMAID_SECURE_COOKIES: "true"
```

首次访问会进入管理员账号设置。FileMaid 只支持一个本地管理员账号；不要与其他服务共用密码。`/config/filemaid.db` 包含账号哈希、API 密钥、设置和操作历史，应限制宿主机文件权限。

建议仅信任由你控制的反向代理，不要接受来自公网客户端自行提供的 `X-Forwarded-*` 请求头。
