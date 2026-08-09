FROM m.daocloud.io/docker.io/library/node:22-bookworm-slim AS build

WORKDIR /app

ENV npm_config_registry=https://registry.npmmirror.com

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM m.daocloud.io/docker.io/library/node:22-bookworm-slim

WORKDIR /app

ENV NODE_ENV=production
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000

COPY --from=build /app/.output /app/.output

EXPOSE 3000

CMD ["node", ".output/server/index.mjs"]
