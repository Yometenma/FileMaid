# 元数据提供器使用、限流与缓存策略

本文记录 FileMaid 所接入公共元数据服务的官方约束，以及项目应采用的保守实现策略。信息核对日期：2026-09-02。服务条款可能变化，发布版本前应重新检查链接中的官方原文。

## 官方约束摘要

| 提供器 | 许可与署名 | 官方限流信号 | FileMaid 策略 |
| --- | --- | --- | --- |
| TMDB | 非商业开发者 API 可免费使用，但必须署名；About/Credits 中应展示官方要求的声明及合规 Logo。商业用途需另行获得许可。 | 旧的“每 10 秒 40 次”限制已停用，但仍有约每秒 40 次的动态上限；必须尊重 HTTP 429。 | 搜索结果短期缓存；遇到 429 优先读取 `Retry-After`，否则指数退避并加入随机抖动。UI 增加 TMDB Credits。 |
| TheTVDB | API 使用受项目/收入对应的许可层级约束；除非另获批准，向最终用户展示其元数据时需要带直链署名。图片权利不由 API 许可自动授予。 | 官方公开页面没有承诺固定数字上限，并禁止“excessive calls”。 | 默认低并发、缓存搜索与详情、处理 429/5xx；在设置或 Credits 中显示带链接署名。发布者需自行确认适用许可层级。 |
| OMDb | 使用受其 Terms of Use 约束。 | 免费 Key 官方标注每日 1,000 次。 | 每个 Key 设置每日请求预算，缓存成功结果；预算接近耗尽时停止自动匹配并给出明确提示，不以重试消耗剩余额度。 |
| TVMaze | 公共 API 数据采用 CC BY-SA，需要署名与 ShareAlike 合规；可使用 API 返回的 TVMaze URL 完成链接署名。 | 至少每 IP 每 10 秒 20 次；429 后退避。边缘缓存命中可能不计入后端限制。 | 限制并发，处理 429；普通响应至少缓存 1 小时，图片 URL 可长期缓存；UI 提供来源链接。 |
| AniDB | 当前实现只下载官方每日标题数据包，不调用需要注册客户端的 HTTP/UDP 明细 API。 | 标题包明确要求每天最多下载一次；HTTP API 另要求至少 2 秒一次并重度缓存。 | 标题包持久化到 `/config/cache/anidb`，成功文件至少复用 24 小时；下载失败时使用旧缓存，禁止并发重复下载。 |

## 建议的缓存模型

后续实现统一 `MetadataCache` 端口，SQLite 只保存小型 JSON 响应和索引元数据，大型 AniDB 压缩包、封面保留在 `/config/cache`：

- 缓存键：提供器、端点、请求类型、标准化查询、媒体类型、语言。
- 搜索结果建议 TTL：TMDB/TVDB/OMDb 24 小时，TVMaze至少 1 小时；未命中结果使用较短 TTL（例如 1 小时）。
- 详情与稳定 ID 映射建议 TTL：7 天，并允许用户主动刷新。
- 同一缓存键采用 single-flight，避免扫描批次并发击穿。
- 只缓存成功的 2xx 响应；401/403 不缓存，429/5xx 使用退避；可在服务不可用时返回未过期或有限期陈旧缓存。
- API Key、PIN、认证头和代理密码不得出现在缓存键、响应文件名或日志中。
- 提供器端点或语言优先级变化时自动形成新缓存键，无需危险的全量删除。

## 官方资料

- [TMDB Rate Limiting](https://developer.themoviedb.org/docs/rate-limiting)
- [TMDB FAQ 与署名要求](https://developer.themoviedb.org/docs/faq)
- [TheTVDB API 与数据许可](https://thetvdb.com/api-information)
- [TheTVDB Terms of Service](https://www.thetvdb.com/tos)
- [OMDb 免费 Key 日额度](https://www.omdbapi.com/apikey.aspx)
- [OMDb Terms of Use](https://www.omdbapi.com/legal.htm)
- [TVMaze API：缓存、限流与许可](https://www.tvmaze.com/api)
- [AniDB API 与每日标题数据包](https://wiki.anidb.net/API)
- [AniDB HTTP API 限流与缓存](https://wiki.anidb.net/HTTP_API_Definition)

## 发布前检查

1. 设置页或 Credits 页面补齐 TMDB、TheTVDB、TVMaze 的署名与链接。
2. 项目发布者确认 TheTVDB 对应许可层级，以及 TMDB 是否仍属于非商业用途。
3. 在实现统一缓存前，保持自动匹配候选数量较小，避免对五个提供器并发放大请求。
4. 不把第三方图片许可等同于元数据 API 许可；服务器管理员负责确认下载和使用图片的权利。
