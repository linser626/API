# 📖 AI Relay 系统操作文档

本文档详细说明 AI Relay 大模型中转平台的各项功能操作方法。

---

## 目录

- [1. 用户端操作](#1-用户端操作)
  - [1.1 注册与登录](#11-注册与登录)
  - [1.2 仪表盘](#12-仪表盘)
  - [1.3 API Key 管理](#13-api-key-管理)
  - [1.4 订阅方案](#14-订阅方案)
  - [1.5 充值与账单](#15-充值与账单)
  - [1.6 用量监控](#16-用量监控)
  - [1.7 优惠券](#17-优惠券)
  - [1.8 通知中心](#18-通知中心)
- [2. API 接入指南](#2-api-接入指南)
  - [2.1 快速开始](#21-快速开始)
  - [2.2 Chat Completion](#22-chat-completion)
  - [2.3 流式输出](#23-流式输出)
  - [2.4 Embeddings](#24-embeddings)
  - [2.5 模型列表](#25-模型列表)
  - [2.6 Python SDK 接入](#26-python-sdk-接入)
  - [2.7 Node.js SDK 接入](#27-nodejs-sdk-接入)
  - [2.8 其他工具接入](#28-其他工具接入)
- [3. 管理端操作](#3-管理端操作)
  - [3.1 管理面板](#31-管理面板)
  - [3.2 用户管理](#32-用户管理)
  - [3.3 渠道管理](#33-渠道管理)
  - [3.4 模型定价](#34-模型定价)
  - [3.5 订单管理](#35-订单管理)
  - [3.6 优惠券管理](#36-优惠券管理)
  - [3.7 系统监控](#37-系统监控)
- [4. 计费说明](#4-计费说明)
- [5. 常见问题](#5-常见问题)

---

## 1. 用户端操作

### 1.1 注册与登录

#### 注册新账号

1. 访问平台首页，点击 **"注册"** 链接
2. 填写注册信息：
   - **用户名**：3-50个字符，支持字母数字下划线
   - **邮箱**：有效的邮箱地址，用于找回密码
   - **密码**：6-50个字符，建议包含字母和数字
   - **确认密码**：再次输入密码
3. 点击 **"注册"** 按钮
4. 注册成功后自动登录，跳转到仪表盘
5. 新用户自动获得 **免费版** 订阅套餐

#### 登录

1. 访问平台首页，输入用户名和密码
2. 可勾选 **"记住我"** 延长登录有效期
3. 登录成功后跳转到仪表盘

#### 修改密码

1. 点击右上角头像 > **修改密码**
2. 输入旧密码和新密码
3. 点击确认保存

### 1.2 仪表盘

仪表盘是用户的主页面，展示以下信息：

| 区域 | 内容 |
|------|------|
| 统计卡片 | 账户余额、已用Token、本月请求数、当前套餐 |
| 用量趋势图 | 最近7天的Token用量和请求数趋势 |
| 快速入门 | 3步接入指南 |
| API接入指南 | cURL/Python/Node.js 代码示例 |
| 快捷操作 | 充值、创建API Key、查看文档 |

### 1.3 API Key 管理

API Key 是调用中转API的凭证，每个Key以 `sk-` 开头。

#### 创建 API Key

1. 进入 **API密钥** 页面
2. 点击 **"创建密钥"** 按钮
3. 输入密钥名称（如 "我的项目-开发环境"）
4. 点击确认
5. **⚠️ 重要**：创建后立即复制完整的Key，之后只能看到脱敏版本

```
完整Key（仅创建时显示）：sk-abc123def456ghi789jkl012mno345pqr678
脱敏显示：sk-abc1********************345pqr678
```

#### API Key 限制

| 套餐 | 最大Key数量 | RPM限制 | TPM限制 |
|------|------------|---------|---------|
| 免费版 | 1 | 10 | 10,000 |
| 基础版 | 5 | 60 | 100,000 |
| 专业版 | 20 | 120 | 200,000 |
| 企业版 | 100 | 300 | 500,000 |

#### 管理 API Key

- **复制Key**：点击复制按钮，将完整Key复制到剪贴板
- **编辑Key**：修改名称和速率限制
- **吊销Key**：删除Key，该Key立即失效（不可恢复）

### 1.4 订阅方案

#### 查看套餐

1. 进入 **订阅方案** 页面
2. 查看当前订阅状态和所有可用套餐
3. 每个套餐卡片展示：价格、Token额度、速率限制、功能特性

#### 订阅套餐

1. 选择目标套餐，点击 **"订阅"** 按钮
2. 在支付对话框中：
   - 选择支付方式（支付宝/微信）
   - 可选：输入优惠券码并点击"验证"
   - 确认支付金额
3. 点击 **"确认支付"**
   - 支付宝：跳转到支付宝支付页面
   - 微信：显示二维码，扫码支付
4. 支付成功后套餐立即生效

#### 取消自动续费

1. 在订阅方案页面，点击 **"取消自动续费"**
2. 当前套餐到期后不会自动续费
3. 到期后降级为免费版

### 1.5 充值与账单

#### 余额充值

1. 进入 **账单** 页面
2. 点击 **"充值"** 按钮
3. 选择充值金额：
   - 快捷金额：10 / 50 / 100 / 200 / 500 元
   - 或输入自定义金额
4. 选择支付方式
5. 可选：输入优惠券码
6. 确认支付

#### 查看交易记录

1. 在账单页面，查看交易记录列表
2. 支持筛选：
   - **交易类型**：充值/消费/退款/赠送/优惠券
   - **时间范围**：选择起止日期
3. 每条记录显示：类型、金额、余额变动、描述、时间

#### 余额说明

- 余额用于支付API调用费用
- 每次API调用根据Token使用量实时扣费
- 余额不足时API调用会返回错误
- 建议设置余额预警（余额 < 1元时系统自动通知）

### 1.6 用量监控

#### 查看用量统计

1. 进入 **用量监控** 页面
2. 选择时间范围（今日/7天/30天/自定义）
3. 查看统计数据：
   - 总请求数、成功/失败数
   - 总Token用量（输入/输出）
   - 总费用、平均延迟

#### 用量趋势图

- 展示选定时间范围内的每日用量趋势
- 包括：请求数、Token数、费用

#### 模型使用分布

- 展示各模型的调用次数、Token用量、费用
- 帮助了解哪些模型使用最多

### 1.7 优惠券

#### 兑换优惠券

1. 进入 **优惠券** 页面
2. 在输入框中输入优惠券码
3. 点击 **"兑换"** 按钮
4. 兑换成功后优惠券出现在"我的优惠券"列表中

#### 查看我的优惠券

| 状态 | 说明 |
|------|------|
| 🟢 未使用 | 可以在充值或订阅时使用 |
| 🔵 已使用 | 已在某笔订单中使用 |
| 🔴 已过期 | 超过有效期，不可使用 |

#### 使用优惠券

- 在充值或订阅支付时，输入优惠券码
- 系统自动验证并计算折扣金额
- 每张优惠券每个用户只能使用一次

### 1.8 通知中心

- 点击顶部导航栏的 🔔 铃铛图标查看通知
- 系统自动发送以下通知：
  - 余额不足预警（余额 < 1元）
  - 订阅即将到期（3天内）
  - 支付成功/失败
  - 优惠券到账
- 点击通知可标记为已读
- 点击 "全部已读" 批量标记

---

## 2. API 接入指南

### 2.1 快速开始

AI Relay 的 API 完全兼容 OpenAI 格式，只需修改 Base URL 和 API Key 即可接入。

| 参数 | 值 |
|------|-----|
| Base URL | `https://your-domain.com/v1` |
| API Key | 你的API Key（`sk-` 开头） |
| 认证方式 | `Authorization: Bearer sk-your-api-key` |

### 2.2 Chat Completion

```bash
curl https://your-domain.com/v1/chat/completions \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Hello!"}
    ],
    "temperature": 0.7,
    "max_tokens": 1000
  }'
```

响应格式（与OpenAI完全一致）：

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "model": "gpt-4",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Hello! How can I help you today?"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 10,
    "total_tokens": 30
  }
}
```

### 2.3 流式输出

```bash
curl https://your-domain.com/v1/chat/completions \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "写一首诗"}],
    "stream": true
  }'
```

响应为 SSE 格式：

```
data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"春"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"风"},"finish_reason":null}]}

data: [DONE]
```

### 2.4 Embeddings

```bash
curl https://your-domain.com/v1/embeddings \
  -H "Authorization: Bearer sk-your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-ada-002",
    "input": "Hello world"
  }'
```

### 2.5 模型列表

```bash
curl https://your-domain.com/v1/models \
  -H "Authorization: Bearer sk-your-api-key"
```

### 2.6 Python SDK 接入

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-your-api-key",
    base_url="https://your-domain.com/v1"
)

# 普通对话
response = client.chat.completions.create(
    model="gpt-4",
    messages=[
        {"role": "user", "content": "Hello!"}
    ]
)
print(response.choices[0].message.content)

# 流式输出
stream = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "写一首诗"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")

# Embeddings
embedding = client.embeddings.create(
    model="text-embedding-ada-002",
    input="Hello world"
)
print(embedding.data[0].embedding[:5])
```

### 2.7 Node.js SDK 接入

```javascript
import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'sk-your-api-key',
  baseURL: 'https://your-domain.com/v1',
});

// 普通对话
const response = await client.chat.completions.create({
  model: 'gpt-4',
  messages: [{ role: 'user', content: 'Hello!' }],
});
console.log(response.choices[0].message.content);

// 流式输出
const stream = await client.chat.completions.create({
  model: 'gpt-4',
  messages: [{ role: 'user', content: '写一首诗' }],
  stream: true,
});
for await (const chunk of stream) {
  process.stdout.write(chunk.choices[0]?.delta?.content || '');
}
```

### 2.8 其他工具接入

#### LangChain

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="gpt-4",
    openai_api_key="sk-your-api-key",
    openai_api_base="https://your-domain.com/v1"
)
```

#### Cursor / VS Code 插件

1. 打开设置 > AI配置
2. API Base URL: `https://your-domain.com/v1`
3. API Key: `sk-your-api-key`

#### ChatBox / NextChat 等客户端

1. 设置 > API地址: `https://your-domain.com`
2. API Key: `sk-your-api-key`
3. 选择模型即可使用

---

## 3. 管理端操作

管理员通过页面右上角头像进入管理后台，或直接访问 `/admin` 路径。

### 3.1 管理面板

管理面板展示平台核心指标：

| 指标 | 说明 |
|------|------|
| 总用户数 | 注册用户总数 |
| 活跃用户 | 今日有API调用的用户数 |
| 今日请求 | 今日API请求总数 |
| 今日收入 | 今日支付订单总额 |
| 总收入 | 平台累计收入 |
| 活跃渠道 | 状态正常的上游渠道数 |
| 活跃订阅 | 当前有效的订阅数 |

同时展示收入趋势图和用户增长图。

### 3.2 用户管理

#### 用户列表

- 支持按用户名/邮箱搜索
- 展示：ID、用户名、邮箱、角色、余额、状态、注册时间
- 分页浏览

#### 操作

| 操作 | 说明 |
|------|------|
| 启用/禁用 | 切换用户状态，禁用后用户无法登录和调用API |
| 重置密码 | 为用户设置新密码 |
| 赠送余额 | 向用户账户添加余额（不经过支付） |

#### 赠送余额

1. 点击用户行的 **"赠送余额"** 按钮
2. 输入赠送金额和说明
3. 确认后余额立即到账

### 3.3 渠道管理

渠道是上游API供应商的接入配置，是中转服务的核心。

#### 添加渠道

| 字段 | 说明 | 示例 |
|------|------|------|
| 名称 | 渠道显示名称 | OpenAI 官方 |
| 类型 | 供应商类型 | openai / claude / gemini / qwen / zhipu / deepseek / wenxin |
| Base URL | API基础地址 | `https://api.openai.com` |
| API Key | 上游密钥 | `sk-proj-xxxxx` |
| 支持的模型 | JSON数组格式 | `["gpt-4","gpt-3.5-turbo"]` |
| 优先级 | 数字越大越优先 | 10 |
| 权重 | 负载均衡权重 | 1-100 |
| 超时时间 | 请求超时（毫秒） | 30000 |
| 最大重试 | 失败重试次数 | 3 |

#### 各类型渠道配置示例

**OpenAI**
```
类型: openai
Base URL: https://api.openai.com
API Key: sk-proj-xxxxxxxxxxxxx
模型: ["gpt-4","gpt-4-turbo","gpt-3.5-turbo","text-embedding-ada-002","dall-e-3"]
```

**Claude (Anthropic)**
```
类型: claude
Base URL: https://api.anthropic.com
API Key: sk-ant-xxxxxxxxxxxxx
模型: ["claude-3-opus-20240229","claude-3-sonnet-20240229","claude-3-haiku-20240307"]
```

**Gemini (Google)**
```
类型: gemini
Base URL: https://generativelanguage.googleapis.com
API Key: AIzaSyxxxxxxxxxxxxx
模型: ["gemini-pro","gemini-1.5-pro","gemini-1.5-flash"]
```

**通义千问 (阿里云)**
```
类型: qwen
Base URL: https://dashscope.aliyuncs.com/compatible-mode
API Key: sk-xxxxxxxxxxxxx
模型: ["qwen-turbo","qwen-plus","qwen-max","text-embedding-v1"]
```

**智谱AI**
```
类型: zhipu
Base URL: https://open.bigmodel.cn/api/paas
API Key: xxxxxxxx.xxxxxxxx
模型: ["glm-4","glm-3-turbo","embedding-2"]
```

**DeepSeek**
```
类型: deepseek
Base URL: https://api.deepseek.com
API Key: sk-xxxxxxxxxxxxx
模型: ["deepseek-chat","deepseek-coder"]
```

**其他中转站**
```
类型: openai    # 如果兼容OpenAI格式就选openai
Base URL: https://other-relay.com   # 中转站地址
API Key: sk-xxxxxxxxxxxxx           # 中转站提供的Key
模型: ["gpt-4","claude-3-opus"]     # 中转站支持的模型
```

#### 渠道操作

| 操作 | 说明 |
|------|------|
| 测试 | 发送测试请求验证渠道可用性 |
| 启用/禁用 | 切换渠道状态 |
| 编辑 | 修改渠道配置 |
| 删除 | 删除渠道（软删除） |

#### 渠道健康

- 系统每5分钟自动检测所有活跃渠道
- 检测失败自动标记为异常状态
- 渠道列表显示：成功率、平均响应时间、总请求数

### 3.4 模型定价

模型定价决定了用户调用每个模型的费用。

#### 添加模型定价

| 字段 | 说明 | 示例 |
|------|------|------|
| 模型ID | 模型唯一标识 | `gpt-4` |
| 显示名称 | 用户看到的名称 | `GPT-4` |
| 输入价格 | 每1K输入Token价格（元） | `0.03` |
| 输出价格 | 每1K输出Token价格（元） | `0.06` |
| 加价倍率 | 在上游价格基础上的加价倍数 | `1.5`（加价50%） |

#### 定价计算示例

```
用户调用 gpt-4，输入500 Token，输出200 Token
输入费用 = 500 / 1000 × 0.03 = 0.015 元
输出费用 = 200 / 1000 × 0.06 = 0.012 元
总费用 = (0.015 + 0.012) × 1.5 = 0.0405 元
```

### 3.5 订单管理

- 查看所有用户订单
- 支持按状态筛选：待支付/已支付/已失败/已退款
- 支持按支付方式筛选：支付宝/微信
- 支持时间范围筛选
- 可对已支付订单执行退款操作

### 3.6 优惠券管理

#### 创建优惠券

| 字段 | 说明 |
|------|------|
| 优惠券码 | 用户兑换时输入的码，如 `WELCOME2024` |
| 名称 | 优惠券显示名称 |
| 类型 | 固定金额 / 百分比折扣 |
| 面值 | 固定金额（如10元）或百分比（如20表示8折） |
| 最低消费 | 使用该券的最低订单金额 |
| 最大优惠 | 百分比类型券的最大优惠金额（封顶） |
| 总量 | 发放总量，-1表示不限量 |
| 每人限领 | 每个用户最多领取次数 |
| 有效期 | 起止时间 |

#### 优惠券类型示例

**固定金额券**：满50减10
```
类型: 固定金额
面值: 10
最低消费: 50
```

**百分比折扣券**：全场8折，最多优惠50元
```
类型: 百分比折扣
面值: 20（表示减20%，即8折）
最低消费: 0
最大优惠: 50
```

### 3.7 系统监控

#### 渠道性能

| 指标 | 说明 |
|------|------|
| 总请求数 | 渠道累计处理请求数 |
| 失败请求数 | 请求失败次数 |
| 成功率 | 成功请求占比 |
| 平均延迟 | 平均响应时间（毫秒） |

#### 错误日志

- 查看最近的API调用错误
- 包含：模型、错误信息、时间、用户、渠道
- 用于排查问题

---

## 4. 计费说明

### 4.1 计费方式

AI Relay 采用 **预充值 + 按量计费** 模式：

1. 用户先充值余额
2. 每次API调用根据实际Token使用量扣费
3. 余额不足时API调用返回错误

### 4.2 Token 计费

- **输入Token**：请求中的提示词（prompt）消耗的Token
- **输出Token**：模型生成的回复消耗的Token
- 输出Token的价格通常是输入Token的2-3倍

### 4.3 费用计算公式

```
单次调用费用 = (输入Token数 / 1000 × 输入单价 + 输出Token数 / 1000 × 输出单价) × 加价倍率
```

### 4.4 套餐对比

| 功能 | 免费版 | 基础版 ¥29.9/月 | 专业版 ¥99.9/月 | 企业版 ¥299.9/月 |
|------|--------|-----------------|-----------------|------------------|
| Token额度 | 100K | 5M | 无限 | 无限 |
| RPM限制 | 10 | 60 | 120 | 300 |
| TPM限制 | 10K | 100K | 200K | 500K |
| API Key数 | 1 | 5 | 20 | 100 |
| 模型访问 | 基础模型 | 全部模型 | 全部模型 | 全部模型 |
| 渠道优先级 | 普通 | 优先 | 优先 | 最高 |
| 客服支持 | - | - | 专属客服 | 专属客服 |

### 4.5 余额不足处理

- 余额 < 1元时，系统发送通知提醒充值
- 余额为0时，API调用返回 `1001 INSUFFICIENT_BALANCE` 错误
- 建议保持余额充足，避免影响业务

---

## 5. 常见问题

### Q1: API调用返回 401 Unauthorized

**原因**：API Key无效或已过期。

**解决**：
1. 检查API Key是否正确（`sk-` 开头）
2. 检查Key是否已被吊销
3. 检查Key是否已过期
4. 检查请求头格式：`Authorization: Bearer sk-your-key`

### Q2: API调用返回 1002 API_KEY_INVALID

**原因**：API Key不存在或已被禁用。

**解决**：在 API密钥 页面创建新的Key。

### Q3: API调用返回 1004 RATE_LIMIT_EXCEEDED

**原因**：请求频率超过套餐限制。

**解决**：
1. 降低请求频率
2. 升级到更高级别的套餐
3. 查看当前套餐的RPM/TPM限制

### Q4: API调用返回 1005 SUBSCRIPTION_EXPIRED

**原因**：订阅已过期。

**解决**：续费订阅或重新订阅。

### Q5: API调用返回 1001 INSUFFICIENT_BALANCE

**原因**：账户余额不足。

**解决**：在账单页面充值。

### Q6: 流式输出中断

**原因**：网络超时或渠道异常。

**解决**：
1. 检查网络连接
2. 系统会自动尝试其他渠道（故障转移）
3. 如果持续中断，联系管理员检查渠道状态

### Q7: 模型返回 "model not found"

**原因**：请求的模型没有可用的上游渠道。

**解决**：
1. 检查模型名称是否正确（如 `gpt-4` 不是 `gpt4`）
2. 联系管理员确认该模型是否有可用渠道
3. 通过 `/v1/models` 接口查看可用模型列表

### Q8: 如何查看我的Token用量

1. 进入 **用量监控** 页面
2. 选择时间范围
3. 查看Token用量统计和模型分布

### Q9: 如何更换API Key

1. 先创建新的API Key
2. 在应用中替换为新Key
3. 确认新Key正常工作后，吊销旧Key

### Q10: 支付后余额没有到账

**可能原因**：
1. 支付回调延迟（通常1-3分钟）
2. 网络问题导致回调失败

**解决**：
1. 等待3分钟后刷新页面
2. 如果仍未到账，联系管理员查看订单状态
3. 管理员可在后台手动确认订单
