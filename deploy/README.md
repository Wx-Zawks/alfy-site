# 奥飞官网生产部署

此目录中的配置会启动五个容器：MySQL 数据库、Java API、Nuxt 官网、管理后台和 Nginx。

## 域名

- `alfy.com.cn` 和 `www.alfy.com.cn`：官网
- `admin.alfy.com.cn`：管理后台
- `api.alfy.com.cn`：可选的 API 访问入口和排障入口

在 HTTPS 配置完成前，Nginx 只监听 80 端口。所有用户可见的 API 请求在官网和后台各自域名下通过 `/api/v1` 转发，不会暴露 Docker 服务端口。

## 首次启动

在项目根目录执行以下命令（生产服务器目录为 `/opt/alfy-site`）：

```bash
cd /opt/alfy-site/deploy
cp .env.production.example .env.production
chmod 600 .env.production
```

编辑 `.env.production` 并替换所有 `replace-with-...` 值。不得将这个文件提交到 Git 或发送到聊天工具。

随后构建并启动：

```bash
sudo docker compose --env-file .env.production up -d --build
```

查看运行状态和日志：

```bash
sudo docker compose --env-file .env.production ps
sudo docker compose --env-file .env.production logs --tail=100 api
```

## 持久化数据

`mysql_data` 保存数据库，`uploads_data` 保存后台上传的媒体文件。更新容器时绝不能使用 `docker compose down -v`，否则会删除这两项数据。

## 更新

在确认备份成功后，更新源代码并重新构建：

```bash
cd /opt/alfy-site
sudo docker compose --env-file deploy/.env.production -f deploy/docker-compose.yml up -d --build
```
