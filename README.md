# 奥飞新材官网与内容管理系统

奥飞新材官网（`alfy-web`）、内容管理后台（`alfy-admin`）与 Spring Boot API（`alfy-api`）的单仓库项目。后端提供内容从“草稿 → 发布/下线 → 官网公开读取”的完整数据能力，并通过 Flyway 管理 MySQL 数据库结构。

> 本文以当前 `alfy-api` 源码和 V1–V15 数据库迁移为准。所有产品参数、客户案例、合作方 Logo、技术指标和联系方式须经企业审核后才可发布。

## 目录

- [技术栈与目录](#技术栈与目录)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [API 通用约定](#api-通用约定)
- [认证与权限](#认证与权限)
- [公开 API](#公开-api)
- [后台 API](#后台-api)
- [请求体字段](#请求体字段)
- [调用示例](#调用示例)
- [数据库说明](#数据库说明)
- [前后端联调](#前后端联调)
- [测试与排错](#测试与排错)

## 技术栈与目录

| 模块 | 位置 | 技术 |
| --- | --- | --- |
| 官网 | `alfy-web/` | Nuxt 3、Vue 3、TypeScript |
| 管理后台 | `alfy-admin/` | Vue 3、Vben Admin、Element Plus |
| 后端 | `alfy-api/` | Spring Boot 3、Spring Security、MyBatis-Plus、Flyway |
| 数据库 | MySQL 8+ | UTF-8（`utf8mb4`） |

后端源代码位于 `alfy-api/src/main/java/com/alfy/api/`：

```text
controller/  HTTP 接口
service/     业务规则、发布状态、审计与富文本清洗
mapper/      MyBatis-Plus 数据访问
entity/      表映射实体
dto/         请求和响应对象
security/    JWT 鉴权
config/      安全、跨域、初始管理员与导入配置
```

## 快速启动

### 前置条件

- JDK 17 或更高版本（项目以 Java 17 编译）。
- MySQL 8+；本项目本地已验证 MySQL 9.4。
- Windows 下无需全局安装 Maven，仓库自带 Maven Wrapper。

### 1. 创建数据库

```sql
CREATE DATABASE alfy_site_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

### 2. 配置本地环境

```powershell
Copy-Item alfy-api\src\main\resources\application-local.example.yml `
  alfy-api\src\main\resources\application-local.yml
```

编辑 `application-local.yml`，填写 MySQL 用户名、密码、至少 32 位的随机 JWT 密钥，以及仅首次启动时使用的管理员账号。该文件已被 Git 忽略，绝不能提交真实密钥或密码。

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/alfy_site_dev?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

alfy:
  security:
    jwt-secret: replace-with-at-least-32-random-characters
  admin-bootstrap:
    username: admin
    password: replace-with-a-strong-one-time-password
```

首次启动时，只有 `admin_user` 表为空且同时配置了 `username`、`password` 时，系统才会创建首个管理员；之后请移除或留空这两个值。

### 3. 测试、启动与健康检查

```powershell
Set-Location alfy-api
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

默认地址为 `http://localhost:8080`，健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

预期响应：

```json
{"code":0,"message":"success","data":{"service":"alfy-api","status":"UP","time":"..."}}
```

启动时 Flyway 会自动按顺序执行 `alfy-api/src/main/resources/db/migration/` 下的迁移。**已经执行过的迁移文件不得修改**；新结构必须新增更高版本号的迁移。

## 配置说明

| 配置/环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `server.port` | `8080` | API 服务端口 |
| `SPRING_PROFILES_ACTIVE` | `local` | Spring Profile |
| `ALFY_SECURITY_JWT_SECRET` | 开发用默认值 | HS256 签名密钥；生产必须替换 |
| `ALFY_SECURITY_ACCESS_TOKEN_TTL` | `PT30M` | access token 有效期 |
| `ALFY_SECURITY_REFRESH_TOKEN_TTL` | `P14D` | refresh token 有效期 |
| `ALFY_INITIAL_ADMIN_USERNAME` | 空 | 首次初始化管理员用户名 |
| `ALFY_INITIAL_ADMIN_PASSWORD` | 空 | 首次初始化管理员密码 |
| `ALFY_CONTENT_STORAGE_ROOT` | `./data/alfy/uploads` | 上传文件根目录 |
| `ALFY_CONTENT_IMPORT_ENABLED` | `false` | 是否启用内容草稿导入 |
| `ALFY_WEB_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | 允许跨域的官网和后台地址，逗号分隔 |

生产环境至少应配置：正式数据库连接、随机 JWT 密钥、正式上传目录、`ALFY_WEB_ALLOWED_ORIGINS`，并通过 HTTPS 反向代理暴露服务。

## API 通用约定

### 基础路径与响应

- API 前缀：`/api/v1`
- 公开接口：`/api/v1/public/**`，无需登录。
- 后台接口：`/api/v1/admin/**`，除登录和刷新 token 外都要求 Bearer Token。
- 成功 JSON 响应统一为：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页接口的 `data`：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

日期使用 ISO 8601，例如 `2026-07-22T09:30:00`。

### 错误码

| HTTP | `code` | 含义 |
| --- | ---: | --- |
| 400 | `40000` | 参数或业务前置条件错误 |
| 401 | `40100` | 未登录、Token 无效或过期 |
| 403 | `40300` | 当前角色无权访问 |
| 404 | `40400` | 内容不存在，或公开内容尚未发布 |
| 409 | `40900` | 乐观锁版本冲突或唯一值冲突 |
| 500 | `50000` | 未处理的服务端错误 |

### 分页、状态与版本

- 分页参数统一为 `page`（从 `1` 开始）和 `size`（最大 `100`）。
- 绝大多数发布型内容状态为 `DRAFT`、`PUBLISHED`、`OFFLINE`。
- 创建请求不要传 `version`；更新请求必须回传详情/保存响应的 `version`。版本不一致时接口返回 `40900`，客户端应重新获取详情后再编辑。
- `slug`、`pageKey`、`sectionKey` 只允许小写字母、数字与单个连字符，例如 `aerogel-powder`。
- 富文本字段 `contentHtml` 会被服务端清洗；不得依赖脚本、内联事件或危险 HTML。

## 认证与权限

后台角色为 `SUPER_ADMIN` 和 `CONTENT_ADMIN`，均可访问当前一期后台内容接口。请求头：

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/admin/auth/login` | 否 | 用户名密码登录 |
| POST | `/admin/auth/refresh` | 否 | 用 refresh token 轮换令牌 |
| POST | `/admin/auth/logout` | 是 | 使当前 refresh token 失效 |
| GET | `/admin/auth/me` | 是 | 当前管理员资料 |
| GET | `/admin/user/info` | 是 | Vben Admin 用户资料兼容接口 |
| GET | `/admin/auth/codes` | 是 | Vben Admin 权限码兼容接口 |
| GET | `/admin/menu/all` | 是 | Vben Admin 菜单兼容接口；当前前端路由模式返回空数组 |

登录请求：

```json
{"username":"admin","password":"your-password"}
```

登录响应 `data` 包含 `accessToken`、`refreshToken`、`tokenType`、`expiresIn` 和 `user`。刷新/登出请求体：

```json
{"refreshToken":"登录返回的 refreshToken"}
```

## 公开 API

所有以下接口均不应返回草稿、已下线内容或不应公开的媒体。

### 站点、首页与媒体

| 方法 | 路径 | 查询参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/health` | - | 服务健康状态 |
| GET | `/public/site` | - | Logo、地址、电话、邮箱、备案、隐私政策 |
| GET | `/public/navigation` | - | `{header, footer}` 导航树 |
| GET | `/public/home` | - | Banner、场景、精选案例、技术概览、首页新闻聚合 |
| GET | `/public/home-sections` | - | 已启用的首页固定区块 |
| GET | `/public/hero-slides` | - | 当前有效期内且已发布的首页轮播 |
| GET | `/public/media/{mediaId}` | - | 已被发布内容引用的媒体二进制文件 |
| GET | `/public/pages/{pageKey}/hero` | - | 已发布的页面顶部首屏 |
| GET | `/public/pages/{pageKey}` | - | 已发布的可编辑单页正文 |
| GET | `/public/pages/technology` | - | 已发布技术研发页 |

`/public/media/{mediaId}` 直接返回文件流，不使用 JSON 包装，并带公共缓存头；不存在、未关联发布内容或文件丢失时不可读取。

### 产品、场景与案例

| 方法 | 路径 | 查询参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/public/product-categories` | - | 已启用产品分类 |
| GET | `/public/products` | `category`、`featured`、`page`、`size` | 已发布产品分页；分类传 slug |
| GET | `/public/products/{slug}` | - | 产品详情、参数、关联场景和案例 |
| GET | `/public/application-scenes` | - | 已发布应用场景 |
| GET | `/public/cases` | `scene`、`product`、`featured`、`page`、`size` | 已发布案例分页；场景/产品传 slug |
| GET | `/public/cases/{slug}` | - | 案例详情与关联产品 |

### 新闻、合作伙伴与询盘

| 方法 | 路径 | 查询参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/public/article-categories` | - | 文章分类 |
| GET | `/public/articles` | `category`、`page`、`size` | 已发布文章分页；分类传 category code |
| GET | `/public/articles/{identifier}` | - | 文章详情；兼容 slug 或数字标识 |
| GET | `/public/partners` | - | 按排序返回已发布合作伙伴 |
| POST | `/public/inquiries` | JSON Body | 提交官网咨询，不提供公开查询 |

询盘请求：

```json
{
  "name": "张三",
  "company": "某公司",
  "phone": "13800000000",
  "email": "name@example.com",
  "inquiryType": "PROJECT_SOLUTION",
  "message": "希望获取项目方案",
  "productId": null,
  "sourceUrl": "https://www.alfy.com.cn/products",
  "utm": {"source": "baidu", "medium": "cpc"},
  "privacyAccepted": true
}
```

`phone` 与 `email` 至少填写一个，`privacyAccepted` 必须为 `true`。成功响应仅返回 `inquiryNo`。生产环境仍应在网关/WAF 增加 IP 限流、验证码与敏感词策略。

## 后台 API

下表中的后台接口都要求 Bearer Token，认证接口除外。`GET /{resource}` 返回分页数据的资源会在“查询参数”列标明。

### 内容与分类

| 资源 | 接口 | 查询参数/说明 |
| --- | --- | --- |
| 产品分类 | `GET/POST /admin/product-categories`、`GET/PUT/DELETE /admin/product-categories/{id}` | 列表：`keyword,page,size`；分类用 `enabled` 控制启用 |
| 产品 | `GET/POST /admin/products`、`GET/PUT/DELETE /admin/products/{id}`、`POST /admin/products/{id}/publish`、`POST /admin/products/{id}/offline` | 列表：`status,categoryId,keyword,page,size` |
| 应用场景 | `GET/POST /admin/application-scenes`、`GET/PUT/DELETE /admin/application-scenes/{id}`、`POST /admin/application-scenes/{id}/publish`、`POST /admin/application-scenes/{id}/offline` | 列表：`status,keyword,page,size` |
| 案例 | `GET/POST /admin/cases`、`GET/PUT/DELETE /admin/cases/{id}`、`POST /admin/cases/{id}/publish`、`POST /admin/cases/{id}/offline` | 列表：`status,sceneId,keyword,page,size` |
| 新闻文章 | `GET/POST /admin/articles`、`GET/PUT/DELETE /admin/articles/{id}`、`POST /admin/articles/{id}/publish`、`POST /admin/articles/{id}/offline` | 列表：`status,categoryId,keyword,page,size` |
| Banner | `GET/POST /admin/banners`、`GET/PUT/DELETE /admin/banners/{id}`、`POST /admin/banners/{id}/publish`、`POST /admin/banners/{id}/offline` | 列表可传 `status`，不分页 |
| 合作伙伴 | `GET/POST /admin/partners`、`GET/PUT/DELETE /admin/partners/{id}`、`POST /admin/partners/{id}/publish`、`POST /admin/partners/{id}/offline` | 列表：`status,keyword,page,size` |
| 可编辑单页 | `GET/POST /admin/pages`、`GET/PUT/DELETE /admin/pages/{id}`、`POST /admin/pages/{id}/publish`、`POST /admin/pages/{id}/offline` | 列表：`status,keyword,page,size` |

发布前校验示例：产品要求分类已启用；案例要求所属场景已发布；文章要求标题、slug、正文与分类；首页 Banner 要求必要的标题和桌面图；单页和合作伙伴要求核心文字字段完整。

### 首页、技术与站点

| 资源 | 接口 | 说明 |
| --- | --- | --- |
| 首页固定区块 | `GET/POST /admin/home-sections`、`GET/PUT/DELETE /admin/home-sections/{id}` | 标题、图片、CTA、启用状态与排序 |
| 技术研发页 | `GET/PUT /admin/technologies`、`POST /admin/technologies/publish`、`POST /admin/technologies/offline` | 单例资源；首次 `PUT` 创建草稿，之后更新需传 `version` |
| 单页首屏 | `GET/POST /admin/page-heroes`、`GET/PUT/DELETE /admin/page-heroes/{id}`、`POST /admin/page-heroes/{id}/publish`、`POST /admin/page-heroes/{id}/offline` | 用 `pageKey` 区分 about、technology 等页面 |
| 站点设置 | `GET/PUT /admin/site-settings` | 单例资源；首次保存创建，后续保存带 `version` |
| 导航 | `GET/POST /admin/navigations`、`GET/PUT/DELETE /admin/navigations/{id}` | 列表必须传 `area=HEADER` 或 `area=FOOTER` |
| 301 规则 | `GET/POST /admin/redirects`、`GET/PUT/DELETE /admin/redirects/{id}` | `sourcePath` 必须是站内绝对路径且无查询参数 |

### 素材与询盘

| 资源 | 接口 | 说明 |
| --- | --- | --- |
| 素材列表 | `GET /admin/media` | `keyword,page,size`；返回后台文件 URL |
| 上传素材 | `POST /admin/media` | `multipart/form-data`；字段 `file`，可选 `altText` |
| 后台读取素材 | `GET /admin/media/{id}/file` | 返回文件流；需后台权限 |
| 询盘列表 | `GET /admin/inquiries` | `status,keyword,page,size` |
| 询盘详情 | `GET /admin/inquiries/{id}` | 返回完整线索和后台备注 |
| 更新询盘 | `PUT /admin/inquiries/{id}` | Body 为 `status`、`adminNote` |

上传文件类型限于 JPG、PNG、WebP、GIF、MP4、PDF，单文件最大 30 MB。`altText` 应填写图片替代文本或素材说明。

## 请求体字段

所有下列对象均为 JSON；`version` 仅更新时必传。媒体字段为已经上传的 `media_asset.id`。

| 对象 | 主要字段 |
| --- | --- |
| 产品分类 | `name, slug, summary, coverMediaId, sortOrder, enabled, version` |
| 产品 | `categoryId, name, slug, summary, contentHtml, contentText, coverMediaId, features[], specifications, featured, sortOrder, seoTitle, seoDescription, seoKeywords, sceneIds[], version` |
| 应用场景 | `name, slug, slogan, summary, painPoint, solution, contentHtml, coverMediaId, featured, sortOrder, seoTitle, seoDescription, seoKeywords, version` |
| 案例 | `sceneId, title, slug, clientName, location, summary, background, customerNeed, solution, implementation, resultSummary, contentHtml, coverMediaId, featured, sortOrder, seo*, productIds[], version` |
| 文章 | `title, slug, summary, contentHtml, contentText, coverMediaId, authorName, sourceUrl, sourcePublishedAt, categoryIds[], sortOrder, featured, homeSlot, homeSortOrder, seo*, version` |
| Banner | `eyebrow, title, highlightText, summary, desktopMediaId, mobileMediaId, primaryActionLabel, primaryActionTarget, secondaryActionLabel, secondaryActionTarget, sortOrder, startsAt, endsAt, version` |
| 首页区块 | `sectionKey, label, eyebrow, title, highlightText, description, imageMediaId, mobileMediaId, buttonLabel, buttonTarget, enabled, sortOrder, version` |
| 技术研发页 | `eyebrow, title, highlightText, summary, ctaLabel, ctaTarget, capabilityRows, pillars, contentHtml, version` |
| 页面首屏 | `pageKey, eyebrow, title, highlightText, summary, backgroundMediaId, mobileBackgroundMediaId, primaryActionLabel, primaryActionTarget, secondaryActionLabel, secondaryActionTarget, version` |
| 可编辑单页 | `pageKey, title, category, summary, contentHtml, coverMediaId, featured, sortOrder, seo*, version` |
| 合作伙伴 | `name, slug, category, summary, logoMediaId, websiteUrl, featured, sortOrder, seo*, version` |
| 站点设置 | `companyName, logoMediaId, address, servicePhone, serviceEmail, wechatQrMediaId, icpNumber, copyrightText, privacyPolicyUrl, version` |
| 导航项 | `navigationArea, parentId, label, target, sortOrder, highlighted, enabled, version` |
| 301 规则 | `sourcePath, targetUrl, enabled, version` |
| 询盘跟进 | `status, adminNote`；状态：`NEW, CONTACTED, QUALIFIED, CLOSED, SPAM` |

`seo*` 是 `seoTitle`、`seoDescription`、`seoKeywords` 的简写。`specifications`、`capabilityRows` 和 `pillars` 为 JSON 数组或对象。

## 调用示例

以下示例的 `API` 为 `http://localhost:8080/api/v1`。

### 1. 登录并保存令牌

```powershell
$login = Invoke-RestMethod -Method Post -ContentType 'application/json' `
  -Uri 'http://localhost:8080/api/v1/admin/auth/login' `
  -Body '{"username":"admin","password":"your-password"}'

$accessToken = $login.data.accessToken
$headers = @{ Authorization = "Bearer $accessToken" }
```

### 2. 上传素材

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/media?altText=气凝胶粉体' `
  -Headers $headers `
  -Form @{ file = Get-Item '.\aerogel-powder.jpg' }
```

保存响应 `data.id`，后续创建 Banner、产品、案例等时填入 `coverMediaId` 或对应图片字段。

### 3. 创建并发布产品

```powershell
$product = @{
  categoryId = 1
  name = '气凝胶粉体'
  slug = 'aerogel-powder'
  summary = '面向涂料、浆料和复合材料体系的轻质多孔基础材料。'
  coverMediaId = 12
  features = @('轻质多孔', '复合开发')
  specifications = @{}
  featured = $true
  sortOrder = 1
  sceneIds = @()
} | ConvertTo-Json -Depth 8

$created = Invoke-RestMethod -Method Post -ContentType 'application/json' `
  -Uri 'http://localhost:8080/api/v1/admin/products' -Headers $headers -Body $product

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/admin/products/$($created.data.id)/publish" `
  -Headers $headers
```

发布前必须先创建并启用产品分类。编辑时先获取详情，使用返回的 `version`：

```powershell
$detail = Invoke-RestMethod "http://localhost:8080/api/v1/admin/products/1" -Headers $headers
# 修改 $detail.data 后，在 PUT 请求体中加入 version = $detail.data.version
```

### 4. 创建首页 Banner

```json
{
  "eyebrow": "技术发布",
  "title": "新一代气凝胶",
  "highlightText": "及其复合材料技术",
  "summary": "连接科研源头与真实工程场景。",
  "desktopMediaId": 12,
  "mobileMediaId": 13,
  "primaryActionLabel": "查看产品",
  "primaryActionTarget": "/products",
  "secondaryActionLabel": "获取方案",
  "secondaryActionTarget": "#inquiry",
  "sortOrder": 1,
  "startsAt": null,
  "endsAt": null
}
```

将其 POST 到 `/admin/banners`，再 POST `/admin/banners/{id}/publish`。若设定 `startsAt`、`endsAt`，公开接口只会在有效时间窗口中返回它。

### 5. 官网读取

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/public/products?featured=true&page=1&size=12'
Invoke-RestMethod 'http://localhost:8080/api/v1/public/home'
Invoke-RestMethod 'http://localhost:8080/api/v1/public/articles?page=1&size=10'
```

## 数据库说明

### 迁移版本

| 版本 | 内容 |
| --- | --- |
| V1 | 管理员账号 |
| V2–V5 | 新闻、分类、媒体、导入记录、刷新令牌与操作日志 |
| V6–V10 | 产品分类/产品、应用场景/案例、关联表和乐观锁 |
| V11 | 首页 Banner、技术研发页、询盘 |
| V12 | 站点设置、导航、页面首屏 |
| V13 | 合作伙伴、可编辑单页 |
| V14 | 301 重定向规则 |
| V15 | 首页固定区块 |

### 表与关系

| 表 | 作用 | 关键关系/字段 |
| --- | --- | --- |
| `admin_user` | 管理员账号 | `role`、BCrypt 密码哈希、状态、最后登录时间 |
| `admin_refresh_token` | 刷新令牌 | 仅保存 SHA-256 摘要；关联 `admin_user` |
| `admin_operation_log` | 操作审计 | 记录操作者、动作、资源类型和资源 ID |
| `article_category` | 新闻分类 | 分类 code、名称、排序 |
| `article` | 新闻文章 | slug、正文、发布状态、首页展示位、SEO |
| `article_category_relation` | 新闻多分类关系 | `article` ↔ `article_category` |
| `article_media` | 新闻媒体关系 | 文章的图片/视频/附件排序 |
| `media_asset` | 上传素材元数据 | 存储键、MIME、文件大小、哈希、替代文本 |
| `product_category` | 产品分类 | slug、启用状态、封面与排序 |
| `product` | 产品 | 分类、正文、规格 JSON、发布状态、SEO |
| `application_scene` | 应用场景 | slug、痛点、方案、首页推荐 |
| `case_project` | 案例 | 所属场景、方案/实施/结果、发布状态 |
| `product_scene_rel` | 产品—场景关系 | 多对多关系与排序 |
| `product_case_rel` | 产品—案例关系 | 多对多关系与排序 |
| `hero_slide` | 首页 Banner | PC/移动图、CTA、有效期、发布状态 |
| `home_section` | 首页固定区块 | 标题、图片、CTA、启用状态与排序 |
| `technology_page` | 技术研发单页 | JSON 能力表、合作栏目、富文本与发布状态 |
| `site_setting` | 站点单例配置 | Logo、地址、联系方式、备案、隐私政策 |
| `site_navigation_item` | 顶部/页脚导航 | 区域、父节点、跳转、排序、启用状态 |
| `page_hero` | 页面顶部首屏 | `page_key`、背景图、CTA、发布状态 |
| `content_page` | 可编辑单页 | `page_key`、正文、封面、SEO、发布状态 |
| `partner` | 合作伙伴 | Logo、官网地址、排序、发布状态 |
| `inquiry` | 官网咨询线索 | 联系信息、UTM、隐私同意、处理状态和备注 |
| `redirect_rule` | 301 跳转规则 | 来源路径、目标地址、启用状态 |
| `content_import_record` | 内容导入记录 | 防止重复导入、保存来源信息 |

大多数内容表有 `deleted`（逻辑删除）、`version`（乐观锁）、`created_at`、`updated_at`。发布型资源还会有 `status` 与 `published_at`。

## 前后端联调

### 官网 `alfy-web`

```powershell
Set-Location alfy-web
Copy-Item .env.example .env
npm ci
npm run dev
```

`.env`：

```env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api/v1
```

### 管理后台 `alfy-admin`

```powershell
Set-Location alfy-admin
corepack enable
pnpm install
pnpm -F @vben/web-ele run dev
```

创建 `alfy-admin/apps/web-ele/.env.local`：

```env
VITE_GLOB_API_URL=http://localhost:8080/api/v1/admin
VITE_NITRO_MOCK=false
```

当前前端中仍有部分页面使用 `cmsState`、静态数据或本地 Mock。后端端点已按上述资源提供；联调时需要将对应页面的数据读取、保存、发布、下线和上传逻辑替换为真实 API 调用，并在编辑页面保存 `version`。

## 测试与排错

```powershell
Set-Location alfy-api
.\mvnw.cmd test
```

常见问题：

| 现象 | 排查方式 |
| --- | --- |
| 启动时 Flyway 校验失败 | 不要修改已执行迁移；新增更高版本迁移。仅本地开发库出现失败迁移记录时，确认未建表后再执行 Flyway repair。 |
| 后台返回 401 | 检查 `Authorization: Bearer {accessToken}`，或调用刷新接口获取新令牌。 |
| 后台返回 409 | 先 GET 详情，使用最新的 `version` 合并修改后再 PUT。 |
| 官网读不到内容/图片 | 确认资源已 `publish`；关联媒体存在且文件位于 `ALFY_CONTENT_STORAGE_ROOT`。 |
| 跨域被拦截 | 将实际官网/后台 origin 加到 `ALFY_WEB_ALLOWED_ORIGINS`，不要在生产使用通配符。 |
| 上传失败 | 检查 MIME 类型、30 MB 限制和上传目录写权限。 |

其他历史设计和联调资料见 `docs/` 目录，尤其是：[API 契约](docs/api-contract.md)、[前端联调说明](docs/前端联调_API说明.md)、[首页内容与询盘说明](docs/首页可编辑内容与询盘接口说明.md) 和 [数据库表结构说明](docs/后端数据库表结构说明.md)。
