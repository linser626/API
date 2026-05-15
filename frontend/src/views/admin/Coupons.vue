<template>
  <div class="admin-coupons">
    <div class="page-header flex-between">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建优惠券
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="coupons" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="code" label="优惠券码" width="180" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'percentage' ? 'warning' : 'success'" size="small">
              {{ row.type === 'percentage' ? '折扣' : '固定金额' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面值" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.type === 'percentage'" class="text-warning">{{ row.value }}%</span>
            <span v-else class="text-success">{{ formatMoney(row.value) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最低消费" width="110" align="center">
          <template #default="{ row }">
            {{ row.min_amount > 0 ? formatMoney(row.min_amount) : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="已用/总量" width="110" align="center">
          <template #default="{ row }">
            {{ row.used_count || 0 }} / {{ row.total_count || '∞' }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200" align="center">
          <template #default="{ row }">
            {{ formatDate(row.start_at) }} ~ {{ formatDate(row.end_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
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
          @size-change="loadCoupons"
          @current-change="loadCoupons"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑优惠券' : '创建优惠券'"
      width="520px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="fixed">固定金额</el-radio>
            <el-radio value="percentage">折扣比例</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="面值" prop="value">
          <el-input-number
            v-model="form.value"
            :min="form.type === 'percentage' ? 1 : 0.01"
            :max="form.type === 'percentage' ? 100 : 100000"
            :precision="form.type === 'percentage' ? 0 : 2"
            style="width: 100%;"
          />
          <span class="ml-10 text-info" style="font-size: 12px;">
            {{ form.type === 'percentage' ? '折扣百分比(1-100)' : '固定金额(元)' }}
          </span>
        </el-form-item>
        <el-form-item label="最低消费" prop="min_amount">
          <el-input-number v-model="form.min_amount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="发放数量" prop="total_count">
          <el-input-number v-model="form.total_count" :min="1" :max="100000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker
            v-model="form.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
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
import { listCoupons, createCoupon, updateCoupon, deleteCoupon } from '@/api/admin'
import { formatMoney, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const coupons = ref([])
const pagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const formRef = ref(null)
const form = reactive({
  name: '',
  type: 'fixed',
  value: 10,
  min_amount: 0,
  total_count: 100,
  dateRange: null
})

const formRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入面值', trigger: 'blur' }],
  dateRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await listCoupons({
      page: pagination.page,
      page_size: pagination.page_size
    })
    coupons.value = res.data?.list || res.data || []
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
  form.type = 'fixed'
  form.value = 10
  form.min_amount = 0
  form.total_count = 100
  form.dateRange = null
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.value = row.value
  form.min_amount = row.min_amount || 0
  form.total_count = row.total_count || 100
  form.dateRange = [row.start_at, row.end_at]
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
    const data = {
      name: form.name,
      type: form.type,
      value: form.value,
      min_amount: form.min_amount,
      total_count: form.total_count,
      start_at: form.dateRange?.[0],
      end_at: form.dateRange?.[1]
    }
    if (isEdit.value) {
      await updateCoupon(editingId.value, data)
      ElMessage.success('优惠券更新成功')
    } else {
      await createCoupon(data)
      ElMessage.success('优惠券创建成功')
    }
    dialogVisible.value = false
    loadCoupons()
  } catch (error) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除优惠券 "${row.name}" 吗？`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCoupon(row.id)
      ElMessage.success('优惠券已删除')
      loadCoupons()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadCoupons()
})
</script>

<style lang="scss" scoped>
.admin-coupons {
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
