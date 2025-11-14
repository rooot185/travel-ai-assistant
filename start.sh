#!/bin/sh

# 启动后端服务
cd /app/backend
node app.js &

# 等待后端启动
sleep 5

# 启动nginx
nginx -g "daemon off;"