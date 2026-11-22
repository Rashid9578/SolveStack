FROM node:22-bookworm-slim AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --prefer-offline --no-audit --no-fund
COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

FROM nginx:stable-alpine
RUN chown -R nginx:nginx /etc/nginx/conf.d
ENV SOLVESTACK_API_UPSTREAM=backend:8080 \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1 \
    NGINX_ENVSUBST_FILTER="^(SOLVESTACK_API_UPSTREAM|NGINX_LOCAL_RESOLVERS)$"
COPY --from=build /workspace/dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf.template /etc/nginx/templates/default.conf.template
USER 101:101
EXPOSE 8080
