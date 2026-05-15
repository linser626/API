<template>
  <div class="docs-page">
    <div class="docs-sidebar">
      <div class="sidebar-title">API文档</div>
      <ul class="nav-list">
        <li><a :class="{ active: activeSection === 'quickstart' }" @click="scrollTo('quickstart')">快速开始</a></li>
        <li><a :class="{ active: activeSection === 'chat' }" @click="scrollTo('chat')">Chat Completion</a></li>
        <li><a :class="{ active: activeSection === 'embeddings' }" @click="scrollTo('embeddings')">Embeddings</a></li>
        <li><a :class="{ active: activeSection === 'models' }" @click="scrollTo('models')">Models</a></li>
        <li><a :class="{ active: activeSection === 'errors' }" @click="scrollTo('errors')">错误码</a></li>
        <li><a :class="{ active: activeSection === 'sdk' }" @click="scrollTo('sdk')">SDK集成</a></li>
      </ul>
    </div>

    <div class="docs-content" @scroll="handleScroll">
      <section id="quickstart" class="doc-section">
        <h2>快速开始</h2>

        <el-card shadow="never" class="doc-card">
          <h3>Base URL</h3>
          <div class="code-block">
            <pre>https://your-domain.com</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.baseUrl)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>认证方式</h3>
          <p>所有API请求都需要在Header中携带API密钥：</p>
          <div class="code-block">
            <pre>Authorization: Bearer sk-your-api-key</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.auth)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
          <p>您可以在 <router-link to="/apikeys">API密钥</router-link> 页面创建和管理密钥。</p>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>快速测试</h3>
          <p>使用curl快速测试API：</p>
          <div class="code-block">
            <pre>{{ codeExamples.quickTest }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.quickTest)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>
      </section>

      <section id="chat" class="doc-section">
        <h2>Chat Completion API</h2>

        <el-card shadow="never" class="doc-card">
          <h3>接口地址</h3>
          <div class="endpoint">
            <el-tag type="success" size="large">POST</el-tag>
            <code class="endpoint-url">/v1/chat/completions</code>
          </div>
          <p>兼容OpenAI Chat Completion API格式，支持流式和非流式响应。</p>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>请求参数</h3>
          <el-table :data="chatParams" stripe border size="small">
            <el-table-column prop="name" label="参数名" width="160" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="required" label="必填" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" />
          </el-table>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>请求示例</h3>
          <el-tabs>
            <el-tab-pane label="curl">
              <div class="code-block">
                <pre>{{ codeExamples.chatCurl }}</pre>
                <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.chatCurl)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="Python">
              <div class="code-block">
                <pre>{{ codeExamples.chatPython }}</pre>
                <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.chatPython)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="Node.js">
              <div class="code-block">
                <pre>{{ codeExamples.chatNode }}</pre>
                <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.chatNode)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>响应格式</h3>
          <div class="code-block">
            <pre>{{ codeExamples.chatResponse }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.chatResponse)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>流式响应</h3>
          <p>设置 <code>stream: true</code> 即可启用流式响应，响应格式为SSE：</p>
          <div class="code-block">
            <pre>{{ codeExamples.sseResponse }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.sseResponse)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>
      </section>

      <section id="embeddings" class="doc-section">
        <h2>Embeddings API</h2>

        <el-card shadow="never" class="doc-card">
          <h3>接口地址</h3>
          <div class="endpoint">
            <el-tag type="success" size="large">POST</el-tag>
            <code class="endpoint-url">/v1/embeddings</code>
          </div>
          <p>获取文本的向量表示，兼容OpenAI Embeddings API格式。</p>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>请求示例</h3>
          <div class="code-block">
            <pre>{{ codeExamples.embeddingCurl }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.embeddingCurl)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>响应格式</h3>
          <div class="code-block">
            <pre>{{ codeExamples.embeddingResponse }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.embeddingResponse)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>
      </section>

      <section id="models" class="doc-section">
        <h2>Models API</h2>

        <el-card shadow="never" class="doc-card">
          <h3>接口地址</h3>
          <div class="endpoint">
            <el-tag type="primary" size="large">GET</el-tag>
            <code class="endpoint-url">/v1/models</code>
          </div>
          <p>获取当前可用的模型列表。</p>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>请求示例</h3>
          <div class="code-block">
            <pre>{{ codeExamples.modelsCurl }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.modelsCurl)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>响应格式</h3>
          <div class="code-block">
            <pre>{{ codeExamples.modelsResponse }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.modelsResponse)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>
      </section>

      <section id="errors" class="doc-section">
        <h2>错误码</h2>

        <el-card shadow="never" class="doc-card">
          <h3>HTTP状态码</h3>
          <el-table :data="errorCodes" stripe border size="small">
            <el-table-column prop="code" label="状态码" width="100" />
            <el-table-column prop="message" label="说明" min-width="300" />
          </el-table>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>业务错误码</h3>
          <el-table :data="bizErrorCodes" stripe border size="small">
            <el-table-column prop="code" label="错误码" width="100" />
            <el-table-column prop="message" label="说明" min-width="300" />
          </el-table>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>错误响应格式</h3>
          <div class="code-block">
            <pre>{{ codeExamples.errorResponse }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.errorResponse)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>
      </section>

      <section id="sdk" class="doc-section">
        <h2>SDK集成</h2>

        <el-card shadow="never" class="doc-card">
          <h3>Python (openai)</h3>
          <div class="code-block">
            <pre>pip install openai</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode('pip install openai')">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
          <div class="code-block">
            <pre>{{ codeExamples.sdkPython }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.sdkPython)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>Node.js (openai)</h3>
          <div class="code-block">
            <pre>npm install openai</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode('npm install openai')">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
          <div class="code-block">
            <pre>{{ codeExamples.sdkNode }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.sdkNode)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>LangChain</h3>
          <div class="code-block">
            <pre>pip install langchain-openai</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode('pip install langchain-openai')">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
          <div class="code-block">
            <pre>{{ codeExamples.sdkLangchain }}</pre>
            <el-button class="copy-btn" link size="small" @click="copyCode(codeExamples.sdkLangchain)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="doc-card">
          <h3>Cursor / ChatBox 配置</h3>
          <el-table :data="toolConfigs" stripe border size="small">
            <el-table-column prop="field" label="配置项" width="160" />
            <el-table-column prop="value" label="值" min-width="300" />
          </el-table>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

const activeSection = ref('quickstart')

const codeExamples = {
  baseUrl: 'https://your-domain.com',
  auth: 'Authorization: Bearer sk-your-api-key',
  quickTest: `curl https://your-domain.com/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer sk-your-api-key" \\
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "你好"}]
  }'`,
  chatCurl: `curl https://your-domain.com/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer sk-your-api-key" \\
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {"role": "system", "content": "你是一个有帮助的助手"},
      {"role": "user", "content": "请介绍一下你自己"}
    ],
    "temperature": 0.7,
    "max_tokens": 1024
  }'`,
  chatPython: `from openai import OpenAI

client = OpenAI(
    api_key="sk-your-api-key",
    base_url="https://your-domain.com/v1"
)

response = client.chat.completions.create(
    model="gpt-3.5-turbo",
    messages=[
        {"role": "system", "content": "你是一个有帮助的助手"},
        {"role": "user", "content": "请介绍一下你自己"}
    ],
    temperature=0.7,
    max_tokens=1024
)

print(response.choices[0].message.content)`,
  chatNode: `import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'sk-your-api-key',
  baseURL: 'https://your-domain.com/v1'
});

const response = await client.chat.completions.create({
  model: 'gpt-3.5-turbo',
  messages: [
    { role: 'system', content: '你是一个有帮助的助手' },
    { role: 'user', content: '请介绍一下你自己' }
  ],
  temperature: 0.7,
  max_tokens: 1024
});

console.log(response.choices[0].message.content);`,
  chatResponse: `{
  "id": "chatcmpl-abc123",
  "object": "chat.completion",
  "created": 1677858242,
  "model": "gpt-3.5-turbo",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "你好！我是一个AI助手..."
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 25,
    "completion_tokens": 50,
    "total_tokens": 75
  }
}`,
  sseResponse: `data: {"id":"chatcmpl-abc","choices":[{"delta":{"content":"你"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc","choices":[{"delta":{"content":"好"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc","choices":[{"delta":{},"finish_reason":"stop"}]}

data: [DONE]`,
  embeddingCurl: `curl https://your-domain.com/v1/embeddings \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer sk-your-api-key" \\
  -d '{
    "model": "text-embedding-ada-002",
    "input": "这是一段需要获取向量的文本"
  }'`,
  embeddingResponse: `{
  "object": "list",
  "data": [{
    "object": "embedding",
    "index": 0,
    "embedding": [0.0023064255, -0.009327292, ...]
  }],
  "model": "text-embedding-ada-002",
  "usage": {
    "prompt_tokens": 12,
    "total_tokens": 12
  }
}`,
  modelsCurl: `curl https://your-domain.com/v1/models \\
  -H "Authorization: Bearer sk-your-api-key"`,
  modelsResponse: `{
  "object": "list",
  "data": [
    {
      "id": "gpt-3.5-turbo",
      "object": "model",
      "created": 1677858242,
      "owned_by": "openai"
    },
    {
      "id": "gpt-4",
      "object": "model",
      "created": 1677858242,
      "owned_by": "openai"
    }
  ]
}`,
  errorResponse: `{
  "error": {
    "message": "Invalid API key provided",
    "type": "invalid_request_error",
    "code": "invalid_api_key"
  }
}`,
  sdkPython: `from openai import OpenAI

client = OpenAI(
    api_key="sk-your-api-key",
    base_url="https://your-domain.com/v1"
)

# 非流式
response = client.chat.completions.create(
    model="gpt-3.5-turbo",
    messages=[{"role": "user", "content": "你好"}]
)
print(response.choices[0].message.content)

# 流式
stream = client.chat.completions.create(
    model="gpt-3.5-turbo",
    messages=[{"role": "user", "content": "你好"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")`,
  sdkNode: `import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'sk-your-api-key',
  baseURL: 'https://your-domain.com/v1'
});

// 非流式
const response = await client.chat.completions.create({
  model: 'gpt-3.5-turbo',
  messages: [{ role: 'user', content: '你好' }]
});
console.log(response.choices[0].message.content);

// 流式
const stream = await client.chat.completions.create({
  model: 'gpt-3.5-turbo',
  messages: [{ role: 'user', content: '你好' }],
  stream: true
});
for await (const chunk of stream) {
  process.stdout.write(chunk.choices[0]?.delta?.content || '');
}`,
  sdkLangchain: `from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    api_key="sk-your-api-key",
    base_url="https://your-domain.com/v1",
    model="gpt-3.5-turbo"
)

response = llm.invoke("你好，请介绍一下你自己")
print(response.content)`
}

const chatParams = ref([
  { name: 'model', type: 'string', required: true, description: '模型ID，如 gpt-3.5-turbo、gpt-4、claude-3-sonnet 等' },
  { name: 'messages', type: 'array', required: true, description: '消息数组，每条消息包含 role（system/user/assistant）和 content' },
  { name: 'temperature', type: 'number', required: false, description: '生成温度，0-2之间，值越高输出越随机，默认1' },
  { name: 'max_tokens', type: 'integer', required: false, description: '最大生成token数，默认根据模型自动设置' },
  { name: 'stream', type: 'boolean', required: false, description: '是否启用流式输出，默认false' },
  { name: 'top_p', type: 'number', required: false, description: '核采样参数，0-1之间，默认1' },
  { name: 'frequency_penalty', type: 'number', required: false, description: '频率惩罚，-2到2之间，默认0' },
  { name: 'presence_penalty', type: 'number', required: false, description: '存在惩罚，-2到2之间，默认0' },
  { name: 'stop', type: 'array', required: false, description: '停止生成的字符串列表' },
  { name: 'n', type: 'integer', required: false, description: '生成的回复数量，默认1' }
])

const errorCodes = ref([
  { code: '200', message: '请求成功' },
  { code: '400', message: '请求参数错误' },
  { code: '401', message: '未授权，API密钥无效或缺失' },
  { code: '403', message: '禁止访问，权限不足' },
  { code: '404', message: '请求的资源不存在' },
  { code: '429', message: '请求频率超限，请稍后重试' },
  { code: '500', message: '服务器内部错误' },
  { code: '502', message: '上游服务不可用' },
  { code: '503', message: '服务暂时不可用' }
])

const bizErrorCodes = ref([
  { code: '1001', message: '余额不足，请先充值' },
  { code: '1002', message: 'API密钥无效或已禁用' },
  { code: '1003', message: '通道不可用，没有可用的上游通道' },
  { code: '1004', message: '请求频率超限，超过RPM/TPM限制' },
  { code: '1005', message: '订阅已过期，请续费' },
  { code: '1006', message: '优惠券无效或已过期' },
  { code: '1009', message: '支付失败' }
])

const toolConfigs = ref([
  { field: 'API Base URL', value: 'https://your-domain.com/v1' },
  { field: 'API Key', value: 'sk-your-api-key（在API密钥页面创建）' },
  { field: '模型', value: 'gpt-3.5-turbo / gpt-4 / claude-3-sonnet 等' }
])

const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const handleScroll = (e) => {
  const sections = ['quickstart', 'chat', 'embeddings', 'models', 'errors', 'sdk']
  const scrollTop = e.target.scrollTop
  for (let i = sections.length - 1; i >= 0; i--) {
    const el = document.getElementById(sections[i])
    if (el && el.offsetTop - 100 <= scrollTop) {
      activeSection.value = sections[i]
      break
    }
  }
}

const copyCode = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}
</script>

<style lang="scss" scoped>
.docs-page {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 120px);
}

.docs-sidebar {
  width: 200px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  align-self: flex-start;

  .sidebar-title {
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 2px solid #409eff;
  }

  .nav-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      margin-bottom: 4px;

      a {
        display: block;
        padding: 8px 12px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
        transition: all 0.2s;

        &:hover {
          background-color: #f5f7fa;
          color: #409eff;
        }

        &.active {
          background-color: #ecf5ff;
          color: #409eff;
          font-weight: 600;
        }
      }
    }
  }
}

.docs-content {
  flex: 1;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #dcdfe6;
    border-radius: 3px;
  }
}

.doc-section {
  margin-bottom: 40px;

  h2 {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 20px;
    padding-bottom: 10px;
    border-bottom: 1px solid #e4e7ed;
  }
}

.doc-card {
  margin-bottom: 16px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  p {
    font-size: 14px;
    line-height: 1.8;
    margin-bottom: 8px;
  }

  code {
    background-color: #f5f7fa;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    color: #c7254e;
  }
}

.endpoint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  .endpoint-url {
    font-size: 16px;
    font-weight: 600;
    font-family: 'Courier New', Courier, monospace;
  }
}

.code-block {
  background-color: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  position: relative;
  overflow-x: auto;

  pre {
    color: #d4d4d4;
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    line-height: 1.6;
    margin: 0;
    white-space: pre;
  }

  .copy-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    color: #909399;

    &:hover {
      color: #fff;
    }
  }
}

@media (max-width: 768px) {
  .docs-page {
    flex-direction: column;
  }

  .docs-sidebar {
    width: 100%;
    position: static;

    .nav-list {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      li {
        margin-bottom: 0;
      }
    }
  }

  .docs-content {
    max-height: none;
  }
}
</style>
