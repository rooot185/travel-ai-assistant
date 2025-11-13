# AI旅行助手 - 项目总览

完整的全栈Web应用，包含前端Vue应用和后端SpringBoot服务。

## 🎯 项目简介

AI旅行助手是一个智能旅行规划平台，用户可以通过输入目的地、预算、偏好等信息，由AI自动生成个性化的旅行路线，包括详细的交通、住宿、景点、餐厅等安排。

## 🏗️ 技术架构

### 前端技术栈
- **框架**: Vue 3 + Composition API
- **UI组件**: Ant Design Vue 4.x
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite
- **地图**: L7 (预留集成)

### 后端技术栈
- **框架**: Spring Boot 3.2.0
- **安全**: Spring Security + JWT
- **数据库**: H2 (内存数据库)
- **ORM**: Spring Data JPA
- **HTTP客户端**: WebFlux
- **构建工具**: Maven
- **AI集成**: DeepSeek API

## 📁 项目结构

```
travel-assistant-1/
├── frontend/                 # Vue前端应用
│   ├── src/
│   │   ├── components/       # 可复用组件
│   │   ├── views/           # 页面组件
│   │   ├── stores/          # 状态管理
│   │   ├── services/        # API服务
│   │   ├── router/          # 路由配置
│   │   └── assets/          # 静态资源
│   ├── package.json
│   ├── vite.config.js
│   └── index.html
├── backend/                 # SpringBoot后端服务
│   ├── src/main/java/com/travel/
│   │   ├── config/          # 配置类
│   │   ├── controller/      # 控制器
│   │   ├── dto/            # 数据传输对象
│   │   ├── entity/         # 实体类
│   │   ├── repository/     # 数据访问层
│   │   ├── security/       # 安全相关
│   │   ├── service/        # 业务逻辑层
│   │   └── TravelAssistantApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
└── README.md               # 项目说明文档
```

## 🚀 快速开始

### 前端启动

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:3000

### 后端启动

```bash
# 进入后端目录
cd backend

# 配置环境变量
cp .env.example .env
# 编辑.env文件，配置DeepSeek API密钥

# 编译并运行
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

## 📋 功能特性

### 用户认证
- ✅ 用户注册/登录
- ✅ JWT令牌管理
- ✅ 路由守卫保护

### 旅行规划
- ✅ 智能旅行计划生成
- ✅ 个性化路线定制
- ✅ 预算分析和建议
- ✅ 详细行程安排

### 数据管理
- ✅ 用户数据持久化
- ✅ 旅行计划存储
- ✅ 历史记录查询

### 用户体验
- ✅ 响应式设计
- ✅ 美观的UI界面
- ✅ 地图可视化(预留)
- ✅ 实时表单验证

## 🔌 API接口

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

### 旅行计划接口
- `POST /api/travel/generate` - 生成旅行计划
- `GET /api/travel/history` - 获取历史计划
- `GET /api/travel/{id}` - 获取计划详情
- `POST /api/travel/save` - 保存旅行计划

## 🎨 界面预览

### 登录页面
- 现代化的登录表单
- 表单验证和错误提示
- 注册链接

### 仪表板
- 欢迎信息和功能介绍
- 旅行计划生成表单
- 历史计划列表

### 旅行计划详情
- 每日行程安排
- 预算明细分析
- 地图可视化展示
- 实用信息提示

## 🔧 开发说明

### 前端开发
- 使用Vue 3 Composition API
- 遵循组件化开发原则
- 使用Pinia进行状态管理
- 集成Ant Design Vue组件

### 后端开发
- 使用Spring Boot框架
- 遵循RESTful API设计
- 使用JWT进行认证
- 集成DeepSeek AI服务

### 数据库
- 当前使用H2内存数据库
- 支持切换到MySQL/PostgreSQL
- 自动创建表结构

## 🚀 部署说明

### 前端部署
```bash
npm run build
# 将dist目录部署到静态服务器
```

### 后端部署
```bash
mvn clean package
java -jar target/travel-assistant-1.0.0.jar
```

## 📝 许可证

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request来改进项目！