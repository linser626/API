# 🚀 AI Relay - AI大模型订阅中转平台

一站式AI大模型API中转服务，支持国内外主流大模型，提供订阅计费、流量监控、优惠券等完整运营能力。

## ✨ 功能特性

### 🔥 核心能力
- **API中转代理** - 兼容OpenAI API格式，无缝接入各类AI应用
- **多模型支持** - OpenAI / Claude / Gemini / 通义千问 / 文心一言 / 智谱 / DeepSeek 等
- **负载均衡** - 优先级/加权随机/轮询/最低延迟 4种策略
- **故障转移** - 自动切换可用渠道，保障服务稳定
- **SSE流式输出** - 支持Server-Sent Events实时流式响应

### 💰 计费与支付
- **Token精确计费** - 按模型区分输入/输出Token单价
- **订阅套餐** - 免费版/基础版/专业版/企业版 4档方案
- **余额充值** - 预充值模式，按量扣费
- **支付宝支付** - PC网页支付 + 手机网站支付
- **微信支付** - Native扫码支付 + JSAPI公众号支付
- **优惠券系统** - 固定金额/百分比折扣，支持兑换码

### 📊 监控与管理
- **流量监控** - 实时请求量、Token用量、费用统计
- **用量报表** - 按日/模型维度的用量分析
- **渠道健康** - 渠道成功率、响应时间、错误追踪
- **管理后台** - 用户/渠道/订单/定价全面管理
- **请求日志** - 完整请求记录，支持审计和调试

### 🔐 安全与限流
- **JWT认证** - 双Token机制（Access + Refresh）
- **API Key管理** - 独立密钥，支持创建/吊销/轮换
- **速率限制** - RPM/TPM双维度Redis滑动窗口限流
- **配额管理** - 按套餐设置Token额度和请求限制

## 🏗️ 技术架构

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   用户端     │────▶│   中转网关层      │────▶│  上游API供应商   │
│  (Web/API)  │     │  (Spring Boot)   │     │  OpenAI/Claude  │
└─────────────┘     │  - 鉴权           │     │  Gemini/通义    │
                    │  - 限流           │     │  文心/智谱      │
                    │  - 负载均衡       │     └─────────────────┘
                    │  - 故障转移       │
                    │  - Token计数      │
                    │  - 日志记录       │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │   业务服务层       │
                    │  - 用户服务       │
                    │  - 订阅服务       │
                    │  - 计费服务       │
                    │  - 渠道服务       │
                    │  - 优惠券服务     │
                    │  - 支付服务       │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │   数据存储层       │
                    │  MySQL 8.0       │
                    │  Redis 7         │
                    └──────────────────┘
```

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.x | 核心框架 |
| Spring Security | 6.x | 安全认证 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0 | 关系数据库 |
| Redis | 7.x | 缓存/限流 |
| JWT (jjwt) | 0.12.3 | Token认证 |
| WebClient | - | 响应式HTTP客户端（中转代理） |
| Alipay SDK | 4.39.x | 支付宝支付 |
| WeChat Pay SDK | 0.2.x | 微信支付 |
| Knife4j | 4.4.0 | API文档 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.4.x | 前端框架 |
| Element Plus | 2.6.x | UI组件库 |
| Pinia | 2.1.x | 状态管理 |
| Vue Router | 4.3.x | 路由管理 |
| ECharts | 5.5.x | 数据可视化 |
| Axios | 1.6.x | HTTP客户端 |
| Vite | 5.1.x | 构建工具 |

## 📁 项目结构

```
ai-relay-platform/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/airelay/
│   │   ├── common/                   # 通用组件
│   │   ├── config/                   # 配置类
│   │   ├── security/                 # JWT安全认证
│   │   ├── user/                     # 用户模块
│   │   ├── apikey/                   # API Key管理
│   │   ├── channel/                  # 渠道管理
│   │   ├── relay/                    # 🔥 核心中转模块
│   │   ├── billing/                  # 计费模块
│   │   ├── subscription/             # 订阅模块
│   │   ├── payment/                  # 支付模块
│   │   ├── coupon/                   # 优惠券模块
│   │   ├── monitor/                  # 监控模块
│   │   └── admin/                    # 管理模块
│   └── src/main/resources/
│       ├── application.yml           # 开发配置
│       ├── application-prod.yml      # 生产配置
│       └── db/schema.sql             # 数据库Schema
│
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── api/                      # API调用层
│       ├── router/                   # 路由配置
│       ├── stores/                   # Pinia状态管理
│       ├── components/layout/        # 布局组件
│       ├── views/                    # 页面视图
│       │   ├── auth/                 # 登录/注册
│       │   ├── dashboard/            # 用户仪表盘
│       │   ├── apikey/               # API Key管理
│       │   ├── subscription/         # 订阅方案
│       │   ├── billing/              # 账单充值
│       │   ├── monitor/              # 用量监控
│       │   ├── coupon/               # 优惠券
│       │   └── admin/                # 管理后台
│       └── utils/                    # 工具函数
│
├── nginx/                            # Nginx配置
├── docker-compose.yml                # Docker编排
├── .env.example                      # 环境变量模板
└── LICENSE                           # MIT许可证
```

## 🚀 快速开始

### 前置条件
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7+
- Docker & Docker Compose（可选）

### 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/your-username/API.git
cd API

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入配置

# 3. 一键启动
docker-compose up -d

# 4. 访问
# 前端: http://localhost
# 后端API: http://localhost:8080
# API文档: http://localhost:8080/doc.html
```

### 方式二：本地开发

#### 后端启动

```bash
cd backend

# 1. 创建数据库
mysql -u root -p < src/main/resources/db/schema.sql

# 2. 修改配置
# 编辑 src/main/resources/application.yml
# 填入MySQL和Redis连接信息

# 3. 启动
./mvnw spring-boot:run
```

#### 前端启动

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev

# 3. 构建生产版本
npm run build
```

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

> ⚠️ 生产环境请务必修改默认密码！

## 📡 API接口

### 中转API（兼容OpenAI格式）

```bash
# Chat Completion
curl http://your-domain/v1/chat/completions \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello!"}],
    "stream": true
  }'

# Embeddings
curl http://your-domain/v1/embeddings \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-ada-002",
    "input": "Hello world"
  }'

# List Models
curl http://your-domain/v1/models \
  -H "Authorization: Bearer sk-your-api-key"
```

### 业务API

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `/api/auth/**` | 注册/登录/刷新Token |
| 用户 | `/api/user/**` | 个人信息/修改密码 |
| API Key | `/api/apikeys/**` | 密钥CRUD |
| 订阅 | `/api/subscription/**` | 套餐/订阅/配额 |
| 计费 | `/api/billing/**` | 余额/交易/充值 |
| 支付 | `/api/payment/**` | 发起支付/订单查询 |
| 优惠券 | `/api/coupon/**` | 兑换/我的优惠券 |
| 监控 | `/api/monitor/**` | 用量统计/趋势 |
| 管理后台 | `/api/admin/**` | 全部管理功能 |

完整API文档启动后访问：`http://localhost:8080/doc.html`

## 🗄️ 数据库设计

共11张核心表：

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `api_key` | API密钥表 |
| `channel` | 上游渠道表 |
| `model_price` | 模型定价表 |
| `plan` | 订阅方案表 |
| `subscription` | 用户订阅表 |
| `order` | 订单表 |
| `balance_transaction` | 余额交易流水表 |
| `coupon` | 优惠券表 |
| `user_coupon` | 用户优惠券表 |
| `request_log` | 请求日志表 |

## 🔧 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL密码 | - |
| `JWT_SECRET` | JWT签名密钥 | - |
| `ALIPAY_APP_ID` | 支付宝应用ID | - |
| `ALIPAY_PRIVATE_KEY` | 支付宝私钥 | - |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 | - |
| `WECHAT_APP_ID` | 微信应用ID | - |
| `WECHAT_MCH_ID` | 微信商户号 | - |
| `WECHAT_API_KEY` | 微信API密钥 | - |

### 负载均衡策略

| 策略 | 说明 |
|------|------|
| `PRIORITY` | 按渠道优先级选择（默认） |
| `WEIGHTED_RANDOM` | 按权重随机分配 |
| `ROUND_ROBIN` | 轮询分配 |
| `LEAST_LATENCY` | 选择最低延迟的渠道 |

## 📋 后续规划

- [ ] 渠道健康检测 - 定时Ping上游API，自动标记不可用渠道
- [ ] 通知系统 - 余额不足/订阅到期邮件/站内信提醒
- [ ] 团队/组织 - 多人协作，子账号管理
- [ ] API文档中心 - 在线交互式文档
- [ ] 推荐返利 - 邀请链接 + 余额返利
- [ ] 自动续费 - 订阅到期自动扣款
- [ ] 在线聊天测试 - 网页端直接测试模型对话
- [ ] 数据导出 - 用量数据CSV/Excel导出
- [ ] 多语言支持 - 中英文界面切换

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

⭐ 如果这个项目对你有帮助，请给个Star支持一下！
