<template>
  <div class="admin-model-prices">
    <div class="page-header flex-between">
      <h2>模型定价</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        添加定价
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="modelPrices" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="model" label="模型名称" width="220" />
        <el-table-column label="输入价格" width="140" align="center">
          <template #default="{ row }">
            ¥{{ row.input_price }} / 1K tokens
          </template>
        </el-table-column>
        <el-table-column label="输出价格" width="140" align="center">
          <template #default="{ row }">
            ¥{{ row.output_price }} / 1K tokens
          </template>
        </el-table-column>
        <el-table-column prop="channel_type" label="渠道类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.channel_type || '通用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
          @size-change="loadModelPrices"
          @current-change="loadModelPrices"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加模型定价" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="模型名称" prop="model">
          <el-input v-model="form.model" placeholder="例如: gpt-4, claude-3-opus" />
        </el-form-item>
        <el-form-item label="输入价格" prop="input_price">
          <el-input-number
            v-model="form.input_price"
            :min="0"
            :precision="6"
            :step="0.001"
            style="width: 100%;"
          />
          <span class="text-info" style="font-size: 12px;">元 / 1K tokens</span>
        </el-form-item>
        <el-form-item label="输出价格" prop="output_price">
          <el-input-number
            v-model="form.output_price"
            :min="0"
            :precision="6"
            :step="0.001"
            style="width: 100%;"
          />
          <span class="text-info" style="font-size: 12px;">元 / 1K tokens</span>
        </el-form-item>
        <el-form-item label="渠道类型" prop="channel_type">
          <el-select v-model="form.channel_type" placeholder="选择渠道类型(可选)" clearable style="width: 100%;">
            <el-option label="通用" value="" />
            <el-option label="OpenAI" value="openai" />
            <el-option label="Azure" value="azure" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Google AI" value="google" />
          </el-select>
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
import { listModelPrices, createModelPrice, deleteModelPrice } from '@/api/admin'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const modelPrices = ref([])
const pagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const formRef = ref(null)
const form = reactive({
  model: '',
  input_price: 0.01,
  output_price: 0.03,
  channel_type: ''
})

const formRules = {
  model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  input_price: [{ required: true, message: '请输入输入价格', trigger: 'blur' }],
  output_price: [{ required: true, message: '请输入输出价格', trigger: 'blur' }]
}

const loadModelPrices = async () => {
  loading.value = true
  try {
    const res = await listModelPrices({
      page: pagination.page,
      page_size: pagination.page_size
    })
    modelPrices.value = res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  form.model = ''
  form.input_price = 0.01
  form.output_price = 0.03
  form.channel_type = ''
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
    await createModelPrice(form)
    ElMessage.success('模型定价添加成功')
    dialogVisible.value = false
    loadModelPrices()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除模型 "${row.model}" 的定价吗？`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteModelPrice(row.id)
      ElMessage.success('定价已删除')
      loadModelPrices()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadModelPrices()
})
</script>

<style lang="scss" scoped>
.admin-model-prices {
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
