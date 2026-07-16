# 官网 API 契约（V1 草案）

> 状态：待前后端共同确认。依据 `奥飞新材｜新一代气凝胶及其复合产品技术  20260715(1).pdf` 的首页长页原型整理；本文件不是已实现接口的声明。
>
> 原则：先冻结前台读取契约和内容归属，再设计 Flyway 迁移、鉴权和后台 CRUD。原型中的示例名称、电话、邮箱、日期、合作方、技术参数和项目描述均不得直接作为生产数据，须由企业书面确认。

## 1. 通用约定

- API 前缀为 `/api/v1`，公开读取接口以 `/public` 开头；后台接口待登录与权限方案确定后再冻结。
- JSON 响应统一为 `{ "code": 0, "message": "success", "data": ... }`；HTTP 状态码表达协议错误，`code` 表达业务错误。
- 列表采用 `page`（从 `1` 开始）和 `size`；默认值、最大值应由后端写入 OpenAPI，并在前端请求封装中统一处理。
- 所有 `imageUrl`、`videoUrl` 必须是受控媒体 URL 或 CDN URL。公开接口不能暴露仅与草稿内容关联的媒体。
- 对外路径使用稳定的 `slug`，内部管理和关联使用 `id`。改名、下线后的重定向策略待确认，不以标题拼接 URL。
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

## 3. 公开读取接口

本节为前端可据以建页面的目标契约。除“新闻”外，其余接口尚未实现。

### 3.1 站点配置

`GET /api/v1/public/site-settings`

返回已审核的 `companyName`、`address`、`servicePhone`、`serviceEmail`、`wechatQrImageUrl`、`icpNumber`、`copyrightText`、`footerMenus`。任一联系方式未确认时返回 `null`，前端隐藏对应项；不得以原型占位信息替代。

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

`GET /api/v1/public/application-domains`

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

首页每个 Tab 最多展示 `2` 项；无推荐项时显示空状态“内容建设中”，不请求或展示其他分类的数据。卡片主图建议约 `1.6:1`。

### 3.5 产品

- `GET /api/v1/public/product-categories`
- `GET /api/v1/public/products?category={slug}&featured=true`
- `GET /api/v1/public/products/{slug}`

产品列表项返回 `slug`、`name`、`shortDescription`、`coverImageUrl`、`isFeatured`、`sortOrder`；详情额外返回 `contentHtml`、`gallery`、`documents`、`seo`。首页每个分类返回 `1` 个 `isFeatured=true` 的主卡与最多 `4` 个普通卡；图片建议按 `1:1` 准备。

产品技术参数、检测报告和下载资料只有在企业确认可公开后才能返回；受限资料不通过公开接口下发。

### 3.6 案例与合作伙伴

- `GET /api/v1/public/case-industries`
- `GET /api/v1/public/case-studies?industry={slug}&featured=true`
- `GET /api/v1/public/case-studies/{slug}`
- `GET /api/v1/public/partners`

案例列表项返回 `slug`、`title`、`industry`、`coverImageUrl`、`summary`、`relatedProductNames`、`sortOrder`。首页每个行业最多返回 `1` 个主案例和 `3` 个普通案例；主图建议约 `1.5:1`。是否公开客户名称、地点、成效和产品用量均为企业确认项。

合作伙伴项返回 `name`、`logoUrl`、`websiteUrl`（可为 `null`）、`sortOrder`。未取得 Logo 使用授权时，仅展示纯文本或不展示。

### 3.7 技术与合作

- `GET /api/v1/public/technology-capabilities`
- `GET /api/v1/public/cooperation-modes`

技术能力返回 `name`、`summary`、`applicationDirection`、`detailTarget`、`sortOrder`；合作模式返回 `name`、`summary`、`actionLabel`、`actionTarget`、`sortOrder`。首页使用这些字段渲染原型中的技术表和三张合作模式卡，不在前端写死文案。

### 3.8 新闻（复用已实现接口，待小幅扩展）

保留现有：

- `GET /api/v1/public/articles?category=news&page=1&size=10`
- `GET /api/v1/public/articles/{articleId}`

为匹配首页的“视频主卡 + 置顶卡 + 三条紧凑列表”，建议增加只读聚合接口：

`GET /api/v1/public/home/news`

返回 `featuredVideo`、`featuredArticle`、`latestArticles[]`。其中每个新闻项至少含现有列表字段，并新增 `contentType`（`ARTICLE`/`VIDEO`）和 `videoUrl`（仅视频项）。后端只从 `PUBLISHED` 文章中选取配置了首页展示位的内容；没有任何已发布内容时各字段为 `null` 或空数组，前端隐藏该卡位而非展示草稿。

### 3.9 询盘提交

`POST /api/v1/public/inquiries`

请求体：

```json
{
  "contactName": "张三",
  "companyName": "某公司",
  "phone": "13800000000",
  "email": "name@example.com",
  "message": "希望获取产品资料",
  "privacyAccepted": true,
  "captchaToken": "…"
}
```

响应仅返回 `{ "id": "公开追踪号" }`，不返回或显示其他客户提交内容。服务端须校验手机号/邮箱、内容长度和 `privacyAccepted`，并实现 IP/设备限流、验证码校验、审计与敏感信息访问控制。未确定隐私政策、接收人和反垃圾方案前，此接口不得上线。

## 4. 管理端边界（本轮不实现）

后台 API 统一位于 `/api/v1/admin`，且必须在登录、角色权限和操作审计落地后开放。建议按领域提供：

- `hero-slides`、`site-settings`、`pages`；
- `application-domains`、`products`、`product-categories`；
- `case-studies`、`partners`；
- `technology-capabilities`、`cooperation-modes`；
- `articles`（草稿、审核、发布、下线、首页展示位）、`media`；
- `inquiries`（只允许获授权角色查看与处理）。

每个发布型实体至少需要 `status`、`publishedAt`、`sortOrder`、创建/更新审计字段；不可让未发布数据被上述公开接口读取。

## 5. 前后端联调规则

| 场景 | 前端行为 | 后端保证 |
| --- | --- | --- |
| 首次加载 | 各模块可并行请求；不依赖巨型首页接口 | 可缓存的公开 GET 返回稳定排序 |
| 加载中 | 保留模块尺寸的骨架屏，避免页面跳动 | 返回字段完整且 `null` 语义稳定 |
| 无数据 | 隐藏该卡/模块或显示“内容建设中”，不显示伪造素材 | 返回空数组或 `null`，不以 404 代表空集合 |
| 单项不存在或下线 | 跳转 404 页面 | 详情返回 404，媒体访问遵守已发布过滤 |
| 移动端 | 使用 `mobileImageUrl`，Tab 可横向滚动 | 不依赖仅桌面裁切的图片；必要时提供移动图 |
| 缓存 | 前端可短缓存公开内容 | 发布/下线后主动失效 CDN 与应用缓存 |

## 6. 确认清单（冻结前必答）

1. 首页哪些模块一期必须后台可维护，哪些可先作为前端静态布局？
2. Banner 是否轮播、是否使用视频，以及需要几张桌面/移动素材？
3. 产品、应用、案例是否分别建模；历史文章中的“案例/研发成果/应用技术”是否仅作为关联内容？
4. 各个首页 Tab 的正式分类、排序和“查看更多”目标 URL 是什么？
5. 伙伴 Logo、客户案例、技术能力、认证/参数、联系方式是否获得正式公开授权？
6. 询盘表单的字段、隐私政策链接、接收人、验证码及通知渠道是什么？
7. 首页新闻展示位由人工置顶还是按发布时间自动聚合？视频新闻是否纳入一期？
8. 管理端采用的鉴权方案及一期角色（至少 `super_admin`、`content_admin`）是什么？
9. 产品/案例/页面的 slug 和历史 URL 301 规则是什么？

## 7. 建议的开发顺序

1. 确认本文件第 6 节，并由前端给出最终路由、移动端素材规格和空状态文案。
2. 先实现登录、角色权限、媒体上传与文章 CRUD，完成“创建/编辑/发布/下线文章 -> 官网新闻显示/隐藏”闭环。
3. 以新的 Flyway 迁移建立站点配置、Banner、产品、应用、案例、伙伴、技术与询盘模型；不修改已执行的 V1-V3。
4. 逐领域实现公开接口、后台维护接口和联调 mock，最后再决定是否需要只读的 `/api/v1/public/home` 聚合接口。
