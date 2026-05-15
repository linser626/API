<template>
  <div class="playground-page">
    <div class="playground-container">
      <div class="playground-sidebar">
        <el-card shadow="hover">
          <div class="sidebar-section">
            <div class="section-label">模型选择</div>
            <el-select v-model="model" placeholder="选择模型" filterable style="width: 100%;">
              <el-option
                v-for="m in models"
                :key="m.id"
                :label="m.name"
                :value="m.id"
              />
            </el-select>
          </div>

          <div class="sidebar-section">
            <div class="section-label">
              温度 (Temperature)
              <span class="section-value">{{ temperature }}</span>
            </div>
            <el-slider v-model="temperature" :min="0" :max="2" :step="0.1" show-input-size="small" />
          </div>

          <div class="sidebar-section">
            <div class="section-label">最大Token数</div>
            <el-input-number v-model="maxTokens" :min="1" :max="128000" :step="256" style="width: 100%;" />
          </div>

          <div class="sidebar-section">
            <div class="section-label">系统提示词</div>
            <el-input
              v-model="systemPrompt"
              type="textarea"
              :rows="4"
              placeholder="输入系统提示词（可选）"
              resize="vertical"
            />
          </div>

          <div class="sidebar-section">
            <div class="section-label">响应模式</div>
            <el-radio-group v-model="streamMode">
              <el-radio :value="true">流式输出</el-radio>
              <el-radio :value="false">完整输出</el-radio>
            </el-radio-group>
          </div>

          <el-button type="danger" plain style="width: 100%; margin-top: 12px;" @click="clearConversation">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
        </el-card>
      </div>

      <div class="playground-main">
        <div class="chat-area" ref="chatAreaRef">
          <div v-if="messages.length === 0" class="empty-chat">
            <el-icon :size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
            <p>选择模型并输入消息开始对话</p>
          </div>
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-message"
            :class="msg.role"
          >
            <div class="message-avatar">
              <el-avatar :size="32" :icon="msg.role === 'user' ? UserFilled : ChatDotRound" />
            </div>
            <div class="message-body">
              <div class="message-role">{{ msg.role === 'user' ? '用户' : '助手' }}</div>
              <div
                v-if="msg.role === 'assistant'"
                class="message-content markdown-body"
                v-html="renderMarkdown(msg.content)"
              ></div>
              <div v-else class="message-content">{{ msg.content }}</div>
              <div v-if="msg.usage" class="message-usage">
                <el-tag size="small" type="info">Token: {{ msg.usage.total_tokens }}</el-tag>
                <el-tag size="small" type="success" style="margin-left: 6px;">输入: {{ msg.usage.prompt_tokens }}</el-tag>
                <el-tag size="small" type="warning" style="margin-left: 6px;">输出: {{ msg.usage.completion_tokens }}</el-tag>
              </div>
              <div v-if="msg.role === 'assistant'" class="message-actions">
                <el-button link type="primary" size="small" @click="copyMessage(msg.content)">
                  <el-icon><CopyDocument /></el-icon> 复制
                </el-button>
              </div>
            </div>
          </div>
          <div v-if="loading" class="chat-message assistant">
            <div class="message-avatar">
              <el-avatar :size="32" :icon="ChatDotRound" />
            </div>
            <div class="message-body">
              <div class="message-role">助手</div>
              <div class="message-content typing">
                <span class="typing-indicator">正在输入</span>
                <span class="typing-dots">
                  <span>.</span><span>.</span><span>.</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入消息... (Enter发送, Shift+Enter换行)"
            resize="none"
            @keydown="handleKeydown"
          />
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!inputMessage.trim() || !model"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { chatPlayground, getPlaygroundModels } from '@/api/playground'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const models = ref([])
const model = ref('')
const temperature = ref(0.7)
const maxTokens = ref(4096)
const systemPrompt = ref('')
const streamMode = ref(true)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const chatAreaRef = ref(null)

const loadModels = async () => {
  try {
    const res = await getPlaygroundModels()
    const list = res.data || []
    models.value = list
    if (list.length > 0 && !model.value) {
      model.value = list[0].id
    }
  } catch (error) {
    // handled by interceptor
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatAreaRef.value) {
    chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || !model.value || loading.value) return

  messages.value.push({ role: 'user', content })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  const apiMessages = []
  if (systemPrompt.value.trim()) {
    apiMessages.push({ role: 'system', content: systemPrompt.value.trim() })
  }
  for (const msg of messages.value) {
    apiMessages.push({ role: msg.role, content: msg.content })
  }

  try {
    if (streamMode.value) {
      await sendStreamRequest(apiMessages)
    } else {
      await sendNonStreamRequest(apiMessages)
    }
  } catch (error) {
    if (!messages.value.length || messages.value[messages.value.length - 1].role !== 'assistant') {
      messages.value.push({
        role: 'assistant',
        content: '请求失败: ' + (error.message || '未知错误')
      })
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const sendNonStreamRequest = async (apiMessages) => {
  const res = await chatPlayground({
    model: model.value,
    messages: apiMessages,
    stream: false,
    temperature: temperature.value,
    max_tokens: maxTokens.value
  })

  const data = res.data || res
  let assistantContent = ''
  let usage = null

  if (data.choices && data.choices.length > 0) {
    assistantContent = data.choices[0].message?.content || data.choices[0].text || ''
  } else if (typeof data === 'string') {
    assistantContent = data
  } else {
    assistantContent = JSON.stringify(data)
  }

  if (data.usage) {
    usage = {
      prompt_tokens: data.usage.prompt_tokens || 0,
      completion_tokens: data.usage.completion_tokens || 0,
      total_tokens: data.usage.total_tokens || 0
    }
  }

  messages.value.push({ role: 'assistant', content: assistantContent, usage })
  scrollToBottom()
}

const sendStreamRequest = async (apiMessages) => {
  const token = localStorage.getItem('token')

  const response = await fetch('/api/playground/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      model: model.value,
      messages: apiMessages,
      stream: true,
      temperature: temperature.value,
      max_tokens: maxTokens.value
    })
  })

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }

  const assistantMsg = { role: 'assistant', content: '', usage: null }
  messages.value.push(assistantMsg)
  scrollToBottom()

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed || trimmed === 'data: [DONE]') continue
      if (!trimmed.startsWith('data: ')) continue

      try {
        const jsonStr = trimmed.slice(6)
        const parsed = JSON.parse(jsonStr)

        if (parsed.choices && parsed.choices.length > 0) {
          const delta = parsed.choices[0].delta
          if (delta && delta.content) {
            assistantMsg.content += delta.content
            scrollToBottom()
          }
        }
        if (parsed.usage) {
          assistantMsg.usage = {
            prompt_tokens: parsed.usage.prompt_tokens || 0,
            completion_tokens: parsed.usage.completion_tokens || 0,
            total_tokens: parsed.usage.total_tokens || 0
          }
        }
      } catch (e) {
        // skip unparseable chunks
      }
    }
  }
}

const clearConversation = () => {
  messages.value = []
}

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    const textarea = document.createElement('textarea')
    textarea.value = content
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const renderMarkdown = (text) => {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^\- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
  html = html.replace(/\n/g, '<br>')
  return html
}

onMounted(() => {
  loadModels()
})
</script>

<style lang="scss" scoped>
.playground-page {
  height: calc(100vh - var(--header-height, 60px) - 40px);

  .playground-container {
    display: flex;
    gap: 16px;
    height: 100%;
  }

  .playground-sidebar {
    width: 280px;
    flex-shrink: 0;

    .sidebar-section {
      margin-bottom: 16px;

      .section-label {
        font-size: 13px;
        font-weight: 600;
        color: var(--color-text-regular);
        margin-bottom: 8px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .section-value {
          font-weight: 400;
          color: var(--color-text-secondary);
        }
      }
    }
  }

  .playground-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    overflow: hidden;

    .chat-area {
      flex: 1;
      overflow-y: auto;
      padding: 20px;

      .empty-chat {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: var(--color-text-secondary);

        p {
          margin-top: 16px;
          font-size: 14px;
        }
      }

      .chat-message {
        display: flex;
        gap: 12px;
        margin-bottom: 20px;

        &.user {
          .message-body {
            .message-content {
              background-color: #ecf5ff;
              border-radius: 12px 12px 4px 12px;
            }
          }
        }

        &.assistant {
          .message-body {
            .message-content {
              background-color: #f5f7fa;
              border-radius: 12px 12px 12px 4px;
            }
          }
        }

        .message-avatar {
          flex-shrink: 0;
        }

        .message-body {
          flex: 1;
          min-width: 0;

          .message-role {
            font-size: 12px;
            color: var(--color-text-secondary);
            margin-bottom: 4px;
          }

          .message-content {
            padding: 10px 14px;
            font-size: 14px;
            line-height: 1.6;
            word-break: break-word;

            &.typing {
              display: flex;
              align-items: center;
              gap: 4px;

              .typing-indicator {
                color: var(--color-text-secondary);
              }

              .typing-dots {
                span {
                  animation: blink 1.4s infinite both;
                  font-weight: bold;
                  color: var(--color-primary);

                  &:nth-child(2) { animation-delay: 0.2s; }
                  &:nth-child(3) { animation-delay: 0.4s; }
                }
              }
            }
          }

          .message-usage {
            margin-top: 6px;
            display: flex;
            align-items: center;
          }

          .message-actions {
            margin-top: 4px;
          }
        }
      }
    }

    .input-area {
      display: flex;
      gap: 12px;
      padding: 16px 20px;
      border-top: 1px solid #f0f0f0;
      background: #fafafa;

      :deep(.el-textarea) {
        flex: 1;
      }

      .el-button {
        align-self: flex-end;
        height: 40px;
      }
    }
  }
}

.markdown-body {
  :deep(pre) {
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 8px 0;

    code {
      background: none;
      padding: 0;
      color: inherit;
      font-size: 13px;
    }
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.06);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  }

  :deep(h2), :deep(h3), :deep(h4) {
    margin: 8px 0 4px;
  }

  :deep(li) {
    margin-left: 20px;
    list-style: disc;
  }
}

@keyframes blink {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}
</style>
