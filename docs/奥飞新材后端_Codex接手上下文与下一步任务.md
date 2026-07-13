# 奥飞新材官网重构项目：Codex 接手上下文与下一步任务

> 用途：将本文件直接提供给 Codex，作为当前后端开发工作的完整上下文。  
> 当前重点：不要一次性实现所有业务；先检查现有仓库，再完成后端公共基础能力，并保持代码可运行、可测试、可回滚。

---

## 1. 项目基本信息

### 1.1 企业名称

湖南奥飞新材料有限公司。

### 1.2 企业背景

根据企业当前提供的介绍材料：

- 成立时间：2022 年 7 月 25 日；
- 所在地：湖南省长沙市；
- 当前公开地址：湖南湘江新区麓谷街道麓松路 459 号东方红小区长丰 1 栋 106；
- 法定代表人：张丁日；
- 注册资本：1133 万元人民币；
- 企业类型：高新技术企业、科技型中小企业；
- 核心业务：气凝胶及其衍生品的研发、量产和应用；
- 依托中南大学及相关科研平台，与中南奥飞气凝胶研究院进行产学研合作；
- 主要技术方向包括：
  - 常压干燥低成本气凝胶制备；
  - 透明气凝胶；
  - 水性气凝胶涂料；
  - 金属气凝胶；
  - 气凝胶复合材料；
- 主要产品和业务方向包括：
  - 气凝胶粉体、分散体；
  - 气凝胶板；
  - 气凝胶涂料；
  - 气凝胶涂料配套产品；
  - 无机保温涂料；
- 主要应用领域包括：
  - AI 数据中心与算力基础设施冷却；
  - 新能源汽车和电池热管理；
  - 建筑节能；
  - 工业保温；
  - 电力设备；
  - 航空航天、国防、家电等；
- 2026 年 1 月，公司完成新一代气凝胶及其复合材料技术中试，并参与和中南大学、立邦中国、三一筑工等单位的合作。

### 1.3 内容真实性要求

以上信息来自企业提供的项目资料，不代表开发者已经逐条核验。

开发和内容录入时必须遵守：

```text
不得让 AI 编造产品参数、专利、客户案例、合作关系、认证信息、报价和性能数据。
所有对外公开内容最终以企业书面确认版本为准。
```

---

## 2. 项目背景与目标

旧官网使用 WordPress，旧域名为：

```text
alfy.cn
www.alfy.cn
```

旧服务器存在问题，计划使用新域名重构：

```text
www.alfy.com.cn
```

项目一期目标：

```text
建设一个稳定、可维护、SEO 友好、支持后台发布内容的企业官网。
```

后续可能扩展：

- AI 客服；
- Dify / Coze 知识库；
- 企业微信通知；
- 内部知识库；
- 员工或客户登录；
- 更复杂的销售和询盘管理。

这些后续能力不是当前后端初始化阶段的任务。

---

## 3. 总体技术路线

项目计划采用：

```text
官网前台：Nuxt 3 + Vue 3 + TypeScript
后台管理：Vben Admin（Element Plus 版本倾向）
后端服务：Spring Boot 3
数据库：MySQL 8
ORM / 数据访问：MyBatis-Plus
数据库迁移：Flyway
部署：阿里云 ECS + Nginx
```

域名规划：

```text
www.alfy.com.cn      官网
admin.alfy.com.cn    管理后台
api.alfy.com.cn      后端 API

alfy.cn
www.alfy.cn          301 跳转到新域名
```

项目目录规划：

```text
alfy-site/
├── alfy-web/        Nuxt 官网
├── alfy-admin/      Vben 管理后台
├── alfy-api/        Spring Boot 后端
├── docs/
└── deploy/
```

---

## 4. 两人分工

已经确定两个人合作开发。

### 当前用户负责

```text
后端开发
数据库
接口
登录权限
文件上传
部署相关工作
```

### 同门负责

```text
Nuxt 官网前端
Vben 后台前端页面
页面样式
移动端适配
前端接口接入
```

### 共同负责

- 需求确认；
- 页面字段确认；
- API 契约；
- 联调；
- 测试；
- 上线；
- 企业验收和交付。

协作原则：

```text
每人本地一套 MySQL
代码通过 Git 同步
数据库结构通过 Flyway 同步
main 分支保持可运行
不要通过压缩包互相覆盖代码
不要直接在生产服务器修改源码
```

---

## 5. 企业提供的网站结构

企业已经提供网站信息架构图。

当前顶级栏目主要包括：

```text
首页
产品
应用
技术
新闻
合作
我们
```

### 5.1 首页

首页属于综合介绍页面，计划包含：

- Banner / 首页轮播图；
- 关于奥飞；
- 中南大学关联或科研背景介绍；
- 应用场景；
- 产品；
- 典型案例；
- 合作伙伴 Logo；
- 技术介绍；
- 合作模式；
- 新闻；
- 联系入口。

备注：

- 首页内容应尽量从产品、新闻、案例、合作伙伴等业务表聚合，不要重复存储多份相同内容；
- 可考虑后端提供 `/api/v1/home` 聚合接口，但尚未最终确定。

### 5.2 产品

产品为列表页，当前结构图中的分类大致包括：

- 气凝胶粉体 / 分散体；
- 气凝胶板，约 5 款；
- 气凝胶涂料相关配套产品，约 12 款；
- 气凝胶涂料，约 6 款。

这些数量只是企业当前估计，不应写死在代码中。

推荐建模方向：

```text
product_category
product
```

而不是每个分类一张产品表。

### 5.3 应用

当前包含：

- 应用场景：列表页；
- 案例：列表页；
- 解决方案：专题页，可后做。

### 5.4 技术

属于综合介绍页面，子页面可能包括：

- 气凝胶材料技术：可编辑单页；
- 气凝胶复合产品技术：可编辑单页；
- 其他技术：可编辑单页。

### 5.5 新闻

新闻为列表页，分类包括：

- 公司新闻；
- 行业新闻。

建议统一使用一张 `article` 表，通过分类字段区分。

### 5.6 合作

属于综合介绍页面，可能包括：

- 经销商：可编辑单页；
- 复合产品开发模式：可编辑单页；
- 产业链合作模式：可编辑单页。

### 5.7 我们

属于综合介绍页面，可能包括：

- 企业介绍：综合介绍页面；
- 研发团队：综合介绍页面；
- 招聘信息：可编辑单页，是否独立为多个岗位尚未确定；
- 联系我们：可编辑单页。

---

## 6. 一期范围

### 6.1 当前建议的一期功能

- 官网主要展示页；
- 首页 Banner；
- 产品分类、产品列表和详情；
- 新闻分类、新闻列表和详情；
- 应用场景；
- 典型案例；
- 可编辑单页；
- 合作伙伴 Logo；
- 后台管理员登录；
- 内容管理员角色；
- 图片和附件上传；
- 联系表单与询盘；
- SEO 字段；
- sitemap；
- robots；
- 404；
- HTTPS；
- 旧域名 301；
- 数据库与上传文件备份；
- 阿里云部署。

### 6.2 暂不做或后做

- 游客注册；
- 员工系统；
- 在线订单；
- 在线支付；
- 多语言；
- 企业 OA；
- 复杂审批；
- 复杂 RBAC；
- 自研知识库；
- 自研 AI Agent；
- Kubernetes；
- 全量 Docker 化；
- 自研 SEO 排名监控。

---

## 7. 候选数据模型

以下只是当前讨论出的建模方向，不是最终数据库设计。

### 7.1 管理员

```text
admin_user
```

当前已创建该表的第一版 Flyway 迁移。

一期可能只使用：

```text
role = super_admin
role = content_admin
```

是否立即实现完整 RBAC 尚未确定，当前倾向一期不做复杂 RBAC。

### 7.2 其他候选表

```text
banner
product_category
product
application_scene
case_project
article
content_page
partner
inquiry
media_asset
site_setting
redirect_rule
not_found_log
operation_log
```

### 7.3 可编辑单页

技术介绍、合作模式、联系我们等页面可能统一使用：

```text
content_page
```

可通过 `page_key` 区分，例如：

```text
company_intro
research_team
recruitment
contact
technology_aerogel
technology_composite
technology_other
cooperation_dealer
cooperation_product_development
cooperation_industry_chain
```

是否所有上述页面都使用同一张表，仍需和前端以及企业确认。

---

## 8. API 当前状态

### 8.1 重要说明

```text
API 尚未最终确定。
```

不要将下面的候选接口当作不可修改的最终合同。

### 8.2 当前倾向

公开接口前缀：

```text
/api/v1
```

后台接口前缀：

```text
/api/v1/admin
```

统一响应候选格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

统一分页候选格式：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

### 8.3 候选公开接口

```text
GET  /api/v1/health
GET  /api/v1/home

GET  /api/v1/product-categories
GET  /api/v1/products
GET  /api/v1/products/{slug}

GET  /api/v1/application-scenes
GET  /api/v1/application-scenes/{slug}

GET  /api/v1/cases
GET  /api/v1/cases/{slug}

GET  /api/v1/articles
GET  /api/v1/articles/{slug}

GET  /api/v1/pages/{pageKey}
GET  /api/v1/partners

POST /api/v1/inquiries
```

### 8.4 候选后台接口

```text
POST /api/v1/admin/auth/login
GET  /api/v1/admin/auth/me
POST /api/v1/admin/auth/logout
```

内容管理接口可能采用统一 REST 风格：

```text
/api/v1/admin/products
/api/v1/admin/product-categories
/api/v1/admin/articles
/api/v1/admin/banners
/api/v1/admin/application-scenes
/api/v1/admin/cases
/api/v1/admin/pages
/api/v1/admin/partners
/api/v1/admin/inquiries
/api/v1/admin/files
```

---

## 9. 尚未确定的关键事项

Codex 不应擅自把这些事项当作已经定稿。

### 9.1 鉴权方案

尚未最终决定：

```text
Sa-Token
或
Spring Security + JWT
```

当前项目还没有加入鉴权依赖。

### 9.2 管理员权限

尚未最终决定：

- 只在 `admin_user.role` 中保存角色；
- 还是立即建立完整 RBAC；
- 当前倾向一期先使用简单角色字段。

### 9.3 文件存储

尚未最终决定：

- 一期先存 ECS 本地目录；
- 还是直接使用阿里云 OSS。

原方案倾向一期先使用：

```text
/data/alfy/uploads
```

并预留 OSS 迁移能力。

### 9.4 富文本格式

新闻、产品和单页内容尚未最终确定使用：

- HTML；
- Markdown；
- 编辑器 JSON；
- HTML + 安全过滤。

### 9.5 内容关系

尚未确认：

- 一个产品是否属于多个分类；
- 产品是否关联多个应用场景；
- 产品、案例、解决方案之间是否需要多对多关系；
- 首页推荐内容由 `is_featured` 控制，还是单独配置首页区块。

### 9.6 URL 与 slug

倾向使用语义化 URL：

```text
/products/{slug}
/news/{slug}
/cases/{slug}
```

但 slug 的生成规则、修改规则和历史重定向策略尚未最终确定。

### 9.7 招聘与研发团队

尚未确认：

- 招聘信息是单页，还是多条岗位记录；
- 研发团队是单页介绍，还是每个成员独立记录。

### 9.8 企业微信

企业微信询盘提醒是否纳入第一版开发，尚需和企业确认。

---

## 10. 当前后端工程状态

项目路径：

```text
D:\Projects\alfy-site\alfy-api
```

### 10.1 技术版本

已经完成并验证：

```text
JDK 17
Spring Boot 3.5.16
MyBatis-Plus 3.5.17
MySQL 8
Maven
Flyway
```

### 10.2 当前 Maven 依赖

`pom.xml` 当前包含：

- `spring-boot-starter-web`；
- `spring-boot-starter-validation`；
- `mybatis-plus-spring-boot3-starter`；
- `mybatis-plus-jsqlparser`；
- `mysql-connector-j`；
- `flyway-core`；
- `flyway-mysql`；
- `lombok`；
- `spring-boot-starter-test`。

当前还没有：

- Spring Security；
- Sa-Token；
- JWT 库；
- Redis；
- Swagger / Knife4j；
- OSS SDK。

### 10.3 当前配置

`application.yml` 已包含：

```yaml
server:
  port: 8080

spring:
  application:
    name: alfy-api

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  flyway:
    enabled: true
    locations: classpath:db/migration
    encoding: UTF-8
    validate-migration-naming: true
    clean-disabled: true

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true

  global-config:
    banner: false
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    root: info
    com.alfy.api: debug
```

本地数据库配置放在：

```text
application-local.yml
```

本地数据库名：

```text
alfy_site_dev
```

真实数据库密码没有提交到 Git。

### 10.4 Flyway

已经存在第一条迁移：

```text
V1__create_admin_user.sql
```

并已成功执行。

数据库已经出现：

```text
admin_user
flyway_schema_history
```

重要规则：

```text
V1 已经执行，不要随意修改 V1。
后续数据库结构变化应创建 V2、V3 等新迁移。
```

### 10.5 已有基础代码

根据当前初始化结果，工程已包含或已经实现：

- `AlfyApiApplication`；
- `ApiResponse` 统一响应基础类；
- `/api/v1/health` 健康检查接口；
- Spring Boot 默认上下文测试；
- 本地 Profile 配置；
- Flyway 初始化。

### 10.6 已完成验证

以下全部已通过：

```text
java -version 为 17
mvn clean test 成功
Spring Boot 能启动
/api/v1/health 能访问
DataGrip 中出现 admin_user
DataGrip 中出现 flyway_schema_history
mvn clean package 成功
Jar 可以通过 java -jar 启动
application-local.yml 未提交
代码已经推送到 Git
同门拉取后能运行
```

### 10.7 已解决的问题

此前 `mvn clean test` 曾报错：

```text
Unknown database 'alfy_site_dev'
```

原因是数据库尚未创建。

已经通过在 MySQL 中创建 `alfy_site_dev` 解决，当前测试已通过。

---

## 11. 当前建议的下一步

不要立即开发产品、新闻、案例等全部业务。

下一步应完成一个最小的“后端公共基础能力”里程碑。

### 11.1 Codex 开始工作前必须先做

请先：

1. 检查实际仓库目录结构；
2. 阅读 `pom.xml`、所有 YAML、Flyway 文件和现有 Java 文件；
3. 运行：
   ```bash
   mvn clean test
   ```
4. 输出：
   - 当前工程结构；
   - 已经存在的代码；
   - 计划新增或修改的文件；
   - 发现的风险或与本文件不一致的地方；
5. 不要假设本文件中的候选设计一定优先于实际代码。

### 11.2 本轮只实现这些内容

请在一个独立 feature 分支中完成：

```text
1. MyBatis-Plus Mapper 扫描和分页插件配置
2. 统一错误码
3. 业务异常类
4. 全局异常处理
5. 统一分页响应结构
6. AdminUser Entity
7. AdminUserMapper
8. 一个验证 admin_user 表可访问的集成测试
```

建议文件方向：

```text
common/ErrorCode.java
common/PageResponse.java
config/MybatisPlusConfig.java
entity/AdminUser.java
exception/BusinessException.java
exception/GlobalExceptionHandler.java
mapper/AdminUserMapper.java
src/test/.../AdminUserMapperTests.java
```

文件名和包结构可根据实际仓库适当调整，但要说明原因。

### 11.3 本轮不要做

- 不要实现管理员登录；
- 不要选择或加入鉴权框架；
- 不要实现 JWT；
- 不要一次建立全部业务表；
- 不要实现文件上传；
- 不要实现新闻 CRUD；
- 不要引入 Redis；
- 不要修改已执行的 V1 迁移；
- 不要提交密码、密钥或本地配置；
- 不要重构与本轮无关的代码；
- 不要把所有错误都返回 HTTP 200。

### 11.4 预期行为

统一错误响应应类似：

```json
{
  "code": 40400,
  "message": "数据不存在",
  "data": null
}
```

同时应使用合理 HTTP 状态码，例如：

```text
参数错误       HTTP 400
未登录         HTTP 401
无权限         HTTP 403
数据不存在     HTTP 404
服务器错误     HTTP 500
```

分页响应应类似：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

### 11.5 本轮验收标准

```text
[ ] mvn clean test 成功
[ ] mvn clean package 成功
[ ] /api/v1/health 仍可访问
[ ] AdminUserMapper 能执行 selectCount
[ ] MyBatis Mapper 未扫描警告消失
[ ] 分页插件配置完成
[ ] 全局异常能返回统一 JSON
[ ] HTTP 状态码合理
[ ] 未修改 V1 迁移
[ ] 未提交任何密码和密钥
[ ] 修改范围仅限后端公共基础能力
```

### 11.6 完成后请输出

1. 修改文件清单；
2. 每个文件的作用；
3. 关键设计说明；
4. 执行过的测试命令和结果；
5. 尚未解决的问题；
6. 建议的下一步，但不要自动继续开发下一阶段。

---

## 12. 后续计划，仅供理解，不属于本轮任务

公共基础能力通过后，计划顺序为：

```text
管理员登录鉴权
→ 文件上传
→ 新闻完整闭环
→ 产品
→ Banner / 首页聚合
→ 可编辑单页
→ 应用场景
→ 典型案例
→ 合作伙伴
→ 询盘
→ SEO / 301 / 404
→ 测试环境和部署
```

第一个业务里程碑：

```text
后台登录后发布一篇新闻
→ 官网新闻列表出现
→ 点击可访问详情
→ 下架后公开接口不再返回
```

在实现登录前，需要开发者最终确认：

```text
使用 Sa-Token
还是
Spring Security + JWT
```

在实现上传前，需要确认：

```text
本地文件目录
还是
阿里云 OSS
```

---

## 13. 对 Codex 的工作方式要求

请遵守：

```text
先读取代码，再提出计划，再修改。
一次只完成当前里程碑。
不要用大规模生成替代对现有代码的理解。
保持代码简洁，避免过度设计。
为重要行为增加测试。
所有数据库变化使用新的 Flyway 迁移。
不修改无关文件。
不输出或记录真实密码。
发现需求不明确时，列出问题，不擅自定稿。
```

当前用户是新手。完成后请用中文说明：

- 你改了什么；
- 为什么这样改；
- 如何运行；
- 如何验证；
- 出错时优先检查什么。

---

## 14. 当前一句话状态

```text
后端 Spring Boot 基线、MySQL、Flyway、健康检查、打包和 Git 协作已经跑通；
API、鉴权、文件存储和详细业务模型尚未最终确定；
当前应先完成后端公共基础能力，而不是直接批量开发业务模块。
```
