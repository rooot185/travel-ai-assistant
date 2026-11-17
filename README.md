# 路书 · 自由行规划 Agent

为中国大陆城市自由行生成和修改行程。Java Agent 查询高德地图，以实际地点、路段与耗时校验模型拟定的安排。用户可以锁定地点和时间，再用自然语言调整指定日期。

## 本地运行

需要 Docker 和 Docker Compose。复制配置并设置独立的数据库密码。

```bash
cp .env.example .env
docker compose up --build --wait
```

打开 [http://localhost:8088](http://localhost:8088)。默认可以打开界面、注册和登录。要使用实时规划，请在 `.env` 中配置 **模型及高德 Web 服务**，然后重新执行 `docker compose up -d`。

| 配置 | 用途 |
| --- | --- |
| `DATABASE_PASSWORD` | PostgreSQL 密码，必填 |
| `AI_ENABLED` | 设为 `true` 启用规划 |
| `MODEL_URL` | 支持工具调用和 JSON 输出的 Chat Completions 地址 |
| `MODEL_NAME` | 服务提供商认可的模型名 |
| `MODEL_API_KEY` | 模型服务密钥 |
| `AMAP_WEB_KEY` | 高德 **Web 服务 API** Key，具有行政区、POI 和路线权限 |
| `COOKIE_SECURE` | HTTPS 部署设为 `true`。默认示例仅用于本机 HTTP |
| `ALLOW_REGISTRATION` | 对外演示时可在创建演示账号后关闭自助注册 |

使用 [DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/) 和 [高德地点检索](https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch)、[高德路线规划](https://lbs.amap.com/api/webservice/guide/api/newroute)。接入其他服务时，选择支持 `tools`、`response_format` 和 `max_tokens` 的兼容模型。密钥只配置在服务端。

## 面试时演示

1. 登录工作台，点击「杭州示例」，设置未来日期并生成。
2. 查看地点、路段查询结果和整组预算。展开「数据来源及本次调用」核对查询次数。
3. 锁定某一站，选择需要调整的日期，输入「第一天下午安排轻松一点，保留已锁定的景点」。跨日移动时同时选择涉及的两天。
4. 对照更新后的日程、路线和调整说明，从「我的行程」重新打开或导出。

费用是估算。站点营业时间、预约、门票价格和出发当天交通需另行核实。高德返回的是查询时的路线估计。页面遇到上游失败会显示具体步骤及处理建议。

## 代码和验证

- `backend/` 使用 Java 21、Spring Boot、Maven、Flyway 和 PostgreSQL。
- `web/` 使用 Vue 3、TypeScript 和 Vite。生产环境由 Nginx 同源代理 API。
- [实现与部署说明](docs/architecture.md)包含 API 契约、边界和旧版数据处理方式。

```bash
mvn -B -f backend/pom.xml verify
npm --prefix web ci
npm --prefix web run build
```

设定 `TRAVEL_TEST_DB`、`TRAVEL_TEST_DB_USER` 和 `TRAVEL_TEST_DB_PASSWORD` 可运行 PostgreSQL 集成测试。使用独立测试数据库。测试提供受控的模型和高德响应，不消耗外部调用额度。

西湖照片由 Taobot 拍摄，图像已缩小并转为 WebP。[原图](https://commons.wikimedia.org/wiki/File:West_Lake_-_Hangzhou,_China.jpg) · [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/)。字体见 [第三方资产说明](docs/assets.md)。
