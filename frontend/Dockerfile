# ---------- build ----------
FROM node:22-alpine AS build
WORKDIR /app

# Dependencies first, so this layer is cached until package-lock.json changes
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- run ----------
FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx/default.conf.template /etc/nginx/templates/
COPY nginx/proxy.inc /etc/nginx/conf.d/
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
