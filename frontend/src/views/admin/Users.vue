<template>
  <div class="admin-users">
    <div class="page-header flex-between">
      <h2>用户管理</h2>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/邮箱"
          clearable
          style="width: 240px;"
          @keyup.enter="loadUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="loadUsers">搜索</el-button>
      </div>
    </div>

    <el-card shadow="hover">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="center">
          <template #default="{ row }">
            {{ formatMoney(row.balance) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 'active' ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="primary" link size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button type="success" link size="small" @click="handleGiftBalance(row)">
              赠送余额
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.page_size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="giftDialogVisible" title="赠送余额" width="400px">
      <el-form :model="giftForm" label-width="80px">
        <el-form-item label="用户">
          <el-input :model-value="giftForm.username" disabled />
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="giftForm.amount" :min="0.01" :max="10000" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="giftForm.remark" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="giftDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="giftLoading" @click="confirmGift">确认赠送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listUsers, updateUserStatus, resetPassword, giftBalance } from '@/api/admin'
import { formatMoney, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const searchKeyword = ref('')
const users = ref([])
const pagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const giftDialogVisible = ref(false)
const giftLoading = ref(false)
const giftForm = reactive({
  userId: null,
  username: '',
  amount: 10,
  remark: ''
})

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      page_size: pagination.page_size
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res = await listUsers(params)
    users.value = res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const action = newStatus === 'active' ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateUserStatus(row.id, { status: newStatus })
      ElMessage.success(`用户已${action}`)
      loadUsers()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const handleResetPassword = (row) => {
  ElMessageBox.confirm(`确定要重置用户 "${row.username}" 的密码吗？`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await resetPassword(row.id, {})
      ElMessage.success(`密码已重置${res.data?.new_password ? '，新密码: ' + res.data.new_password : ''}`)
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const handleGiftBalance = (row) => {
  giftForm.userId = row.id
  giftForm.username = row.username
  giftForm.amount = 10
  giftForm.remark = ''
  giftDialogVisible.value = true
}

const confirmGift = async () => {
  if (giftForm.amount <= 0) {
    ElMessage.warning('请输入有效的金额')
    return
  }
  giftLoading.value = true
  try {
    await giftBalance({
      user_id: giftForm.userId,
      amount: giftForm.amount,
      remark: giftForm.remark
    })
    ElMessage.success('余额赠送成功')
    giftDialogVisible.value = false
    loadUsers()
  } catch (error) {
    // handled by interceptor
  } finally {
    giftLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style lang="scss" scoped>
.admin-users {
  .search-bar {
    display: flex;
    gap: 12px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
