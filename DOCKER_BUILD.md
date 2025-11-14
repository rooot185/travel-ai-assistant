# Docker 构建和使用说明

## 项目概述
这是一个完整的旅行助手应用，包含：
- **前端**: Vue 3 + Ant Design + 地图组件
- **后端**: Node.js + Express + SQLite数据库
- **代理**: Nginx 统一处理前后端请求

## Docker镜像构建

### 前提条件
- 确保Docker已安装并正在运行
- 在项目根目录下执行以下命令

### 构建镜像
```bash
# 构建包含前后端的完整镜像
docker build -t travel-assistant:latest .

# 查看构建的镜像
docker images
```

### 运行容器
```bash
# 运行容器（生产模式）
docker run -d \
  --name travel-assistant \
  -p 80:80 \
  -e NODE_ENV=production \
  travel-assistant:latest

# 运行容器并持久化数据库
mkdir -p database
docker run -d \
  --name travel-assistant \
  -p 80:80 \
  -v $(pwd)/database:/app/backend/database \
  -e NODE_ENV=production \
  travel-assistant:latest
```

### 带环境变量的运行
```bash
# 使用环境变量文件
docker run -d \
  --name travel-assistant \
  -p 80:80 \
  --env-file .env \
  travel-assistant:latest
```

## 容器管理

### 查看容器状态
```bash
# 查看运行中的容器
docker ps

# 查看所有容器
docker ps -a
```

### 查看日志
```bash
# 查看容器日志
docker logs travel-assistant

# 实时查看日志
docker logs -f travel-assistant
```

### 停止和删除容器
```bash
# 停止容器
docker stop travel-assistant

# 删除容器
docker rm travel-assistant

# 强制删除容器
docker rm -f travel-assistant
```

### 进入容器
```bash
# 进入容器shell
docker exec -it travel-assistant sh
```

## 访问应用

### 前端界面
构建完成后，可以通过以下URL访问前端界面：
```
http://localhost
```

### API接口
API接口通过Nginx代理，可以通过以下URL访问：
```
http://localhost/api/
```

### 健康检查
容器内置了健康检查，可以通过以下URL验证服务状态：
```
http://localhost/api/health
```

## 环境变量配置

### 必需的环境变量
- `PORT`: 服务端口（默认：8080）
- `NODE_ENV`: 运行环境（development/production）

### 可选的环境变量
- `FRONTEND_URL`: 前端URL（用于CORS配置）
- `JWT_SECRET`: JWT密钥
- `DATABASE_PATH`: 数据库文件路径

## Docker Compose（可选）

创建 `docker-compose.yml` 文件：
```yaml
version: '3.8'

services:
  travel-assistant:
    build: .
    ports:
      - "8080:8080"
    environment:
      - NODE_ENV=production
    volumes:
      - ./database:/app/database
    restart: unless-stopped
```

使用Docker Compose启动：
```bash
docker-compose up -d
```