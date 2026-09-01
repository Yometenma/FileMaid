# Security Policy

## Supported versions

安全修复仅保证进入最新发布版本。部署者应固定版本并定期查看 GitHub Releases。

## Reporting a vulnerability

请使用 GitHub 仓库的 **Security → Report a vulnerability** 私下报告，不要在公开 Issue 中披露可利用细节、凭据、媒体路径或数据库文件。

报告请包含受影响版本、复现步骤、影响范围及建议修复（如有）。请勿对不属于你的服务器或数据执行测试。

## Secrets

FileMaid 的公开源码和发布镜像不包含元数据服务密钥。部署者提供的密钥保存在自己的 SQLite 数据库或环境变量中，读取 API 会脱敏。请勿上传 `.env`、`config/`、数据库、备份或日志中的隐私数据。
