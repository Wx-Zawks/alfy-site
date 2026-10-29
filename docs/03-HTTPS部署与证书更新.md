# HTTPS 部署与证书更新

更新日期：2026-10-29。适用于 `/opt/alfy-site` 的 ECS / Docker Compose / Nginx 生产架构。

## 当前状态

官网与管理后台已部署 vTrus OV RSA 证书，覆盖：

- `alfy.com.cn`
- `www.alfy.com.cn`
- `admin.alfy.com.cn`

三个域名均通过公网 HTTPS 证书验证并返回页面；HTTP 使用 308 永久跳转到同域 HTTPS，保留请求方法、路径和查询参数。TLS 版本允许 1.2 和 1.3。当前未启用长期 HSTS，以保留故障回退空间。

证书有效期为 2026-10-29 06:14:51 UTC 至 **2027-05-16 06:14:50 UTC**（北京时间 2027-05-16 14:14:50）。证书 SHA-256 指纹：

```text
02:C6:98:57:98:06:9D:B5:5C:15:3E:96:95:F2:F8:29:5F:71:83:BC:40:6B:17:E0:1B:5B:31:16:F4:A3:81:FC
```

阿里云订阅到期日不等于单张证书到期日。自动托管不等于当前 Docker Nginx 已自动部署新证书；后续续签后仍需确认服务器使用的是新证书。

## 文件及入口

| 位置 | 用途 |
|---|---|
| `deploy/docker-compose.yml` | 发布宿主机 443 端口，只读挂载证书目录 |
| `deploy/nginx/http.conf` | TLS、官网/后台反向代理与 HTTP 跳转 |
| `/opt/alfy-site/deploy/nginx/certs/alfy.com.cn.pem` | 完整证书链，仅保存在服务器受限目录 |
| `/opt/alfy-site/deploy/nginx/certs/alfy.com.cn.key` | 匹配私钥，仅保存在服务器受限目录 |

证书目录权限为 700，证书链和私钥文件权限为 600。下载目录 `SSL-Nginx-Certificaiton/`、服务器 `deploy/nginx/certs/` 与 `.key` 文件已列入 Git 忽略规则。不得把私钥、生产环境变量或服务器私有备份提交到仓库、输出到聊天。

阿里云安全组允许公网 TCP 443；若启用主机防火墙，也需允许 443。80 保留跳转用途。应用内部的 Node、Java 与数据库端口仍由 Docker 网络承载。

## 本次发布范围与验证

本次只更新 Nginx 的配置、端口和证书挂载。官网、后台、API、数据库容器未重建；数据库保持 healthy。

发布先启用 HTTP / HTTPS 并行访问，公网证书验证通过后再启用 HTTP 跳转。配置检查、证书链校验、证书与私钥匹配校验均通过。

验收覆盖三个域名的 HTTPS、同域健康接口、主要栏目、产品/案例/新闻详情、公开媒体与静态资源、移动端首页、百度站点验证标签，以及保留路径与查询参数的 HTTP 跳转。后台登录接口以空参数请求验证参数校验和代理链路，未使用或修改管理员账户。

临时后台演示端口 18081 及对应旧规则属于既有交接待办，本次未联动修改 API 或安全组。运维完成后应将临时开放的 SSH 恢复为指定来源；清理演示端口需另行核对使用情况。

## 日常验证

在服务器的 `deploy/` 目录下：

```bash
docker compose --env-file .env.production config --quiet
docker exec alfy-nginx nginx -t
curl --resolve www.alfy.com.cn:443:127.0.0.1 https://www.alfy.com.cn/api/v1/health
docker compose --env-file .env.production ps
```

从公网验证三个域名的 HTTPS，并检查 HTTP 的 308 跳转目标。证书检查必须保留正常的 CA 和主机名验证。

## 续签与替换

1. 在当前单张证书到期前，完成阿里云续签，并配合 OV 所需的域名和企业核验。
2. 下载 Nginx 格式的证书，核对三个域名、有效期、完整证书链和私钥匹配。私钥无需进入源码仓库。
3. 在服务器受限备份目录中备份当前证书及私钥，目录权限 700、文件权限 600。
4. 上传并替换 `deploy/nginx/certs/` 下的证书及私钥，维持固定文件名和权限。Compose 挂载整个目录，替换时不改变容器内路径。
5. 两个文件均更新后执行 `docker exec alfy-nginx nginx -t`。检查成功再执行 `docker exec alfy-nginx nginx -s reload`。
6. 从公网核对新证书指纹、有效期和域名，以及官网、后台和接口；失败时恢复匹配的旧证书与私钥并重载。

仅更新证书无需构建应用镜像。若修改了 Compose 的端口或挂载，则需校验 Compose 后仅更新 Nginx 容器。

## 本次发布回退

原配置保存在服务器：

```text
/opt/alfy-site/.codex-backups/https-20261029T084722Z/docker-compose.yml
/opt/alfy-site/.codex-backups/https-20261029T084722Z/http.conf
```

若需要撤销本次 HTTPS 部署，恢复这两个配置文件，执行 Compose 配置检查，再仅重建 Nginx 容器。该备份对应原 HTTP 服务，恢复后不会提供 HTTPS，须结合对外访问安排操作；不删除数据库卷、上传卷或应用容器。

历史服务器 Git 工作区存在差异，本次只回写已审查的部署配置；不得用全仓覆盖代替回退。
