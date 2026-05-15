<template>
  <div class="admin-channels">
    <div class="page-header flex-between">
      <h2>渠道管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        添加渠道
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="channels" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ channelTypeMap[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="base_url" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column label="模型" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row.models || []).join(', ') }}
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80" align="center" prop="priority" />
        <el-table-column label="权重" width="80" align="center" prop="weight" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'active'"
              @change="handleToggle(row)"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
            />
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.success_rate >= 95 ? 'text-success' : row.success_rate >= 80 ? 'text-warning' : 'text-danger'">
              {{ formatPercent(row.success_rate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleTest(row)" :loading="testingId === row.id">测试</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.page_size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadChannels"
          @current-change="loadChannels"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑渠道' : '添加渠道'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入渠道名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择渠道类型" style="width: 100%;">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Azure OpenAI" value="azure" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Google AI" value="google" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" prop="base_url">
          <el-input v-model="form.base_url" placeholder="https://api.openai.com" />
        </el-form-item>
        <el-form-item label="API Key" prop="api_key">
          <el-input v-model="form.api_key" type="password" show-password placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="支持模型" prop="models">
          <el-select
            v-model="form.models"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入模型名称后回车"
            style="width: 100%;"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="100" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权重" prop="weight">
              <el-input-number v-model="form.weight" :min="1" :max="100" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(秒)" prop="timeout">
              <el-input-number v-model="form.timeout" :min="1" :max="300" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="重试次数" prop="retries">
          <el-input-number v-model="form.retries" :min="0" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listChannels, createChannel, updateChannel, deleteChannel, testChannel, toggleChannel } from '@/api/admin'
import { formatPercent } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const testingId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const channels = ref([])
const pagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const channelTypeMap = {
  openai: 'OpenAI',
  azure: 'Azure',
  anthropic: 'Anthropic',
  google: 'Google AI',
  custom: '自定义'
}

const formRef = ref(null)
const form = reactive({
  name: '',
  type: 'openai',
  base_url: '',
  api_key: '',
  models: [],
  priority: 0,
  weight: 1,
  timeout: 30,
  retries: 3
})

const formRules = {
  name: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  base_url: [{ required: true, message: '请输入Base URL', trigger: 'blur' }],
  api_key: [{ required: true, message: '请输入API Key', trigger: 'blur' }]
}

const loadChannels = async () => {
  loading.value = true
  try {
    const res = await listChannels({
      page: pagination.page,
      page_size: pagination.page_size
    })
    channels.value = res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
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
  form.type = 'openai'
  form.base_url = ''
  form.api_key = ''
  form.models = []
  form.priority = 0
  form.weight = 1
  form.timeout = 30
  form.retries = 3
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.base_url = row.base_url
  form.api_key = ''
  form.models = row.models || []
  form.priority = row.priority || 0
  form.weight = row.weight || 1
  form.timeout = row.timeout || 30
  form.retries = row.retries || 3
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
    const data = { ...form }
    if (isEdit.value && !data.api_key) {
      delete data.api_key
    }
    if (isEdit.value) {
      await updateChannel(editingId.value, data)
      ElMessage.success('渠道更新成功')
    } else {
      await createChannel(data)
      ElMessage.success('渠道创建成功')
    }
    dialogVisible.value = false
    loadChannels()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleToggle = async (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  try {
    await toggleChannel(row.id, { status: newStatus })
    ElMessage.success(newStatus === 'active' ? '渠道已启用' : '渠道已禁用')
    loadChannels()
  } catch (error) {
    // handled by interceptor
  }
}

const handleTest = async (row) => {
  testingId.value = row.id
  try {
    const res = await testChannel(row.id)
    if (res.data?.success) {
      ElMessage.success(`渠道测试成功，延迟: ${res.data.latency || 0}ms`)
    } else {
      ElMessage.error(`渠道测试失败: ${res.data?.error || '未知错误'}`)
    }
  } catch (error) {
    // handled by interceptor
  } finally {
    testingId.value = null
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除渠道 "${row.name}" 吗？`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteChannel(row.id)
      ElMessage.success('渠道已删除')
      loadChannels()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadChannels()
})
</script>

<style lang="scss" scoped>
.admin-channels {
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
