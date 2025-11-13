# AI旅行助手 - 前端应用

基于 Vue 3 + Ant Design Vue + DeepSeek API 的智能旅行规划Web应用。

## 功能特性

- 🎯 **智能旅行规划** - 基于用户输入生成个性化旅行路线
- 🗺️ **地图集成** - 可视化展示旅行路线和地点
- 📱 **响应式设计** - 适配各种屏幕尺寸
- 🔐 **用户认证** - 完整的登录注册系统
- 💰 **预算管理** - 详细的费用预算分析
- 📋 **行程卡片** - 美观的每日行程展示

## 技术栈

- **前端框架**: Vue 3 + Composition API
- **UI组件库**: Ant Design Vue 4.x
- **状态管理**: Pinia
- **路由管理**: Vue Router 4
- **HTTP客户端**: Axios
- **地图服务**: L7 (预留集成)
- **构建工具**: Vite
- **AI集成**: DeepSeek API

## 项目结构

```
travel-assistant-1/
├── src/
│   ├── components/          # 可复用组件
│   │   ├── TravelPlanForm.vue    # 旅行计划表单
│   │   ├── TravelMap.vue         # 地图组件
│   │   └── ItineraryCard.vue     # 行程卡片
│   ├── views/               # 页面组件
│   │   ├── Login.vue             # 登录页面
│   │   ├── Register.vue          # 注册页面
│   │   ├── Dashboard.vue         # 仪表板
│   │   └── TravelPlan.vue        # 旅行计划详情
│   ├── stores/              # 状态管理
│   │   ├── auth.js               # 认证状态
│   │   └── travel.js             # 旅行数据状态
│   ├── services/            # API服务
│   │   └── deepseek.js           # DeepSeek API集成
│   ├── router/              # 路由配置
│   │   └── index.js
│   ├── assets/              # 静态资源
│   │   └── global.css
│   └── main.js              # 应用入口
├── package.json
├── vite.config.js
└── index.html
```

## 快速开始

### 环境要求

- Node.js 16+
- npm 或 yarn

### 安装依赖

```bash
npm install
```

### 配置环境变量

复制 `.env.example` 为 `.env` 并配置：

```env
VITE_DEEPSEEK_API_KEY=your-deepseek-api-key-here
VITE_API_BASE_URL=http://localhost:8080/api
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 主要功能说明

### 1. 用户认证
- 用户注册/登录
- JWT token管理
- 路由守卫保护

### 2. 旅行计划生成
用户输入以下信息生成个性化旅行计划：
- 目的地
- 旅行开始时间
- 旅行天数 (1-30天)
- 同行人数 (1-10人)
- 总预算 (人民币)
- 旅行偏好 (休闲、冒险、观景、体验等)
- 额外需求

### 3. 旅行计划展示
- 每日详细行程安排
- 住宿、餐饮、交通推荐
- 预算明细分析
- 地图可视化
- 实用信息提示

### 4. AI集成
- 集成DeepSeek API生成智能旅行计划
- 支持JSON格式数据解析
- 容错处理机制

## API接口

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

### 旅行计划相关
- `POST /api/travel/generate` - 生成旅行计划
- `POST /api/travel/save` - 保存旅行计划
- `GET /api/travel/history` - 获取历史计划

## 开发说明

### 添加新组件
1. 在 `src/components/` 创建Vue组件
2. 在需要使用的地方导入
3. 遵循Vue 3 Composition API规范

### 状态管理
使用Pinia进行状态管理：
- `auth` store: 管理用户认证状态
- `travel` store: 管理旅行数据

### 样式规范
- 使用Ant Design Vue组件
- 自定义样式写在组件scoped样式块中
- 全局样式在 `src/assets/global.css`

## 部署说明

### 构建生产版本
```bash
npm run build
```

### 部署到静态服务器
将 `dist` 目录部署到任何静态文件服务器。

## 许可证

MIT License