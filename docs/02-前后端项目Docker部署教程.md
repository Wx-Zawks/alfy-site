# 前后端项目部署到服务器教程（Docker Compose）

> 面向首次部署“前端官网 + 管理后台 + 后端 API + MySQL”的开发人员。
> 目标：从购买 Linux 服务器到可验证的生产部署，形成可重复执行、可维护的流程。

## 1. 总体原则

推荐使用 Docker Compose，而不是在服务器直接安装并手工启动 Node、Java、MySQL：

```text
浏览器 -> Nginx（80/443） -> 前端/后台/API 容器 -> MySQL 容器
```

这样做的好处是环境一致、更新可重复、端口暴露少、服务可自动重启、日志和状态可统一查看。

生产服务器只应公开 Nginx 的 80、443。MySQL 3306、后端 8080、Node 3000 和管理后台容器端口都放在 Docker 内网。

## 2. 先理解：Docker、Compose 和 Nginx 分别做什么

部署不是“把代码复制到服务器后双击运行”，而是把项目拆成多个职责清晰的运行单元，再由入口服务把访问正确地转发给它们。

### 2.1 Docker：把应用及其运行环境封装起来

Docker 用于把“代码运行需要的环境”一起打包。例如后端需要 Java 17，官网需要 Node.js，后台构建需要 pnpm，数据库需要 MySQL。若直接安装在服务器上，版本冲突、升级和迁移都会很麻烦；Docker 则让每个服务在独立环境中运行。

| 概念 | 可以理解为 | 在本项目中的例子 |
|---|---|---|
| Dockerfile | 制作应用运行包的配方 | `alfy-api/Dockerfile`、`afly-web/Dockerfile` |
| 镜像（image） | 按配方制作出的只读模板 | `deploy-api`、`mysql:8.4`、`nginx` |
| 容器（container） | 从镜像启动后的正在运行的进程 | `alfy-api`、`alfy-db`、`alfy-nginx` |
| Volume（数据卷） | 容器外的持久化磁盘空间 | `mysql_data`、`uploads_data` |
| Network（网络） | 容器之间的内部局域网 | Compose 创建的默认网络 |

镜像与容器的区别尤其重要：镜像类似“安装包/模板”，容器类似“启动后的程序”。删除和重建无状态容器通常没问题；但数据库数据和上传文件必须放到 volume 中，否则容器重建后会丢失。

### 2.2 多阶段构建：为什么一个 Dockerfile 会出现两个 FROM

前端与后端构建都需要很多编译工具和依赖，但运行时不需要这些工具。多阶段构建将二者分开：

```text
构建阶段
  Java JDK / Node.js / pnpm / Maven
  下载依赖、编译项目
      ↓ 只复制最终产物
运行阶段
  JRE 或精简 Node/Nginx
  只运行 JAR、Nuxt .output 或 dist 静态文件
```

这样生成的运行镜像更小、攻击面更少，且不把源码、构建缓存和开发依赖带入生产容器。

### 2.3 Docker Compose：把多个容器当作一个系统管理

一个企业网站往往至少有数据库、API、官网、后台和反向代理五个进程。Docker Compose 使用一份 `docker-compose.yml` 描述它们的关系：

- 哪些服务需要构建镜像，哪些直接使用现成镜像；
- 哪些环境变量需要传给后端和数据库；
- 哪些数据卷需要保存；
- 哪些服务要等待数据库健康后才能启动；
- 哪些端口允许映射到宿主机；
- 哪些容器应在异常退出后自动重启。

因此，`docker compose up -d --build` 的含义是“按配置构建需要重建的应用镜像，并在后台以一个整体启动全部服务”，而不是只启动一个网站。

### 2.4 Nginx：网站的统一入口和转发员

Nginx 是高性能 Web 服务器和反向代理。它监听公网 80/443 端口，根据用户访问的域名和路径，把请求转发到 Docker 内部对应服务。

“反向代理”可理解为：浏览器只认识 Nginx；Nginx 代替浏览器去访问官网、后台或 API，并把结果返回。浏览器无需知道 Node、Java 或 MySQL 的真实端口。

| 用户请求 | Nginx 判断依据 | 转发目标 |
|---|---|---|
| `https://alfy.com.cn/` | 域名为官网，路径为 `/` | Nuxt 官网 `web:3000` |
| `https://alfy.com.cn/api/v1/...` | 路径以 `/api/` 开头 | 后端 `api:8080` |
| `https://admin.alfy.com.cn/` | 域名为后台 | 后台静态站点 `admin:80` |
| `https://admin.alfy.com.cn/api/v1/...` | 后台域名下的 `/api/` | 后端 `api:8080` |

这也是前端建议使用相对 API 地址 `/api/v1` 的原因：官网和后台都只请求“当前域名的 `/api`”，由 Nginx 转发，无需把 API 8080 端口暴露给互联网。

### 2.5 本项目从浏览器到数据库的一次完整请求

以管理员登录为例：

```text
1. 浏览器访问 admin.alfy.com.cn
2. DNS 将域名解析为 ECS 公网 IP
3. 安全组允许 TCP 443（或备案前演示的受限 18081）
4. alfy-nginx 接收请求
5. Nginx 将 / 返回给 alfy-admin 静态页面
6. 后台页面请求 /api/v1/认证接口
7. Nginx 将 /api/ 转发给 alfy-api
8. alfy-api 通过 Compose 内部网络访问 alfy-db
9. MySQL 返回数据；结果按相反方向返回浏览器
```

官网读取新闻、产品和素材也是同样的链路，只是第一步访问的是官网域名，页面由 `alfy-web` 的 Nuxt 服务渲染。

### 2.6 本项目一次发布的原理

```text
修改源码
  -> 上传/拉取到服务器
  -> docker compose config 校验变量和语法
  -> docker compose up -d --build
  -> Docker 依据 Dockerfile 构建新镜像
  -> Compose 用新镜像重建有变化的容器
  -> MySQL 和上传 volume 保持不变
  -> Nginx/健康检查验证新版本
```

如果只修改 Nginx、Compose 或环境变量，通常执行 `docker compose up -d` 即可；如果修改了应用源码、依赖、Dockerfile，则必须加 `--build`。无论哪种情况，发布后都要检查容器状态、API 健康检查、官网和后台登录。

## 3. 购买服务器前的决策

| 项目 | 建议 |
|---|---|
| 地域 | 选择靠近主要用户的地域；中国内地地域正式建站必须完成 ICP 备案。 |
| 规格 | 轻量项目 2 核 4 GiB 起步；Java 后端且需要服务器构建时建议 4 核 8 GiB。 |
| 磁盘 | 建议至少 80–100 GiB；Docker 镜像、构建缓存、数据库和日志会增长。 |
| 带宽 | 官网起步可用 5 Mbps；图片/视频多时应使用 OSS/CDN。 |
| 系统 | 选 Linux，例如 Alibaba Cloud Linux 或 Ubuntu LTS。 |
| 域名 | 提前完成实名认证；规划官网、后台、API 子域名。 |
| 备份 | 购买时启用快照策略，后续仍需数据库和上传文件的独立备份。 |

域名建议：

```text
example.com / www.example.com  官网
admin.example.com              管理后台
api.example.com                可选 API 域名
```

## 4. 创建实例后的安全配置

### 3.1 登录

优先创建和使用 SSH 密钥。若必须使用密码，密码要随机、高强度，后续应逐步关闭 root 密码登录。

创建普通运维用户：

```bash
# 以 root 执行一次
useradd --create-home --shell /bin/bash deployer
passwd deployer
usermod -aG wheel deployer

# 验证 sudo
su - deployer
sudo whoami
```

输出应为 `root`。日常不要直接使用 root。

### 3.2 安全组

最小入方向规则：

| 端口 | 协议 | 来源 | 用途 |
|---|---|---|---|
| 22 | TCP | 仅运维电脑公网 IP/32 或 Workbench 网段 | SSH/Workbench |
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

不要开放数据库、后端和开发端口。临时演示端口只能对演示设备的公网 IP/32 放行，演示后立即关闭。

## 5. 安装 Docker 与验证

按发行版选择 Docker 官方或云厂商文档安装 Docker Engine、Compose Plugin 和 Buildx。安装完成后：

```bash
sudo systemctl enable --now docker
sudo docker --version
sudo docker compose version
sudo docker run --rm hello-world
```

如果拉取镜像超时，不要立刻重装 Docker。先用单独的 `docker pull` 验证镜像来源和网络。

## 6. 将项目容器化

### 5.1 后端

Java/Spring Boot 推荐多阶段 Dockerfile：

```dockerfile
FROM <JDK镜像> AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM <JRE镜像>
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

后端必须从环境变量读取数据库地址、数据库账号密码、JWT 密钥、初始管理员密码及上传目录，不应将生产秘密写入镜像。

### 5.2 SSR 官网

Nuxt SSR 需要运行构建后的 Node 服务，而不只是复制静态文件：

```dockerfile
FROM <Node镜像> AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM <Node镜像>
WORKDIR /app
ENV NODE_ENV=production
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000
COPY --from=build /app/.output /app/.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
```

### 5.3 管理后台

Vite/Vue 后台通常构建为静态资源，再用 Nginx 托管：

```dockerfile
FROM <Node镜像> AS build
WORKDIR /app
RUN corepack enable
COPY . .
RUN pnpm install --frozen-lockfile
RUN pnpm --dir apps/web run build

FROM <Nginx镜像>
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/web/dist /usr/share/nginx/html
EXPOSE 80
```

后台 API 地址推荐配置为相对路径 `/api/v1`；Nginx 统一转发，避免直接暴露 API 端口和跨域复杂度。

### 5.4 .dockerignore

每个构建目录都需要 .dockerignore，至少排除：

```text
node_modules/
.git/
dist/
.output/
target/
*.log
```

注意不能误排除源码。例如全局忽略规则 `**/data/` 可能会误伤 `src/data`，造成生产构建报 `ENOENT`。构建失败时，要同时检查 Git 忽略、Docker 忽略、压缩包内容和 import 路径。

## 7. Compose 和环境变量

建议服务为：`db`、`api`、`web`、`admin`、`nginx`。

关键要求：

- MySQL 使用命名 volume。
- API 依赖数据库 healthcheck，而不是只等待容器启动。
- 所有服务使用 `restart: unless-stopped`。
- 只有 Nginx 配置 `ports: 80:80` 和后续 `443:443`。
- 密钥仅写入 `.env.production`，提交 `.env.production.example` 作为模板。

示例：

```dotenv
MYSQL_ROOT_PASSWORD=高强度随机密码
MYSQL_APP_PASSWORD=高强度随机密码
APP_JWT_SECRET=足够长的随机密钥
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD=高强度随机密码
```

权限：

```bash
chmod 600 .env.production
```

## 8. Nginx 反向代理

官网与后台使用不同域名：

```nginx
server {
    listen 80;
    server_name example.com www.example.com;

    location /api/ {
        proxy_pass http://api:8080;
    }
    location / {
        proxy_pass http://web:3000;
    }
}

server {
    listen 80;
    server_name admin.example.com;

    location /api/ {
        proxy_pass http://api:8080;
    }
    location / {
        proxy_pass http://admin:80;
    }
}
```

每个代理 location 建议增加：

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

有文件上传时，按业务设置 `client_max_body_size`。

## 9. 上传源码并首次部署

创建目录：

```bash
sudo mkdir -p /opt/company-site
sudo chown -R deployer:deployer /opt/company-site
```

上传或 Git 拉取源码。不要上传本地 `.env`、`node_modules`、`.output`、`target`、私钥、数据库导出和真实上传素材。

首次启动：

```bash
cd /opt/company-site/deploy
cp .env.production.example .env.production
chmod 600 .env.production
# 用编辑器填写真实值

sudo docker compose --env-file .env.production config --quiet
echo $?

sudo docker compose --env-file .env.production up -d --build
```

只有校验命令返回 0 才启动。首次构建可能下载 Maven、npm、pnpm 依赖，出现一段时间无输出不一定失败；不要随意按 Ctrl+C。

## 10. 镜像、npm 和 Maven 网络问题

在中国内地服务器上，Docker Hub、npmjs 和 Maven Central 可能访问慢或超时。

处理顺序：

1. 单独执行 `docker pull <完整镜像地址>`，确认真实失败点。
2. 为 Docker 使用经验证可用的镜像来源。
3. Node 构建设置受信任 npm 镜像，例如 `npm_config_registry`；pnpm/Corepack 同时设置对应 registry。
4. Maven 使用项目级 `settings.xml` 指定稳定镜像。
5. 长期把基础镜像同步到公司私有 ACR，固定版本或 digest。

不要仅凭 `not found` 断言镜像标签不存在；公共镜像加速器可能只是没有同步该标签。

## 11. 部署后的验证

### 容器状态

```bash
sudo docker compose --env-file .env.production ps
```

数据库应为 `healthy`，其他服务应为 `Up`。

### API 健康检查

```bash
curl -i http://127.0.0.1/api/v1/health
```

应返回 HTTP 200。

### DNS 未生效时验证虚拟主机

```bash
curl -I -H 'Host: example.com' http://127.0.0.1/
curl -I -H 'Host: admin.example.com' http://127.0.0.1/
```

这可以验证 Nginx 分流，不依赖公网 DNS。

### 日志

```bash
sudo docker compose --env-file .env.production logs --tail=200 api
sudo docker compose --env-file .env.production logs --tail=200 nginx
sudo docker compose --env-file .env.production logs -f
```

## 12. ICP、DNS 与 HTTPS

中国内地服务器的正确顺序：

1. 域名实名认证。
2. 购买满足备案条件的服务器。
3. 提交并完成 ICP 备案。
4. 备案通过并可查询后，添加 DNS A 记录。
5. 验证官网、后台访问。
6. 申请证书（推荐 DNS 验证）。
7. 配置 Nginx 443 和 HTTP 自动跳转 HTTPS。
8. 网站正式开通后按要求办理公安联网备案等手续。

备案未完成时，不应将生产域名长期解析到中国内地服务器并对外运营；云厂商可能显示备案拦截页。

## 13. 更新、回滚与备份

发布代码或 Dockerfile：

```bash
sudo docker compose --env-file .env.production up -d --build
sudo docker compose --env-file .env.production ps
curl -i http://127.0.0.1/api/v1/health
```

仅 Compose、Nginx 或环境变量变化：

```bash
sudo docker compose --env-file .env.production up -d
```

关键原则：

- 发布前备份数据库和上传文件。
- 镜像回滚不等于数据库回滚；Flyway 等迁移必须评估可逆性。
- 不要在生产环境使用 `docker compose down -v`。
- 不要在未理解后果时执行 `docker system prune -a`。
- 定期做快照、数据库导出、上传文件异地备份，并实际演练恢复。
- 演示用端口、临时安全组规则和 hosts 记录必须在用完后删除。
