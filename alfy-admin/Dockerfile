FROM m.daocloud.io/docker.io/library/node:22-bookworm-slim AS build

WORKDIR /app

ENV COREPACK_NPM_REGISTRY=https://registry.npmmirror.com
ENV npm_config_registry=https://registry.npmmirror.com

RUN corepack enable
COPY . .
RUN pnpm install --frozen-lockfile
RUN pnpm --dir apps/web-ele run build

FROM m.daocloud.io/docker.io/library/nginx:1.30.4-alpine

COPY apps/web-ele/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/web-ele/dist /usr/share/nginx/html

EXPOSE 80
