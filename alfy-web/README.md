# 奥飞新材官网前端

基于 Nuxt 3、Vue 3 和 TypeScript 的官网前端项目。

## 本地开发

```powershell
Set-Location "E:\0005 实习\alfy-web"
npm.cmd run dev
```

浏览器访问 `http://localhost:3000`。

## 常用命令

```powershell
npm.cmd run dev        # 启动开发环境
npm.cmd run typecheck  # TypeScript/Vue 类型检查
npm.cmd run build      # 生产构建
npm.cmd run preview    # 预览生产构建
```

## 页面路由

```text
/                              首页
/products                      产品列表
/products/[slug]               产品详情
/applications                  应用与案例
/cases/[slug]                  案例详情
/technology                    技术研发
/news                          新闻列表
/news/[slug]                   新闻详情
/cooperation                   商务合作
/about                         关于我们
```

## 数据与 API

当前页面数据位于 `data/`，用于前端开发阶段展示。

Spring Boot API 完成后，通过 `composables/useApi.ts` 调用。复制 `.env.example` 为 `.env`，配置：

```env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api/v1
```

## 素材

网站图片放在 `public/images/`。正式上线前需要替换或确认：

- 企业电话、地址和备案号；
- 产品正式参数和检测口径；
- 案例数据及客户名称授权；
- 研发团队公开信息；
- 合作伙伴 Logo 使用授权。
