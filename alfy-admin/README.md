# 奥飞新材管理后台

基于 Vue 3、Vite、TypeScript、Vben Admin 和 Element Plus 的官网内容管理后台。

## 已完成模块

- 登录、角色和前端菜单权限
- 运营控制台
- 首页 Banner
- 产品、应用场景、案例、技术、新闻、合作伙伴和单页管理
- 草稿、发布、下线、置顶、排序、SEO 字段和软删除交互
- 询盘筛选、跟进、负责人、状态和 CSV 导出
- 图片/文档素材库与本地上传预览
- 页面 SEO 完整度、301 重定向和 404 记录
- 站点设置、管理员角色、操作日志和接口说明

当前业务数据使用浏览器本地存储，适合在 Spring Boot API 完成前进行前端开发和演示。

## 本地启动

```powershell
Set-Location E:\alfy-projects\alfy-admin
pnpm.cmd run dev:alfy
```

访问 `http://localhost:5777`。

演示账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 超级管理员 | `admin` | `123456` |
| 内容管理员 | `editor` | `123456` |
| 询盘人员 | `inquiry` | `123456` |

## 生产构建

```powershell
pnpm.cmd run build:alfy
```

构建产物位于 `apps/web-ele/dist`。

## 接入 Spring Boot

真实后端完成后：

1. 将 `apps/web-ele/.env.development` 中的 API 地址指向 `/api/v1/admin`。
2. 关闭本地 Nitro Mock。
3. 按 `src/api/core` 的接口定义对接登录、用户和权限码。
4. 将 `src/data/cms.ts` 的本地状态逐模块替换为后台 CRUD 接口。

统一接口响应格式：

```json
{ "code": 0, "data": {}, "message": "success" }
```
