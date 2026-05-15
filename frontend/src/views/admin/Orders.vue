<template>
  <div class="admin-orders">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="flex-between">
          <span class="card-title">订单列表</span>
          <div class="filters">
            <el-select v-model="filters.status" placeholder="订单状态" clearable style="width: 130px;" @change="loadOrders">
              <el-option label="全部" value="" />
              <el-option label="待支付" value="pending" />
              <el-option label="已支付" value="paid" />
              <el-option label="已退款" value="refunded" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
            <el-select v-model="filters.payment_method" placeholder="支付方式" clearable style="width: 130px; margin-left: 12px;" @change="loadOrders">
              <el-option label="全部" value="" />
              <el-option label="支付宝" value="alipay" />
              <el-option label="微信支付" value="wechat" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px; margin-left: 12px;"
              @change="loadOrders"
            />
          </div>
        </div>
      </template>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="order_no" label="订单号" width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="金额" width="120" align="center">
          <template #default="{ row }">
            {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="110" align="center">
          <template #default="{ row }">
            {{ paymentMethodMap[row.payment_method] || row.payment_method }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'" size="small">
              {{ statusLabelMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'paid'"
              type="warning"
              link
              size="small"
              @click="handleRefund(row)"
            >
              退款
            </el-button>
            <span v-else class="text-info" style="font-size: 12px;">-</span>
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
          @size-change="loadOrders"
          @current-change="loadOrders"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrders } from '@/api/payment'
import { refundOrder } from '@/api/admin'
import { formatMoney, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const orders = ref([])
const pagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const filters = reactive({
  status: '',
  payment_method: '',
  dateRange: null
})

const paymentMethodMap = {
  alipay: '支付宝',
  wechat: '微信支付'
}

const statusTagMap = {
  pending: 'warning',
  paid: 'success',
  refunded: 'info',
  cancelled: 'danger'
}

const statusLabelMap = {
  pending: '待支付',
  paid: '已支付',
  refunded: '已退款',
  cancelled: '已取消'
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      page_size: pagination.page_size
    }
    if (filters.status) params.status = filters.status
    if (filters.payment_method) params.payment_method = filters.payment_method
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.start_date = filters.dateRange[0]
      params.end_date = filters.dateRange[1]
    }
    const res = await getOrders(params)
    orders.value = res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleRefund = (row) => {
  ElMessageBox.confirm(
    `确定要对订单 ${row.order_no} 进行退款吗？退款金额: ${formatMoney(row.amount)}`,
    '退款确认',
    {
      confirmButtonText: '确定退款',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await refundOrder(row.order_no)
      ElMessage.success('退款成功')
      loadOrders()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadOrders()
})
</script>

<style lang="scss" scoped>
.admin-orders {
  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .filters {
    display: flex;
    align-items: center;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
