<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-left">
        <div class="brand-section">
          <el-icon :size="48" color="#fff"><Cpu /></el-icon>
          <h1>AI Relay</h1>
          <p>AI大模型中转平台</p>
          <div class="benefits">
            <div class="benefit-item">
              <el-icon><Promotion /></el-icon>
              <span>注册即送体验额度</span>
            </div>
            <div class="benefit-item">
              <el-icon><Timer /></el-icon>
              <span>按量计费，用多少付多少</span>
            </div>
            <div class="benefit-item">
              <el-icon><Shield /></el-icon>
              <span>数据安全有保障</span>
            </div>
          </div>
        </div>
      </div>
      <div class="register-right">
        <div class="register-card">
          <h2>创建账户</h2>
          <p class="subtitle">注册新账户开始使用AI服务</p>
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            size="large"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                :prefix-icon="Message"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.referralCode"
                placeholder="邀请码（选填）"
                :prefix-icon="Promotion"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="register-btn"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="register-footer">
            <span>已有账户？</span>
            <router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Promotion } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  referralCode: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      referralCode: registerForm.referralCode || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.ref) {
    registerForm.referralCode = route.query.ref
  }
})
</script>

<style lang="scss" scoped>
.register-page {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-container {
  display: flex;
  width: 900px;
  min-height: 580px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-left {
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

    .benefits {
      margin-top: 40px;
      text-align: left;

      .benefit-item {
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

.register-right {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;

  .register-card {
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

    .register-btn {
      width: 100%;
      height: 44px;
      font-size: 16px;
    }

    .register-footer {
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
