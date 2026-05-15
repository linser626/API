<template>
  <div class="main-layout">
    <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="sidebar-logo">
        <el-icon :size="28"><Cpu /></el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">AI Relay</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/apikeys">
          <el-icon><Key /></el-icon>
          <template #title>API密钥</template>
        </el-menu-item>
        <el-menu-item index="/subscription">
          <el-icon><Tickets /></el-icon>
          <template #title>订阅方案</template>
        </el-menu-item>
        <el-menu-item index="/billing">
          <el-icon><Wallet /></el-icon>
          <template #title>账单</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><DataLine /></el-icon>
          <template #title>用量监控</template>
        </el-menu-item>
        <el-menu-item index="/coupons">
          <el-icon><Present /></el-icon>
          <template #title>优惠券</template>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/dashboard">
          <el-icon><Setting /></el-icon>
          <template #title>管理后台</template>
        </el-menu-item>
      </el-menu>
    </div>

    <div class="main-container" :class="{ expanded: appStore.sidebarCollapsed }">
      <div class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="appStore.toggleSidebar"
          >
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title && currentRoute.name !== 'Dashboard'">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div class="balance-display">
            <el-icon><Wallet /></el-icon>
            <span>余额: {{ formatMoney(userStore.balance) }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>

    <el-dialog v-model="profileDialogVisible" title="个人信息" width="480px">
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="480px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.old_password" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.new_password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirm_password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { updateProfile, changePassword } from '@/api/auth'
import { formatMoney } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const profileForm = reactive({
  username: '',
  email: ''
})

const passwordForm = reactive({
  old_password: '',
  new_password: '',
  confirm_password: ''
})

const handleCommand = (command) => {
  if (command === 'profile') {
    profileForm.username = userStore.userInfo.username || ''
    profileForm.email = userStore.userInfo.email || ''
    profileDialogVisible.value = true
  } else if (command === 'password') {
    passwordForm.old_password = ''
    passwordForm.new_password = ''
    passwordForm.confirm_password = ''
    passwordDialogVisible.value = true
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
    }).catch(() => {})
  }
}

const handleUpdateProfile = async () => {
  try {
    await updateProfile(profileForm)
    await userStore.getUserInfo()
    profileDialogVisible.value = false
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    // error handled by interceptor
  }
}

const handleChangePassword = async () => {
  if (passwordForm.new_password !== passwordForm.confirm_password) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  try {
    await changePassword(passwordForm)
    passwordDialogVisible.value = false
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
  } catch (error) {
    // error handled by interceptor
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background-color: var(--color-sidebar);
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;

  &.collapsed {
    width: var(--sidebar-collapsed-width);
  }

  .sidebar-logo {
    height: var(--header-height);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding: 0 16px;
    white-space: nowrap;
    overflow: hidden;

    .logo-text {
      font-size: 18px;
      letter-spacing: 1px;
    }
  }

  .el-menu {
    border-right: none;
  }

  :deep(.el-menu-item) {
    &.is-active {
      background-color: var(--color-sidebar-active) !important;
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.05) !important;
    }
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg);
  transition: margin-left 0.3s ease;

  .header {
    height: var(--header-height);
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
    z-index: 10;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .collapse-btn {
        cursor: pointer;
        color: var(--color-text-regular);
        transition: color 0.3s;

        &:hover {
          color: var(--color-primary);
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 20px;

      .balance-display {
        display: flex;
        align-items: center;
        gap: 6px;
        color: var(--color-warning);
        font-weight: 600;
        font-size: 14px;
      }

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 6px;
        transition: background-color 0.3s;

        &:hover {
          background-color: #f5f7fa;
        }

        .username {
          font-size: 14px;
          color: var(--color-text-primary);
        }
      }
    }
  }

  .main-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
  }
}
</style>
