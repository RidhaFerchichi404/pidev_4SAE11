# Stage 1: Build Angular app
FROM node:22-alpine AS builder
WORKDIR /app

# Cache node_modules layer — only reinstalls when package-lock changes
COPY package*.json ./
RUN npm ci --prefer-offline

# Build production bundle (public API URL for browser; override with --build-arg)
COPY . .
ARG API_GATEWAY_PUBLIC_URL=http://api.smartfreelance.example.com
RUN sed -i "s|__API_GATEWAY_PUBLIC_URL__|${API_GATEWAY_PUBLIC_URL}|g" src/environments/environment.production.ts
RUN npm run build -- --configuration production

# Stage 2: Serve with Nginx
FROM nginx:1.27-alpine

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy built app and custom Nginx config
COPY --from=builder /app/dist/smart-freelance-app/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Non-root: Nginx worker runs as nginx user by default in this image
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
