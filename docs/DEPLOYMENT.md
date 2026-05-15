# 🚀 AI Relay 部署文档

本文档详细说明如何从零开始部署 AI Relay 大模型中转平台。

---

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 拉取代码](#2-拉取代码)
- [3. 方式一：Docker Compose 一键部署（推荐）](#3-方式一docker-compose-一键部署推荐)
- [4. 方式二：手动部署](#4-方式二手动部署)
- [5. 配置详解](#5-配置详解)
- [6. 支付配置](#6-支付配置)
- [7. Nginx 配置](#7-nginx-配置)
- [8. HTTPS 配置](#8-https-配置)
- [9. 数据库维护](#9-数据库维护)
- [10. 常见问题](#10-常见问题)

---

## 1. 环境要求

### 最低配置

| 组件 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 磁盘 | 20GB | 50GB+ SSD |
| 操作系统 | Ubuntu 20.04 / CentOS 7+ | Ubuntu 22.04 LTS |

### 软件依赖

#### Docker 部署方式（推荐）

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器引擎 |
| Docker Compose | v2.0+ | 容器编排 |

#### 手动部署方式

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存/限流 |
| Nginx | 1.20+ | 反向代理 |

---

## 2. 拉取代码

```bash
# 克隆仓库
git clone https://github.com/linser626/API.git

# 进入项目目录
cd API
```

---

## 3. 方式一：Docker Compose 一键部署（推荐）

### 3.1 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置
vim .env
```

`.env` 文件内容说明：

```bash
# ==================== MySQL 配置 ====================
MYSQL_ROOT_PASSWORD=your_mysql_password_here    # MySQL root密码，请修改为强密码
MYSQL_DATABASE=ai_relay                         # 数据库名，默认不用改

# ==================== Spring Boot 配置 ====================
SPRING_DATASOURCE_USERNAME=root                 # 数据库用户名
SPRING_DATASOURCE_PASSWORD=your_mysql_password_here  # 与上面MYSQL_ROOT_PASSWORD保持一致

# ==================== JWT 配置 ====================
# JWT签名密钥，必须修改！建议使用64位以上随机字符串
# 生成方法: openssl rand -base64 64
JWT_SECRET=your_jwt_secret_here_please_change_this_to_a_random_string

# ==================== 支付宝配置 ====================
# 如果暂时不需要支付功能，可以先留空
ALIPAY_APP_ID=                                  # 支付宝应用AppID
ALIPAY_PRIVATE_KEY=                             # 支付宝应用私钥
ALIPAY_PUBLIC_KEY=                              # 支付宝公钥

# ==================== 微信支付配置 ====================
# 如果暂时不需要支付功能，可以先留空
WECHAT_APP_ID=                                  # 微信AppID
WECHAT_MCH_ID=                                  # 微信商户号
WECHAT_API_KEY=                                 # 微信APIv3密钥
WECHAT_CERT_PATH=                               # 微信证书路径（容器内路径）
```

### 3.2 一键启动

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看后端日志
docker-compose logs -f backend
```

### 3.3 验证部署

```bash
# 检查后端健康
curl http://localhost:8080/api/auth/login

# 检查前端
curl http://localhost

# 浏览器访问
# 前端: http://你的服务器IP
# API文档: http://你的服务器IP/api/doc.html（仅开发环境）
```

### 3.4 常用 Docker 命令

```bash
# 停止所有服务
docker-compose down

# 重启某个服务
docker-compose restart backend

# 重新构建并启动（代码更新后）
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend    # 后端日志
docker-compose logs -f frontend   # 前端日志
docker-compose logs -f mysql      # MySQL日志

# 进入容器
docker-compose exec backend bash
docker-compose exec mysql bash
```

---

## 4. 方式二：手动部署

### 4.1 安装基础环境

```bash
# Ubuntu 系统
sudo apt update

# 安装 JDK 17
sudo apt install openjdk-17-jdk -y
java -version  # 验证

# 安装 MySQL 8.0
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# 安装 Redis
sudo apt install redis-server -y
sudo systemctl start redis
sudo systemctl enable redis

# 安装 Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs -y
node -v && npm -v  # 验证

# 安装 Nginx
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 4.2 配置 MySQL

```bash
# 登录 MySQL
sudo mysql -u root -p

# 创建数据库和用户
CREATE DATABASE ai_relay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'airelay'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ai_relay.* TO 'airelay'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入数据库Schema
mysql -u airelay -p ai_relay < backend/src/main/resources/db/schema.sql
```

### 4.3 配置 Redis

```bash
# 编辑 Redis 配置
sudo vim /etc/redis/redis.conf

# 建议修改：
# bind 127.0.0.1          # 只允许本地访问
# requirepass your_redis_password  # 设置密码（可选）

# 重启 Redis
sudo systemctl restart redis
```

### 4.4 构建后端

```bash
cd backend

# 方式一：使用 Maven Wrapper（推荐）
chmod +x mvnw
./mvnw clean package -DskipTests

# 方式二：使用系统 Maven
mvn clean package -DskipTests

# JAR 包位置
ls -la target/ai-relay-*.jar
```

### 4.5 配置后端

```bash
# 创建配置目录
sudo mkdir -p /opt/airelay
sudo cp target/ai-relay-*.jar /opt/airelay/

# 创建生产配置文件
sudo vim /opt/airelay/application-prod.yml
```

`application-prod.yml` 内容：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_relay?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: airelay
    password: your_password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password    # 如果设置了密码

jwt:
  secret: your_jwt_secret_here_please_change_this
  access-token-expiration: 86400000
  refresh-token-expiration: 604800000

app:
  alipay:
    app-id: your_alipay_app_id
    private-key: your_alipay_private_key
    public-key: your_alipay_public_key
  wechat:
    app-id: your_wechat_app_id
    mch-id: your_wechat_mch_id
    api-key: your_wechat_api_key
```

### 4.6 创建后端系统服务

```bash
sudo vim /etc/systemd/system/airelay.service
```

内容：

```ini
[Unit]
Description=AI Relay Platform
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/airelay
ExecStart=/usr/bin/java -jar /opt/airelay/ai-relay-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl start airelay
sudo systemctl enable airelay

# 查看状态
sudo systemctl status airelay

# 查看日志
sudo journalctl -u airelay -f
```

### 4.7 构建前端

```bash
cd frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 部署到 Nginx
sudo cp -r dist/* /var/www/html/
```

### 4.8 配置 Nginx

```bash
sudo vim /etc/nginx/sites-available/airelay
```

内容：

```nginx
server {
    listen 80;
    server_name your-domain.com;    # 替换为你的域名或IP

    # 前端静态文件
    root /var/www/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
    gzip_min_length 1000;

    # 文件上传大小限制
    client_max_body_size 50m;

    # 前端路由（SPA）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }

    # AI中转API代理（支持SSE流式）
    location /v1/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # SSE 流式响应关键配置
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        chunked_transfer_encoding on;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/airelay /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default    # 删除默认配置
sudo nginx -t    # 测试配置
sudo systemctl reload nginx
```

---

## 5. 配置详解

### 5.1 application.yml 完整配置项

```yaml
# ==================== 服务端口 ====================
server:
  port: 8080

# ==================== 数据库 ====================
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_relay?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    hikari:
      maximum-pool-size: 20       # 最大连接数，生产建议20-50
      minimum-idle: 5             # 最小空闲连接
      idle-timeout: 600000        # 空闲超时10分钟
      max-lifetime: 1800000       # 连接最大生命周期30分钟

# ==================== Redis ====================
  data:
    redis:
      host: localhost
      port: 6379
      password:                   # Redis密码，建议设置
      lettuce:
        pool:
          max-active: 20          # 最大连接数
          max-idle: 10            # 最大空闲连接
          min-idle: 5             # 最小空闲连接

# ==================== JWT ====================
jwt:
  secret: your_base64_encoded_secret   # JWT密钥，必须修改！
  access-token-expiration: 86400000    # Access Token有效期：24小时（毫秒）
  refresh-token-expiration: 604800000  # Refresh Token有效期：7天（毫秒）

# ==================== 中转配置 ====================
app:
  relay:
    load-balance-strategy: PRIORITY    # 负载均衡策略: PRIORITY/WEIGHTED_RANDOM/ROUND_ROBIN/LEAST_LATENCY
    channel-timeout-ms: 30000          # 上游请求超时时间：30秒
    max-retries: 3                     # 最大重试次数
    rate-limit-enabled: true           # 是否启用限流

# ==================== 通知配置 ====================
  notification:
    balance-warning-threshold: 1.0     # 余额预警阈值（元）
    subscription-warning-days: 3       # 订阅到期预警天数

# ==================== 支付宝 ====================
  alipay:
    app-id:                            # 应用AppID
    private-key:                       # 应用私钥（RSA2）
    public-key:                        # 支付宝公钥
    gateway: https://openapi.alipay.com/gateway.do    # 正式网关
    # gateway: https://openapi-sandbox.dl.alipaydev.com/gateway.do  # 沙箱网关
    notify-url: https://your-domain.com/api/payment/alipay/notify   # 异步通知地址
    return-url: https://your-domain.com/billing                       # 同步跳转地址

# ==================== 微信支付 ====================
  wechat:
    app-id:                            # 应用AppID
    mch-id:                            # 商户号
    api-key:                           # APIv3密钥
    cert-path: /opt/airelay/wechat-cert/apiclient_cert.p12  # 证书路径
    notify-url: https://your-domain.com/api/payment/wechat/notify   # 异步通知地址
```

### 5.2 JWT 密钥生成

```bash
# 方法一：OpenSSL生成
openssl rand -base64 64

# 方法二：Java生成
java -e 'import java.util.UUID; println(UUID.randomUUID().toString().replace("-",""))'

# 将生成的字符串填入 jwt.secret
```

### 5.3 负载均衡策略选择

| 策略 | 适用场景 | 说明 |
|------|----------|------|
| `PRIORITY` | 通用 | 按渠道优先级选择，优先级高的优先使用 |
| `WEIGHTED_RANDOM` | 多渠道分流 | 按权重随机分配，适合多个同优先级渠道 |
| `ROUND_ROBIN` | 均匀分配 | 轮询分配，每个渠道轮流使用 |
| `LEAST_LATENCY` | 追求低延迟 | 选择响应时间最短的渠道 |

---

## 6. 支付配置

### 6.1 支付宝配置

1. 登录 [支付宝开放平台](https://open.alipay.com/)
2. 创建网页/移动应用
3. 在应用详情页获取 **AppID**
4. 设置应用公钥，获取 **支付宝公钥**
5. 生成应用私钥（RSA2）：

```bash
# 下载支付宝密钥工具
# https://opendocs.alipay.com/common/02kipk

# 或使用 OpenSSL
openssl genrsa -out app_private_key.pem 2048
openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem
```

6. 配置异步通知地址：`https://your-domain.com/api/payment/alipay/notify`

### 6.2 微信支付配置

1. 登录 [微信支付商户平台](https://pay.weixin.qq.com/)
2. 获取 **商户号 (mch-id)**
3. 在 API安全 页面设置 **APIv3密钥**
4. 下载 **API证书** (apiclient_cert.p12)
5. 将证书放到服务器：`/opt/airelay/wechat-cert/`
6. 配置异步通知地址：`https://your-domain.com/api/payment/wechat/notify`

### 6.3 暂不配置支付

如果暂时不需要支付功能，可以跳过支付配置，系统其他功能不受影响。用户仍可通过管理员手动赠送余额来使用服务：

```bash
# 管理员在后台 > 用户管理 > 赠送余额
# 或通过API：
curl -X POST http://localhost:8080/api/admin/billing/gift \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"userId": 2, "amount": 100.00, "description": "初始赠送"}'
```

---

## 7. Nginx 配置

### 7.1 关键配置说明

| 配置项 | 说明 |
|--------|------|
| `proxy_buffering off` | **必须**，SSE流式响应需要关闭缓冲 |
| `proxy_read_timeout 300s` | AI模型响应可能较慢，需要较长超时 |
| `chunked_transfer_encoding on` | 支持分块传输 |
| `proxy_http_version 1.1` | SSE需要HTTP/1.1 |
| `client_max_body_size 50m` | 允许较大请求体 |

### 7.2 性能优化

```nginx
# 在 http 块中添加
client_body_buffer_size 128k;
client_header_buffer_size 4k;
large_client_header_buffers 4 8k;
output_buffers 1 32k;
postpone_output 1460;

# 开启高效文件传输
sendfile on;
tcp_nopush on;
tcp_nodelay on;

# 保持连接
keepalive_timeout 65;
```

---

## 8. HTTPS 配置

### 8.1 使用 Let's Encrypt 免费证书

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx -y

# 申请证书（替换域名）
sudo certbot --nginx -d your-domain.com

# 自动续期（Certbot已自动配置）
sudo certbot renew --dry-run
```

### 8.2 手动配置证书

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-domain.pem;
    ssl_certificate_key /etc/ssl/private/your-domain.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # ... 其他配置同上
}

# HTTP 跳转 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

---

## 9. 数据库维护

### 9.1 备份

```bash
# 手动备份
mysqldump -u root -p ai_relay > backup_$(date +%Y%m%d_%H%M%S).sql

# 定时备份（crontab）
crontab -e
# 每天凌晨3点备份
0 3 * * * mysqldump -u root -pyour_password ai_relay > /opt/backups/ai_relay_$(date +\%Y\%m\%d).sql
```

### 9.2 恢复

```bash
mysql -u root -p ai_relay < backup_20260515_030000.sql
```

### 9.3 清理日志数据

```sql
-- 清理30天前的请求日志（释放空间）
DELETE FROM request_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 清理已过期的用户优惠券
UPDATE user_coupon SET status = 'expired'
WHERE status = 'unused' AND coupon_id IN (
  SELECT id FROM coupon WHERE end_time < NOW()
);
```

---

## 10. 常见问题

### Q1: Docker 启动后后端连接 MySQL 失败

**原因**：MySQL 还没完全启动，后端就尝试连接了。

**解决**：
```bash
# 查看MySQL状态
docker-compose logs mysql

# 等MySQL完全启动后重启后端
docker-compose restart backend
```

### Q2: 前端页面空白

**原因**：Nginx 没有正确配置 SPA 路由。

**解决**：确保 Nginx 配置中有：
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Q3: SSE 流式响应不工作

**原因**：Nginx 默认会缓冲代理响应。

**解决**：在 `/v1/` location 中添加：
```nginx
proxy_buffering off;
proxy_cache off;
chunked_transfer_encoding on;
```

### Q4: 支付回调不生效

**原因**：支付宝/微信无法访问你的回调地址。

**解决**：
1. 确保服务器公网可访问
2. 确保回调URL配置正确（HTTPS）
3. 检查防火墙是否放行80/443端口
4. 查看后端日志：`docker-compose logs backend | grep notify`

### Q5: JWT Token 报错 "Secret key too short"

**原因**：JWT 密钥太短。

**解决**：
```bash
# 重新生成密钥
openssl rand -base64 64
# 将新密钥填入 .env 的 JWT_SECRET
# 重启后端
docker-compose restart backend
```

### Q6: 如何修改默认管理员密码

```bash
# 登录后端容器
docker-compose exec backend bash

# 或直接调用API修改密码
curl -X PUT http://localhost:8080/api/user/password \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword": "admin123", "newPassword": "your_new_password"}'
```

### Q7: 如何添加上游渠道

1. 以管理员登录
2. 进入 管理后台 > 渠道管理
3. 点击 "添加渠道"
4. 填写信息：
   - **名称**：如 "OpenAI 官方"
   - **类型**：选择 openai / claude / gemini 等
   - **Base URL**：如 `https://api.openai.com`
   - **API Key**：你的上游API密钥
   - **支持的模型**：如 `["gpt-4","gpt-3.5-turbo","text-embedding-ada-002"]`
5. 点击 "测试" 验证渠道可用性
6. 保存

### Q8: 如何设置模型定价

1. 进入 管理后台 > 模型定价
2. 添加模型价格：
   - **模型ID**：如 `gpt-4`
   - **显示名称**：如 "GPT-4"
   - **输入价格**：每1K Token价格（元），如 `0.03`
   - **输出价格**：每1K Token价格（元），如 `0.06`
   - **加价倍率**：如 `1.5` 表示在上游价格基础上加价50%

### Q9: 服务器端口规划

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | Nginx | HTTP入口 |
| 443 | Nginx | HTTPS入口 |
| 8080 | Spring Boot | 后端服务（仅内网） |
| 3306 | MySQL | 数据库（仅内网） |
| 6379 | Redis | 缓存（仅内网） |

> ⚠️ 8080/3306/6379 端口**不要**对外暴露，只通过 Nginx 代理访问。

### Q10: 如何更新代码

```bash
cd API

# 拉取最新代码
git pull origin main

# Docker 方式：重新构建
docker-compose up -d --build

# 手动方式：重新构建后端
cd backend
./mvnw clean package -DskipTests
sudo cp target/ai-relay-*.jar /opt/airelay/
sudo systemctl restart airelay

# 重新构建前端
cd frontend
npm run build
sudo cp -r dist/* /var/www/html/
```
