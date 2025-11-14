# 多阶段构建：前端构建阶段
FROM node:18-alpine as frontend-build

WORKDIR /app/front

# 复制前端package文件
COPY front/package*.json ./

# 安装前端依赖
RUN npm ci

# 复制前端源代码
COPY front/ ./

# 构建前端应用
RUN npm run build

# 后端构建阶段
FROM node:18-alpine as backend-build

WORKDIR /app/backend

# 复制后端package文件
COPY backend/package*.json ./

# 安装后端依赖
RUN npm ci --only=production

# 复制后端源代码
COPY backend/ ./

# 生产阶段
FROM node:18-alpine

# 安装nginx
RUN apk add --no-cache nginx

# 创建工作目录
WORKDIR /app

# 从前端构建阶段复制构建好的文件
COPY --from=frontend-build /app/front/dist ./frontend

# 从后端构建阶段复制后端应用
COPY --from=backend-build /app/backend ./backend

# 复制nginx配置
COPY nginx.conf /etc/nginx/http.d/default.conf

# 创建nginx运行目录
RUN mkdir -p /run/nginx

# 创建数据库目录
RUN mkdir -p /app/backend/database

# 暴露端口
EXPOSE 80

# 启动脚本
COPY start.sh .
RUN chmod +x start.sh

# 启动应用
CMD ["./start.sh"]