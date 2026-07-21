# 奥飞新材官网前后端联调 API 说明

更新日期：2026-07-19。开发环境基础地址：`http://localhost:8080/api/v1`。

## 1. 通用规则

所有接口返回：

```json
{ "code": 0, "message": "success", "data": {} }
```

分页接口的 `data`：

```json
{ "records": [], "total": 0, "page": 1, "size": 12 }
```

公开接口无需认证。后台接口需请求头：

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

常见错误：`40000` 参数错误、`40100` 未登录/Token 无效、`40300` 无权限、`40400` 不存在或未发布、`40900` 编辑版本冲突。

## 2. 公开官网接口

### 产品

| 方法 | 路径 | 参数 | 返回重点 |
|---|---|---|---|
| GET | `/public/product-categories` | 无 | 分类数组：`id,name,slug,summary,coverImageUrl,sortOrder` |
| GET | `/public/products` | `category`、`featured`、`page=1`、`size=12` | 产品分页；卡片字段 `id,name,slug,category,categoryName,summary,coverImageUrl,features` |
| GET | `/public/products/{slug}` | 路径 slug | 产品详情，额外有 `contentHtml,specifications,seo` |

`category` 传产品分类 slug，例如 `raw`、`blanket`。`featured=true` 仅取推荐产品。前端当前的 `image` 可映射接口 `coverImageUrl`。

### 应用场景与案例

| 方法 | 路径 | 参数 | 返回重点 |
|---|---|---|---|
| GET | `/public/application-scenes` | 无 | 场景数组：`id,key,name,slogan,summary,coverImageUrl` |
| GET | `/public/cases` | `scene`、`product`、`featured`、`page=1`、`size=12` | 案例分页 |
| GET | `/public/cases/{slug}` | 路径 slug | 案例详情 |

场景中的 `key` 就是数据库/URL 使用的场景 slug。案例列表项包括：`id,slug,title,scene,sceneSlug,location,summary,coverImageUrl,featured,sortOrder`。案例详情额外包括 `background,customerNeed,solution,implementation,resultSummary,contentHtml,relatedProducts,seo`。

`scene` 和 `product` 都传 slug。例如：`/public/cases?scene=building&featured=true`。

### 新闻与媒体

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| GET | `/public/article-categories` | 无 | 已启用新闻分类 |
| GET | `/public/articles` | `category`、`page=1`、`size=10` | 已发布新闻分页读取 |
| GET | `/public/articles/{identifier}` | slug 或数字 ID | 已发布新闻详情 |
| GET | `/public/media/{id}` | 媒体 ID | 仅当该素材被已发布内容引用时返回文件 |

公开接口只返回 `PUBLISHED` 内容；草稿或下线数据即使知道 slug 也不会返回。

## 3. 管理员认证接口

| 方法 | 路径 | 认证 | 请求/说明 |
|---|---|---|---|
| POST | `/admin/auth/login` | 否 | `{ "username": "...", "password": "..." }` |
| POST | `/admin/auth/refresh` | 否 | `{ "refreshToken": "..." }` |
| POST | `/admin/auth/logout` | Bearer | Body：`{ "refreshToken": "..." }`，使刷新令牌失效 |
| GET | `/admin/auth/me` | Bearer | 当前管理员信息 |

登录成功 `data` 中包含 `accessToken`、`refreshToken`、有效期和用户信息。前端应保存 Token；遇到 401 时可尝试一次刷新后重发请求。

## 4. 后台产品分类与产品接口

### 产品分类 `/admin/product-categories`

- `GET /admin/product-categories?keyword=&page=1&size=10`
- `GET /admin/product-categories/{id}`
- `POST /admin/product-categories`
- `PUT /admin/product-categories/{id}`
- `DELETE /admin/product-categories/{id}`

创建请求示例：

```json
{
  "name": "气凝胶原料",
  "slug": "raw",
  "summary": "经公司确认后填写",
  "coverMediaId": null,
  "sortOrder": 0,
  "enabled": true
}
```

编辑时额外传详情接口返回的 `version`。删除前该分类不能仍有关联产品。

### 产品 `/admin/products`

- `GET /admin/products?status=DRAFT&categoryId=&keyword=&page=1&size=10`
- `GET /admin/products/{id}`
- `POST /admin/products`
- `PUT /admin/products/{id}`
- `POST /admin/products/{id}/publish`
- `POST /admin/products/{id}/offline`
- `DELETE /admin/products/{id}`

创建/编辑请求：

```json
{
  "categoryId": 1,
  "name": "示例产品",
  "slug": "example-product",
  "summary": "列表简介",
  "contentHtml": "<p>详情正文</p>",
  "contentText": "详情正文",
  "coverMediaId": null,
  "features": ["特点一", "特点二"],
  "specifications": { "字段": "需企业确认" },
  "featured": false,
  "sortOrder": 0,
  "sceneIds": [1],
  "seoTitle": null,
  "seoDescription": null,
  "seoKeywords": null
}
```

更新时必须带 `version`。发布前要求 `name`、`slug`、`summary` 完整且所属分类已启用。

## 5. 后台应用场景与案例接口

### 应用场景 `/admin/application-scenes`

- `GET /admin/application-scenes?status=&keyword=&page=1&size=10`
- `GET /admin/application-scenes/{id}`
- `POST /admin/application-scenes`
- `PUT /admin/application-scenes/{id}`
- `POST /admin/application-scenes/{id}/publish`
- `POST /admin/application-scenes/{id}/offline`
- `DELETE /admin/application-scenes/{id}`

核心请求字段：`name,slug,slogan,summary,painPoint,solution,contentHtml,coverMediaId,featured,sortOrder,seoTitle,seoDescription,seoKeywords`。编辑时传 `version`；发布前必须有名称、slug、摘要。

### 案例 `/admin/cases`

- `GET /admin/cases?status=&sceneId=&keyword=&page=1&size=10`
- `GET /admin/cases/{id}`
- `POST /admin/cases`
- `PUT /admin/cases/{id}`
- `POST /admin/cases/{id}/publish`
- `POST /admin/cases/{id}/offline`
- `DELETE /admin/cases/{id}`

创建/编辑的核心字段：

```json
{
  "sceneId": 1,
  "title": "案例标题",
  "slug": "case-slug",
  "location": null,
  "summary": "案例摘要",
  "background": "项目背景",
  "customerNeed": "需求或痛点",
  "solution": "解决方案",
  "implementation": "实施过程",
  "resultSummary": "结果（须授权）",
  "contentHtml": "<p>详情正文</p>",
  "coverMediaId": null,
  "featured": false,
  "sortOrder": 0,
  "productIds": [1]
}
```

更新时携带 `version`。发布案例前，所属场景必须已发布。

## 6. 新闻后台接口

`/admin/articles` 已支持 `GET` 列表、`GET /{id}`、`POST` 创建、`PUT /{id}` 编辑、`POST /{id}/publish`、`POST /{id}/offline`、`DELETE /{id}`。文章创建/编辑时需要 `title,slug,categoryIds`，正文为 `contentHtml`，编辑时必须带 `version`。

## 7. 联调顺序建议

1. 登录获取 Token；
2. 创建并启用产品分类；
3. 创建场景并发布；
4. 创建产品、关联场景并发布；
5. 创建案例、关联已发布场景/产品并发布；
6. 用公开接口检查前端展示；
7. 任一编辑保存后，用响应中的新 `version` 覆盖本地版本。

未实现的模块：技术页、合作页、关于页、询盘、Banner、首页聚合接口。前端应在这些接口完成前继续保留相应 mock 或空态处理。

## 8. 字段级请求契约

### 公共字段规则

| 字段 | 规则 |
|---|---|
| `slug` | 小写字母、数字、单个连字符组成，例如 `aerogel-blanket`；不能有空格、中文或连续连字符 |
| `sortOrder` | 整数，小值优先；缺省为 0 |
| `featured` | `true/false`；缺省 `false` |
| `coverMediaId` | 已存在的 `media_asset.id`；可为 `null` |
| `seoTitle` | 最长 255 字符；可为 `null` |
| `seoDescription`、`seoKeywords` | 最长 500 字符；可为 `null` |
| `version` | 仅更新请求必须携带；创建请求不传 |

所有 `contentHtml` 会由后端清洗，不应依赖脚本、内联事件或不安全标签在前端执行。

### 产品分类字段

| 字段 | 创建 | 更新 | 响应 | 说明 |
|---|---|---|---|---|
| `name` | 必填 | 必填 | 是 | 最长 100 |
| `slug` | 必填 | 必填 | 是 | 最长 100、唯一 |
| `summary` | 可选 | 可选 | 是 | 最长 1000 |
| `coverMediaId` | 可选 | 可选 | 是 | 分类封面 |
| `sortOrder` | 可选 | 可选 | 是 | 缺省 0 |
| `enabled` | 可选 | 可选 | 是 | `true`=数据库 `status=1` |
| `version` | 不传 | 必填 | 是 | 乐观锁 |

### 产品字段

| 字段 | 创建/更新要求 | 响应字段 | 说明 |
|---|---|---|---|
| `categoryId` | 必填，必须存在 | `categoryId` | 发布时分类还必须启用 |
| `name`、`slug` | 必填 | `name`、`slug` | slug 全站产品唯一 |
| `summary` | 创建可空，发布时必填 | `summary` | 最大 1000 |
| `contentHtml`、`contentText` | 可选 | 两者均返回 | 未传 `contentText` 时由 HTML 生成 |
| `features` | 可选字符串数组 | 数组 | 每项最长 100；保存进 JSON |
| `specifications` | 可选任意 JSON | JSON 或 null | 不要填未经确认指标 |
| `sceneIds` | 可选、不得重复 | 数组 | 所有 ID 必须存在；后台关联使用 |
| `status` | 不在创建/更新 Body 中传 | 响应返回 | 由 publish/offline 动作控制 |
| `publishedAt` | 不传 | 响应返回 | 首次发布由后端写入 |

### 场景字段

| 字段 | 创建/更新要求 | 说明 |
|---|---|---|
| `name`、`slug` | 必填 | 最大 100；`slug` 唯一 |
| `slogan` | 可选 | 最大 255；适合首页大图的一句话价值主张 |
| `summary` | 可选，发布时必填 | 最大 1000 |
| `painPoint`、`solution` | 可选 | 行业痛点与方案，纯文本字段 |
| `contentHtml` | 可选 | 清洗后的场景详情正文 |
| `featured`、`sortOrder`、`coverMediaId`、`seo*` | 可选 | 与公共字段规则一致 |

### 案例字段

| 字段 | 创建/更新要求 | 说明 |
|---|---|---|
| `sceneId` | 必填，必须存在 | 发布案例前场景必须已经发布 |
| `title`、`slug` | 必填 | slug 唯一，最大 255 |
| `clientName`、`location` | 可选 | 未获授权则不要填或发布 |
| `summary` | 可选，发布时必填 | 最大 1000 |
| `background`、`customerNeed` | 可选 | 背景与需求/痛点 |
| `solution`、`implementation`、`resultSummary` | 可选 | 方案、实施和结果；结果数据要有授权 |
| `productIds` | 可选数组、不得重复 | 每个产品 ID 必须存在；维护产品—案例关联 |

## 9. 主要响应结构示例

### 公开产品列表

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [{
      "id": 1,
      "name": "示例产品",
      "slug": "example-product",
      "category": "raw",
      "categoryName": "气凝胶原料",
      "summary": "列表摘要",
      "coverImageUrl": "/api/v1/public/media/8",
      "features": ["特点一"]
    }],
    "total": 1,
    "page": 1,
    "size": 12
  }
}
```

### 公开案例详情

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3,
    "slug": "example-case",
    "title": "案例标题",
    "scene": "建筑节能",
    "sceneSlug": "building",
    "location": null,
    "summary": "案例摘要",
    "background": "项目背景",
    "customerNeed": "需求",
    "solution": "方案",
    "implementation": "实施",
    "resultSummary": "结果",
    "contentHtml": "<p>正文</p>",
    "coverImageUrl": null,
    "relatedProducts": [{ "id": 1, "name": "示例产品", "slug": "example-product" }],
    "seo": { "title": null, "description": null, "keywords": null }
  }
}
```

### 后台编辑成功响应的版本处理

```json
{
  "code": 0,
  "message": "success",
  "data": { "id": 1, "status": "DRAFT", "version": 4 }
}
```

前端提交下一次 `PUT` 时必须使用 `version: 4`。如果另一个管理员已保存，接口返回 `40900`；前端应提示“内容已更新”，重新 GET 详情后让用户合并修改，不应静默覆盖。

## 10. API 调用示例（fetch）

```ts
const apiBase = 'http://localhost:8080/api/v1'

const list = await $fetch(`${apiBase}/public/products`, {
  query: { category: 'raw', page: 1, size: 12 }
})

const accessToken = '登录返回的 accessToken'
const product = await $fetch(`${apiBase}/admin/products`, {
  method: 'POST',
  headers: { Authorization: `Bearer ${accessToken}` },
  body: {
    categoryId: 1,
    name: '示例产品',
    slug: 'example-product',
    summary: '列表摘要',
    features: [],
    sceneIds: []
  }
})
```

## 11. 精确的接口可见性与状态表

| 接口类别 | 是否要求 Token | 会返回的状态 | 备注 |
|---|---|---|---|
| `/public/products`、`/public/products/{slug}` | 否 | 仅 `PUBLISHED` 产品 | 分类必须启用才能出现在分类列表；产品详情按产品状态过滤 |
| `/public/application-scenes` | 否 | 仅 `PUBLISHED` 场景 | 排序后返回数组 |
| `/public/cases*` | 否 | 仅已发布案例且场景已发布 | 场景/产品筛选参数都是 slug |
| `/public/articles*` | 否 | 仅 `PUBLISHED` 文章 | 分类筛选参数是分类 code |
| `/admin/**`（认证除外） | 是 | 所有未删除状态 | 仅 `SUPER_ADMIN`、`CONTENT_ADMIN` |

## 12. 前端适配清单

1. 使用后端分页 `records`，不要假定接口直接返回数组；只有分类/场景接口直接返回数组。
2. 将 API 的 `coverImageUrl` 映射为组件需要的 `image`；当值为 `null` 时显示默认占位图或空态。
3. 所有后台编辑页保留并回传 `version`；新建页不传。
4. 用 `slug` 作为公开详情页路由参数，不要使用数值 id；文章详情兼容数字 id，但新代码也应优先 slug。
5. 后台发布操作不是更新 Body 中的 `status`，而是单独请求 `/publish` 或 `/offline`。
