# Travel Assistant Backend

AI旅行助手后端API服务，为前端提供用户认证、旅行计划生成和数据存储功能。

## 功能特性

- 🔐 JWT用户认证系统
- 🗺️ 智能旅行计划生成
- 💾 SQLite数据存储
- 📊 用户旅行历史管理
- 🛡️ 安全中间件保护
- 📝 输入数据验证

## 快速开始

### 环境要求

- Node.js 16+
- npm 或 yarn

### 安装依赖

```bash
cd backend
npm install
```

### 环境配置

1. 复制环境变量文件：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，配置必要的环境变量：
```env
JWT_SECRET=your-super-secret-jwt-key-here
DEEPSEEK_API_KEY=your-deepseek-api-key-here
```

### 启动服务

```bash
# 开发模式
npm run dev

# 生产模式
npm start
```

服务将在 http://localhost:8080 启动

## API接口文档

### 认证接口

#### POST /api/auth/login
用户登录
```json
{
  "username": "demo",
  "password": "demo123"
}
```

#### POST /api/auth/register
用户注册
```json
{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

#### GET /api/auth/verify
验证JWT令牌

### 旅行计划接口

#### POST /api/travel/generate
生成旅行计划
```json
{
  "destination": "北京",
  "startDate": "2024-01-15",
  "days": 5,
  "travelers": 2,
  "budget": 5000,
  "preferences": ["美食", "文化"],
  "additionalRequirements": "需要无障碍设施"
}
```

#### POST /api/travel/save
保存旅行计划
```json
{
  "planData": {
    // 完整的旅行计划数据
  }
}
```

#### GET /api/travel/history
获取旅行历史

#### GET /api/travel/:id
获取特定旅行计划详情

#### DELETE /api/travel/:id
删除旅行计划

### 用户接口

#### GET /api/user/profile
获取用户资料

#### PUT /api/user/profile
更新用户资料

#### GET /api/user/stats
获取用户统计信息

## 数据库结构

### Users表
- id (主键)
- username (用户名)
- email (邮箱)
- password (加密密码)
- created_at (创建时间)
- updated_at (更新时间)

### Travel_plans表
- id (主键)
- user_id (外键)
- destination (目的地)
- start_date (开始日期)
- days (天数)
- travelers (人数)
- budget (预算)
- preferences (偏好设置)
- additional_requirements (额外要求)
- plan_data (完整计划数据)
- created_at (创建时间)
- updated_at (更新时间)

## 安全特性

- JWT令牌认证
- 密码bcrypt加密
- Helmet安全头设置
- 请求频率限制
- 输入数据验证
- CORS跨域保护

## 开发说明

### 测试账号
系统会自动创建测试账号：
- 用户名：demo
- 密码：demo123

### 数据格式
所有API响应都遵循统一的JSON格式：
```json
{
  "success": true,
  "data": {},
  "message": "操作成功"
}
```

错误响应：
```json
{
  "error": "错误类型",
  "message": "错误描述"
}
```

## 部署说明

1. 设置生产环境变量
2. 确保数据库文件可写
3. 配置反向代理（如Nginx）
4. 使用PM2等进程管理器

## 许可证

MIT License