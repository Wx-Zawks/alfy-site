# 奥飞新材官网前端

基于 Nuxt 3、Vue 3 和 TypeScript 的官网前端项目。

## 本地开发

```powershell
Set-Location "E:\alfy-projects\alfy-web"
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

官网已接入 `alfy-api` 的公开接口。首页、站点信息、导航、页头、产品、应用场景、案例、技术页、新闻、合作伙伴、关于页与合作页均读取后端已发布数据；咨询表单会写入后台询盘。

复制 `.env.example` 为 `.env`，配置：

```env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api/v1
```

本地调试时先启动 `alfy-api`（默认 `8080`），再启动本项目。后台内容只有在状态为“已发布”后才会出现在官网；保存为草稿或下线的内容不会公开展示。发布后刷新官网即可看到更新。

`data/` 中的旧演示数据仅保留为设计参考，页面运行时不再引用。

## 素材

网站图片放在 `public/images/`。正式上线前需要替换或确认：

- 企业电话、地址和备案号；
- 产品正式参数和检测口径；
- 案例数据及客户名称授权；
- 研发团队公开信息；
- 合作伙伴 Logo 使用授权。
