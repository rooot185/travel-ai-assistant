# 🐳 Docker 镜像使用说明

## 快速开始

### 方法1: 使用 GitHub Container Registry (推荐)

```bash
# 拉取最新版本
docker pull ghcr.io/your-username/travel-assistant:latest

# 运行应用
docker run -d --name travel-assistant -p 80:80 ghcr.io/your-username/travel-assistant:latest

# 访问应用
open http://localhost
```

### 方法2: 下载镜像文件

1. 前往 GitHub Releases 页面
2. 下载 `travel-assistant.tar.gz` 文件
3. 加载镜像：
```bash
docker load < travel-assistant.tar.gz

# 运行
docker run -d --name travel-assistant -p 80:80 travel-assistant
```

### 方法3: 从源码构建

```bash
git clone https://github.com/your-username/travel-assistant.git
cd travel-assistant

docker build -t travel-assistant .
docker run -d --name travel-assistant -p 80:80 travel-assistant
```

## 持久化数据库

```bash
mkdir -p database
docker run -d --name travel-assistant -p 80:80 -v $(pwd)/database:/app/backend/database travel-assistant
```

## 环境变量

```bash
docker run -d --name travel-assistant -p 80:80 -e NODE_ENV=production travel-assistant
```