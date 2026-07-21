# 官网 API 契约（V1）

> 状态：前后端设计已收敛，作为一期接口、数据迁移与联调的唯一基线。依据首页原型和《奥飞新材官网详细设计方案与 API 设计思路》整理；本文件不是已实现接口的声明。
>
> 原则：先冻结前台读取契约和内容归属，再设计 Flyway 迁移、鉴权和后台 CRUD。原型中的示例名称、电话、邮箱、日期、合作方、技术参数和项目描述均不得直接作为生产数据，须由企业书面确认。

## 1. 通用约定

- API 前缀为 `/api/v1`，公开读取接口以 `/public` 开头；后台接口待登录与权限方案确定后再冻结。
- JSON 响应统一为 `{ "code": 0, "message": "success", "data": ... }`；HTTP 状态码表达协议错误，`code` 表达业务错误。该约定与已实现的文章接口一致，不采用字符串 `code: "OK"`，后续可在不破坏兼容性的前提下增加 `requestId`。
- 列表统一采用 `page`（从 `1` 开始）和 `size`；当前及 V1 的分页数据为 `records`、`total`、`page`、`size`，不采用另一套 `items/pageSize` 命名。
- 所有 `imageUrl`、`videoUrl` 必须是受控媒体 URL 或 CDN URL。公开接口不能暴露仅与草稿内容关联的媒体。
- 对外路径使用稳定的 `slug`，内部管理和关联使用 `id`。新闻目前仅有按数字 `articleId` 读取的历史接口；发布闭环开发时新增 slug，并将官网路由切换为 slug。改名、下线后的重定向策略由 `redirect_rule` 维护，不以标题拼接 URL。
- 日期使用 ISO 8601（例如 `2026-07-16T09:00:00+08:00`）；显示格式由 Nuxt 决定。

## 2. 首页模块与数据归属

| 原型模块 | 前台交互 | 数据归属 | V1 建议 |
| --- | --- | --- | --- |
| 顶部导航、页脚、联系信息、二维码 | 站内跳转、展开菜单、联系/提交 | 后台可编辑的站点配置 | `site_settings` 单页配置；导航可先在前端配置，待栏目稳定后后台化 |
| 首屏 Banner、两个 CTA、四个能力短句 | 轮播/视频背景、跳转 | 独立业务实体 | `hero_slide`；支持排序、启停、桌面/移动图、主副标题和两个 CTA |
| 关于我们/科研平台 | 四个入口跳转至对应单页 | 后台可编辑单页 | `page` 或 `company_profile`；首页只读摘要，不重复维护正文 |
| 应用场景 | 分类 Tab、两张推荐卡、查看更多 | 独立业务实体 | `application_domain`；每个分类下可配置首页推荐卡 |
| 产品中心 | 分类 Tab、主推荐卡、四张产品卡、查看更多 | 独立业务实体 | `product_category`、`product`；不要借用文章表承载产品参数与图册 |
| 典型项目/案例 | 行业 Tab、主案例、案例卡、详情跳转 | 独立业务实体（可关联历史文章） | `case_study`；历史 `article(category=case)` 可作为迁移来源或关联内容 |
| 合作伙伴 Logo 墙 | 展示合作伙伴/资质标识 | 独立业务实体 | `partner`；公开前须逐项确认授权、名称与排序 |
| 技术研发、技术表、合作模式 | 跳转详情、咨询合作 | 单页 + 结构化列表 | `technology_capability`、`cooperation_mode`；技术参数需企业审核 |
| 新闻资讯 | 视频/置顶卡/紧凑列表、详情跳转 | 现有文章聚合 | 扩展现有 `article` 的首页展示位/推荐排序，复用已实现文章详情接口 |
| 页脚询盘表单 | 提交、成功/失败提示 | 独立表单实体 | `inquiry`；不公开查询接口；限流、验证码与隐私同意为上线前置条件 |

### 2.1 已冻结的信息架构与关系

一级导航及前端路由固定为：首页 `/`、产品 `/products`、应用与案例 `/applications`、技术 `/technology`、新闻 `/news`、合作 `/cooperation`、我们 `/about`，以及全局高亮的“获取方案”按钮（打开询盘弹窗或滚动至询盘区）。

一期按 10 类页面模板建设：首页、产品列表、产品详情、应用与案例、案例详情、技术、新闻列表、新闻详情、合作、我们。招聘信息先归入 `/about#jobs`，仅当内容量增长时再拆分为独立 `/jobs`。

```text
产品分类 1 ── N 产品
应用场景 N ── N 产品
应用场景 1 ── N 案例
案例 N ── N 产品
案例 N ── N 合作伙伴
新闻分类 1 ── N 新闻
```

产品、应用、案例必须为独立实体；历史文章中的案例、研发成果、应用技术可作为迁移来源或关联内容，不能替代结构化的产品/案例数据。

## 3. 公开读取接口

本节为前端可据以建页面的目标契约。除“新闻”外，其余接口尚未实现。

### 3.1 首页聚合、站点与导航

`GET /api/v1/public/home`

首页采用一个只读聚合接口，避免首屏请求十多个接口；领域接口仍是列表页、详情页与后台联调的权威来源。只返回已发布、在有效时间内且被配置为首页展示的数据；任一内容发布、下线或调整首页展示位后须失效该接口缓存。

```json
{
  "banners": [],
  "trustPoints": [],
  "companyOverview": {},
  "applicationScenes": [],
  "productCategories": [],
  "featuredProducts": [],
  "featuredCases": [],
  "partners": [],
  "technologyOverview": {},
  "featuredArticles": [],
  "siteContact": {}
}
```

`GET /api/v1/public/site`

返回已审核的 `companyName`、`address`、`servicePhone`、`serviceEmail`、`wechatQrImageUrl`、`icpNumber`、`copyrightText`、`privacyPolicyUrl`。任一联系方式未确认时返回 `null`，前端隐藏对应项；不得以原型占位信息替代。

`GET /api/v1/public/navigation`

返回顶部和页脚导航树；每项包含 `label`、`target`、`children`、`sortOrder`、`highlighted`。产品可配置二级大菜单；“获取方案”配置为 `highlighted=true`。

### 3.2 首屏轮播

`GET /api/v1/public/hero-slides`

返回按 `sortOrder` 排列且已启用的轮播项：

```json
[
  {
    "id": 1,
    "eyebrow": "中国大学成果转化平台 · 新一代气凝胶技术",
    "title": "以新材料之力",
    "highlightText": "服务“双碳”战略",
    "summary": "…",
    "desktopImageUrl": "/api/v1/public/media/101",
    "mobileImageUrl": "/api/v1/public/media/102",
    "videoUrl": null,
    "primaryAction": { "label": "了解核心技术", "targetType": "INTERNAL", "target": "/technology" },
    "secondaryAction": { "label": "获取产品资料", "targetType": "INTERNAL", "target": "/products" }
  }
]
```

前端：无数据时隐藏轮播区并使用站点默认首屏；图片加载期间使用等比例骨架屏。桌面和移动图均应提供，首屏主图建议按约 `2.2:1` 准备，最终裁切以视觉验收为准。

### 3.3 公司简介摘要

`GET /api/v1/public/company-profile/summary`

返回 `eyebrow`、`title`、`highlightText`、`summary`、`imageUrl`、`actions[]`。`actions` 含 `label`、`target`，用于“企业介绍/研发团队/气凝胶技术/复合材料技术”等入口；完整正文由各自单页 URL 提供。

### 3.4 应用领域

`GET /api/v1/public/application-scenes`

返回排序后的分类及首页推荐项：

```json
[
  {
    "id": 1,
    "slug": "building-energy-efficiency",
    "name": "建筑节能",
    "featuredItems": [
      { "title": "…", "summary": "…", "imageUrl": "…", "target": "/applications/example", "sortOrder": 1 }
    ]
  }
]
```

首页每个 Tab 最多展示 `2` 项；完整应用页同时提供行业痛点 `painPoint`、奥飞方案 `solution` 与 `recommendedProducts`。无推荐项时显示空状态“内容建设中”，不请求或展示其他分类的数据。卡片主图建议约 `16:9`。

### 3.5 产品

- `GET /api/v1/public/product-categories`
- `GET /api/v1/public/products?category={slug}&keyword=&featured=&page=1&size=12`
- `GET /api/v1/public/products/{slug}`

产品列表项返回 `slug`、`name`、`shortDescription`、`features`、`coverImageUrl`、`isFeatured`、`sortOrder`；详情额外返回 `contentHtml`、`gallery`、`specifications`、`applicationScenes`、`relatedCases`、`relatedProducts`、`documents`、`seo`。首页每个分类返回 `1` 个 `isFeatured=true` 的主卡与最多 `4` 个普通卡；图片建议按 `1:1` 或 `4:3` 准备。

产品技术参数、检测报告和下载资料只有在企业确认可公开后才能返回；受限资料不通过公开接口下发。

当前已实现产品公开读取：`GET /api/v1/public/product-categories`、`GET /api/v1/public/products`、`GET /api/v1/public/products/{slug}`。产品仅在状态为 `PUBLISHED` 时返回；列表字段已对齐 `alfy-web` 的产品卡片（分类 slug/名称、摘要、封面和特点）。

### 3.6 应用案例与合作伙伴

- `GET /api/v1/public/application-scenes`
- `GET /api/v1/public/cases?scene={slug}&product={slug}&featured=&page=1&size=12`
- `GET /api/v1/public/cases/{slug}`
- `GET /api/v1/public/partners`

应用场景接口返回 `key`（即场景 slug）、名称、slogan、摘要和封面 URL，字段可直接映射 `alfy-web` 的 `ApplicationScene`。案例列表项返回 `slug`、`title`、`scene`、`sceneSlug`、`location`（可为 `null`）、`coverImageUrl`、`summary`、`featured`、`sortOrder`；详情额外返回项目背景、客户需求/行业痛点、方案、实施过程、结果、关联产品与场景。首页每个场景最多返回 `1` 个主案例和 `3` 个普通案例；主图建议约 `3:2`。是否公开客户名称、地点、成效和产品用量均为企业确认项。

V7 已建立应用场景、案例与产品关联表，并实现上述场景和案例的公开读取接口。场景、案例、关联产品均须为 `PUBLISHED` 才会通过公开接口返回；后台场景/案例 CRUD 将在后台内容管理批次补齐。

合作伙伴项返回 `name`、`logoUrl`、`websiteUrl`（可为 `null`）、`sortOrder`。未取得 Logo 使用授权时，仅展示纯文本或不展示。

### 3.7 技术、合作与我们

- `GET /api/v1/public/pages/technology`
- `GET /api/v1/public/pages/cooperation`
- `GET /api/v1/public/pages/about`
- `GET /api/v1/public/jobs`

单页以固定结构的 `blocks` 返回，而非让前端解析任意 HTML。技术区块须标识 `stage`（`MASS_PRODUCTION`、`PILOT_VALIDATION`、`LAB_RESERVE`），明确区分已量产、中试/验证中和实验室储备；合作页含合作对象、能力、流程、条件和 CTA；我们页含企业、历程、团队、资质、招聘与联系锚点。首页技术概览和三张合作卡由 `/home` 返回，不在前端写死文案。

### 3.8 新闻

当前已实现并保留：

- `GET /api/v1/public/articles?category=news&page=1&size=10`

当前还存在仅按数字读取的 `GET /api/v1/public/articles/{articleId}`，它是开发期兼容能力，不作为 Nuxt 的最终契约。发布闭环开发时将详情读取升级为 `GET /api/v1/public/articles/{slug}`，并为列表增加 `year`、`featured` 筛选。新闻详情补充封面、来源、附件、上一篇/下一篇、三篇相关推荐和 SEO；首页的“视频主卡 + 置顶卡 + 紧凑列表”统一由 `/home` 的 `featuredArticles` 返回。后端只从 `PUBLISHED` 文章中选取配置了首页展示位的内容；没有任何已发布内容时返回 `null` 或空数组，前端隐藏该卡位而非展示草稿。

### 3.9 询盘提交

`POST /api/v1/public/inquiries`

请求体：

```json
{
  "name": "张三",
  "company": "某公司",
  "phone": "13800000000",
  "email": "name@example.com",
  "inquiryType": "PROJECT_SOLUTION",
  "message": "希望获取产品资料",
  "productId": null,
  "sourceUrl": "https://www.alfy.com.cn/products",
  "utm": { "source": "baidu", "medium": "cpc", "campaign": "aerogel" },
  "privacyAccepted": true,
  "captchaToken": "…"
}
```

响应仅返回 `{ "inquiryNo": "AF202607160001" }`，不返回或显示其他客户提交内容。服务端须校验手机号/邮箱、内容长度和 `privacyAccepted`，并实现 IP/设备限流、验证码校验、审计与敏感信息访问控制。未确定隐私政策、接收人和反垃圾方案前，此接口不得上线。

## 4. 管理端契约（须先鉴权后实现）

后台 API 统一位于 `/api/v1/admin`。先实现认证、RBAC 和操作审计，再开放任何写接口。

一期认证方案已确定为 Spring Security + HS256 JWT：访问令牌有效期默认 30 分钟，刷新令牌默认 14 天且仅保存 SHA-256 摘要。生产环境必须由 `ALFY_SECURITY_JWT_SECRET` 注入随机密钥；首次本地初始化管理员可使用不提交到仓库的 `ALFY_INITIAL_ADMIN_USERNAME` 与 `ALFY_INITIAL_ADMIN_PASSWORD` 环境变量。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/admin/auth/login` | 密码与验证码登录 |
| POST | `/admin/auth/refresh` | 刷新访问令牌 |
| POST | `/admin/auth/logout` | 注销并失效 refresh token |
| GET | `/admin/auth/me` | 当前用户、角色、权限和菜单 |
| CRUD | `/admin/banners` | Banner、CTA、有效期与排序 |
| CRUD | `/admin/product-categories`、`/admin/products` | 产品分类和产品 |

已实现：`/admin/product-categories` 支持产品分类的列表、详情、创建、带 `version` 的编辑和删除；`/admin/products` 支持列表、详情、创建、带 `version` 的编辑、发布、下线和软删除。产品请求可用 `sceneIds` 维护应用场景关联，发布前分类必须启用。
| CRUD | `/admin/application-scenes`、`/admin/cases` | 应用场景和案例 |
| CRUD | `/admin/technologies`、`/admin/partners`、`/admin/pages`、`/admin/jobs` | 技术、伙伴、单页、招聘 |
| CRUD | `/admin/article-categories`、`/admin/articles`、`/admin/media` | 新闻、分类、受控媒体 |

已实现：`/admin/application-scenes` 与 `/admin/cases` 均支持列表、详情、创建、带 `version` 的编辑、发布、下线和软删除；案例请求可通过 `productIds` 维护产品关联。案例发布前要求所属场景已发布。
| GET/PUT | `/admin/site-settings` | 联系方式、Logo、备案、统计配置 |
| CRUD | `/admin/redirects` | 旧站 301 规则 |
| GET | `/admin/not-found-logs`、`/admin/operation-logs` | 404 与操作审计 |
| GET/PUT | `/admin/inquiries`、`/admin/inquiries/{id}/assignee`、`/admin/inquiries/{id}/status` | 询盘列表、分配和状态 |
| POST | `/admin/inquiries/{id}/follow-ups` | 新增跟进记录 |

各发布型资源统一提供 `POST /{resource}/{id}/publish`、`POST /{resource}/{id}/offline`、`POST /{resource}/{id}/restore` 与 `PUT /{resource}/sort`；文章保留现有 `DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`OFFLINE` 状态。每个发布型实体至少有 `status`、`publishedAt`、`sortOrder`、`version`、创建/更新审计字段，且未发布数据不得由任何公开接口读取。

后台预览不复用公开详情接口：草稿预览应生成仅绑定指定资源、短时有效的签名 URL。管理员删除媒体前，服务端必须检查是否仍被内容引用。

### 4.1 当前已实现：文章发布闭环

已实现的后台端点如下，均要求 `Authorization: Bearer {accessToken}`，登录和刷新除外：

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| POST | `/api/v1/admin/auth/login` | 返回访问令牌、刷新令牌和当前管理员资料 |
| POST | `/api/v1/admin/auth/refresh` | 刷新令牌轮换，旧令牌立即失效 |
| POST | `/api/v1/admin/auth/logout` | 使当前刷新令牌失效 |
| GET | `/api/v1/admin/auth/me` | 获取当前管理员身份 |
| GET/POST | `/api/v1/admin/articles` | 后台分页查询、创建文章草稿 |
| GET/PUT/DELETE | `/api/v1/admin/articles/{id}` | 草稿/已发布文章读取、编辑、软删除 |
| POST | `/api/v1/admin/articles/{id}/publish` | 发布文章；校验标题、slug、正文和分类 |
| POST | `/api/v1/admin/articles/{id}/offline` | 下线文章，公开读取与媒体访问随即隐藏 |

文章保存时服务端白名单清洗富文本，自动维护纯文本；更新必须携带 `version`，冲突返回 HTTP 409。V4 已为文章增加首页展示位、推荐排序和乐观锁，并为历史草稿补齐稳定 slug；V5 新增刷新令牌及后台操作日志表。

## 5. 前后端联调规则

| 场景 | 前端行为 | 后端保证 |
| --- | --- | --- |
| 首次加载 | 各模块可并行请求；不依赖巨型首页接口 | 可缓存的公开 GET 返回稳定排序 |
| 加载中 | 保留模块尺寸的骨架屏，避免页面跳动 | 返回字段完整且 `null` 语义稳定 |
| 无数据 | 隐藏该卡/模块或显示“内容建设中”，不显示伪造素材 | 返回空数组或 `null`，不以 404 代表空集合 |
| 单项不存在或下线 | 跳转 404 页面 | 详情返回 404，媒体访问遵守已发布过滤 |
| 移动端 | 使用 `mobileImageUrl`，Tab 可横向滚动 | 不依赖仅桌面裁切的图片；必要时提供移动图 |
| 缓存 | 前端可短缓存公开内容 | 发布/下线后主动失效 CDN 与应用缓存 |

## 6. 开发前仍需由企业提供或确认的内容

以下项目不阻塞领域模型和接口编码，但阻塞对应内容的正式发布：

1. 正式 Logo/VI、Banner 主题、桌面/移动端图片或视频，以及技术来源的授权表述；
2. 应用场景、产品分类/名称/参数/下载权限，以及技术能力的公开口径、验证阶段和数据来源；
3. 可公开案例、项目数据、客户名称、合作伙伴 Logo、新闻视频的授权；
4. 经销商、复合产品开发和产业链合作的正式条件；
5. 团队和招聘公开范围、联系方式、地图、二维码、备案号、隐私政策与询盘接收人；
6. 新旧域名、旧 URL 到新 slug 的 301 映射、统计账号、企业微信通知方式；
7. 一期角色按 `super_admin`、`content_admin` 实现；如需询盘专员，再增加 `inquiry_operator`。

## 7. 建议的开发顺序

1. 以前端已确认的路由、移动端规则、素材规格建立 Nuxt 页面骨架与 mock。
2. 实现登录、角色权限、媒体上传与文章 CRUD，完成“创建/编辑/发布/下线文章 -> 官网新闻显示/隐藏”闭环。
3. 以新的 Flyway 迁移建立站点配置、Banner、产品、应用场景、案例、伙伴、技术、单页、询盘及关联表；不修改已执行的 V1-V3。
4. 实现各领域公开接口、后台维护接口与 `/api/v1/public/home` 聚合接口，按发布/下线动作失效缓存后联调。
