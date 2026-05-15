<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand-section">
          <el-icon :size="48" color="#fff"><Cpu /></el-icon>
          <h1>AI Relay</h1>
          <p>AI大模型中转平台</p>
          <div class="features">
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>多模型统一接入</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>智能负载均衡</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>实时用量监控</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>灵活订阅方案</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-card">
          <h2>欢迎登录</h2>
          <p class="subtitle">登录您的账户以继续使用服务</p>
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            size="large"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <div class="login-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="login-footer">
            <span>还没有账户？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch (error) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container {
  display: flex;
  width: 900px;
  min-height: 520px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-left {
  width: 400px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;

  .brand-section {
    text-align: center;
    color: #fff;

    h1 {
      font-size: 32px;
      margin-top: 16px;
      letter-spacing: 2px;
    }

    p {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.7);
      margin-top: 8px;
    }

    .features {
      margin-top: 40px;
      text-align: left;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 16px;
        font-size: 14px;
        color: rgba(255, 255, 255, 0.85);

        .el-icon {
          color: var(--color-success);
          font-size: 16px;
        }
      }
    }
  }
}

.login-right {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;

  .login-card {
    width: 100%;
    max-width: 360px;

    h2 {
      font-size: 28px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 14px;
      color: var(--color-text-secondary);
      margin-bottom: 32px;
    }

    .login-options {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .login-btn {
      width: 100%;
      height: 44px;
      font-size: 16px;
    }

    .login-footer {
      text-align: center;
      margin-top: 20px;
      font-size: 14px;
      color: var(--color-text-secondary);

      a {
        color: var(--color-primary);
        margin-left: 4px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
