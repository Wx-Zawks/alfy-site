# 奥飞官网生产部署

此目录中的配置会启动五个容器：MySQL 数据库、Java API、Nuxt 官网、管理后台和 Nginx。

## 域名

- `alfy.com.cn` 和 `www.alfy.com.cn`：官网
- `admin.alfy.com.cn`：管理后台
- `api.alfy.com.cn`：可选的 API 访问入口和排障入口

Nginx 对外监听 80 和 443，官网和后台的 HTTP 请求使用 308 跳转到同域 HTTPS，保留请求方法、路径和查询参数。所有用户可见的 API 请求在官网和后台各自域名下通过 `/api/v1` 转发。

证书安装、续签、验证及回退说明见 [HTTPS 部署与证书更新](../docs/03-HTTPS部署与证书更新.md)。

## 首次启动

在项目根目录执行以下命令（生产服务器目录为 `/opt/alfy-site`）：

```bash
cd /opt/alfy-site/deploy
cp .env.production.example .env.production
chmod 600 .env.production
```

编辑 `.env.production` 并替换所有 `replace-with-...` 值。不得将这个文件提交到 Git 或发送到聊天工具。

首次启动前，将已签发的 Nginx 证书链和匹配私钥放在服务器的以下位置（文件不在 Git 中）：

```text
/opt/alfy-site/deploy/nginx/certs/alfy.com.cn.pem
/opt/alfy-site/deploy/nginx/certs/alfy.com.cn.key
```

证书目录权限设为 700，两个文件权限设为 600；Compose 将该目录只读挂载至 Nginx。缺少证书文件时，Nginx 无法启动。阿里云 ECS 安全组与启用中的服务器防火墙均需允许 TCP 443。

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

仅修改入口配置时，在 `deploy/` 下验证配置后，只更新 Nginx：

```bash
docker compose --env-file .env.production config --quiet
docker compose --env-file .env.production run --rm --no-deps -T nginx nginx -t
docker compose --env-file .env.production up -d --no-deps --no-build nginx
```

若只更新了已经挂载的证书文件或 Nginx 配置内容，检查通过后执行 `docker exec alfy-nginx nginx -s reload`。Compose 的端口或挂载变化需要重建 Nginx 容器，仅 reload 不会应用这些变化。

正式域名的本机健康检查使用域名验证证书：

```bash
curl --resolve www.alfy.com.cn:443:127.0.0.1 https://www.alfy.com.cn/api/v1/health
```
