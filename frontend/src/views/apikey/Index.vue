<template>
  <div class="apikey-page">
    <div class="page-header flex-between">
      <h2>API密钥管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建密钥
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="apiKeyList" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column label="Key" min-width="240">
          <template #default="{ row }">
            <div class="key-cell">
              <code class="key-text">{{ maskKey(row.key) }}</code>
              <el-button
                type="primary"
                link
                size="small"
                @click="copyKey(row.key)"
              >
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rate_limit" label="速率限制" width="120" align="center">
          <template #default="{ row }">
            {{ row.rate_limit || '默认' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 'active' ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleKeyStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑密钥' : '创建密钥'"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入密钥名称" />
        </el-form-item>
        <el-form-item label="速率限制" prop="rate_limit">
          <el-input-number v-model="form.rate_limit" :min="0" :max="10000" placeholder="0表示默认" />
          <span class="ml-10 text-info" style="font-size: 12px;">次/分钟，0为默认</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="keyDisplayVisible" title="密钥创建成功" width="500px">
      <el-alert
        title="请妥善保存您的API密钥，关闭后将无法再次查看完整密钥"
        type="warning"
        :closable="false"
        show-icon
        class="mb-20"
      />
      <div class="key-display">
        <code>{{ createdKey }}</code>
        <el-button type="primary" size="small" @click="copyKey(createdKey)">
          <el-icon><CopyDocument /></el-icon>
          复制
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listApiKeys, createApiKey, updateApiKey, deleteApiKey } from '@/api/apikey'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import Clipboard from 'clipboard'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const keyDisplayVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const createdKey = ref('')
const apiKeyList = ref([])

const formRef = ref(null)
const form = reactive({
  name: '',
  rate_limit: 0
})

const formRules = {
  name: [
    { required: true, message: '请输入密钥名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度为2-50个字符', trigger: 'blur' }
  ]
}

const maskKey = (key) => {
  if (!key) return '-'
  if (key.length <= 8) return key
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const copyKey = (key) => {
  const clipboard = new Clipboard('.apikey-page', {
    text: () => key
  })
  clipboard.on('success', () => {
    ElMessage.success('密钥已复制到剪贴板')
    clipboard.destroy()
  })
  clipboard.on('error', () => {
    ElMessage.error('复制失败，请手动复制')
    clipboard.destroy()
  })
  clipboard.onClick({ currentTarget: document.querySelector('.apikey-page') })
}

const loadApiKeys = async () => {
  loading.value = true
  try {
    const res = await listApiKeys()
    apiKeyList.value = res.data?.list || res.data || []
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.rate_limit = 0
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.rate_limit = row.rate_limit || 0
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateApiKey(editingId.value, form)
      ElMessage.success('密钥更新成功')
    } else {
      const res = await createApiKey(form)
      if (res.data?.key) {
        createdKey.value = res.data.key
        keyDisplayVisible.value = true
      }
      ElMessage.success('密钥创建成功')
    }
    dialogVisible.value = false
    loadApiKeys()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const toggleKeyStatus = async (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  try {
    await updateApiKey(row.id, { status: newStatus })
    ElMessage.success(newStatus === 'active' ? '密钥已启用' : '密钥已禁用')
    loadApiKeys()
  } catch (error) {
    // handled by interceptor
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除密钥 "${row.name}" 吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteApiKey(row.id)
      ElMessage.success('密钥已删除')
      loadApiKeys()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadApiKeys()
})
</script>

<style lang="scss" scoped>
.apikey-page {
  .key-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .key-text {
      font-family: 'Courier New', monospace;
      font-size: 13px;
      background-color: #f5f7fa;
      padding: 4px 8px;
      border-radius: 4px;
      color: var(--color-text-regular);
    }
  }

  .key-display {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background-color: #f5f7fa;
    border-radius: 8px;

    code {
      flex: 1;
      font-family: 'Courier New', monospace;
      font-size: 14px;
      word-break: break-all;
      color: var(--color-text-primary);
    }
  }
}
</style>
