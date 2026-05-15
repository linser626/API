<template>
  <div class="billing-page">
    <div class="page-header">
      <h2>账单中心</h2>
    </div>

    <el-card shadow="hover" class="mb-20">
      <div class="balance-section">
        <div class="balance-info">
          <div class="balance-label">账户余额</div>
          <div class="balance-amount">{{ formatMoney(overview.balance) }}</div>
          <div class="balance-detail">
            <span>本月消费: {{ formatMoney(overview.monthly_spending) }}</span>
          </div>
        </div>
        <el-button type="primary" size="large" @click="showRechargeDialog">
          <el-icon><Wallet /></el-icon>
          充值
        </el-button>
      </div>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header flex-between">
          <span class="card-title">交易记录</span>
          <div class="filters">
            <el-select v-model="filters.type" placeholder="交易类型" clearable style="width: 140px;" @change="loadTransactions">
              <el-option label="全部" value="" />
              <el-option label="充值" value="recharge" />
              <el-option label="消费" value="consume" />
              <el-option label="退款" value="refund" />
              <el-option label="赠送" value="gift" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px; margin-left: 12px;"
              @change="loadTransactions"
            />
            <el-button type="success" plain style="margin-left: 12px;" @click="handleExportTransactions" :loading="exportLoading">
              <el-icon><Download /></el-icon>
              导出CSV
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="transactions" v-loading="loading" stripe>
        <el-table-column label="交易类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type] || 'info'" size="small">
              {{ typeLabelMap[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="center">
          <template #default="{ row }">
            <span :class="row.amount >= 0 ? 'text-success' : 'text-danger'">
              {{ row.amount >= 0 ? '+' : '' }}{{ formatMoney(Math.abs(row.amount)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="交易时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
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
          @size-change="loadTransactions"
          @current-change="loadTransactions"
        />
      </div>
    </el-card>

    <el-dialog v-model="rechargeDialogVisible" title="账户充值" width="480px" @close="handleRechargeDialogClose">
      <el-form :model="rechargeForm" label-width="90px">
        <el-form-item label="充值金额">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="1"
            :max="10000"
            :step="10"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="快捷金额">
          <div class="quick-amounts">
            <el-button
              v-for="amount in quickAmounts"
              :key="amount"
              :type="rechargeForm.amount === amount ? 'primary' : 'default'"
              @click="rechargeForm.amount = amount"
            >
              ¥{{ amount }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-radio-group v-model="rechargeForm.payment_method">
            <el-radio value="alipay">
              <div class="payment-option">
                <el-icon :size="18" color="#1677ff"><CreditCard /></el-icon>
                <span>支付宝</span>
              </div>
            </el-radio>
            <el-radio value="wechat">
              <div class="payment-option">
                <el-icon :size="18" color="#07c160"><ChatDotRound /></el-icon>
                <span>微信支付</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠券码">
          <div class="coupon-input-row">
            <el-input v-model="rechargeForm.coupon_code" placeholder="可选，输入优惠券码" clearable />
            <el-button type="primary" plain @click="applyRechargeCoupon" :loading="rechargeCouponLoading">验证</el-button>
          </div>
          <div v-if="rechargeCouponInfo" class="coupon-result text-success">
            优惠券已生效: {{ rechargeCouponInfo.description || `优惠 ¥${rechargeDiscountAmount.toFixed(2)}` }}
          </div>
        </el-form-item>
        <el-form-item label="实付金额">
          <div class="final-amount">
            <span v-if="rechargeDiscountAmount > 0" class="original-price">¥{{ rechargeForm.amount }}</span>
            <span class="final-price">¥{{ rechargeFinalAmount.toFixed(2) }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeLoading" @click="handleRecharge">
          确认充值 ¥{{ rechargeFinalAmount.toFixed(2) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getOverview, getTransactions, recharge } from '@/api/billing'
import { exportTransactions } from '@/api/monitor'
import { pay } from '@/api/payment'
import { formatMoney, formatDate, downloadFile } from '@/utils/format'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const rechargeDialogVisible = ref(false)
const rechargeLoading = ref(false)
const rechargeCouponLoading = ref(false)
const rechargeCouponInfo = ref(null)
const exportLoading = ref(false)

const overview = reactive({
  balance: 0,
  monthly_spending: 0
})

const transactions = ref([])
const pagination = reactive({
  page: 1,
  page_size: 10,
  total: 0
})

const filters = reactive({
  type: '',
  dateRange: null
})

const quickAmounts = [10, 50, 100, 200, 500, 1000]

const rechargeForm = reactive({
  amount: 100,
  payment_method: 'alipay',
  coupon_code: ''
})

const rechargeDiscountAmount = computed(() => {
  if (!rechargeCouponInfo.value) return 0
  if (rechargeCouponInfo.value.type === 'percent') {
    return rechargeForm.amount * (rechargeCouponInfo.value.value / 100)
  }
  return rechargeCouponInfo.value.value || 0
})

const rechargeFinalAmount = computed(() => {
  return Math.max(0, rechargeForm.amount - rechargeDiscountAmount.value)
})

const typeTagMap = {
  recharge: 'success',
  consume: 'danger',
  refund: 'warning',
  gift: 'primary'
}

const typeLabelMap = {
  recharge: '充值',
  consume: '消费',
  refund: '退款',
  gift: '赠送'
}

const loadOverview = async () => {
  try {
    const res = await getOverview()
    if (res.data) {
      overview.balance = res.data.balance || 0
      overview.monthly_spending = res.data.monthly_spending || 0
    }
  } catch (error) {
    // handled by interceptor
  }
}

const loadTransactions = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      page_size: pagination.page_size
    }
    if (filters.type) params.type = filters.type
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.start_date = filters.dateRange[0]
      params.end_date = filters.dateRange[1]
    }
    const res = await getTransactions(params)
    transactions.value = res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleExportTransactions = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (filters.type) params.type = filters.type
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startTime = filters.dateRange[0] + 'T00:00:00'
      params.endTime = filters.dateRange[1] + 'T23:59:59'
    }

    const response = await exportTransactions(params)
    const blob = new Blob([response], { type: 'text/csv;charset=utf-8' })
    const filename = `export_transactions_${dayjs().format('YYYYMMDD')}.csv`
    downloadFile(blob, filename)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const showRechargeDialog = () => {
  rechargeForm.amount = 100
  rechargeForm.payment_method = 'alipay'
  rechargeForm.coupon_code = ''
  rechargeCouponInfo.value = null
  rechargeDialogVisible.value = true
}

const handleRechargeDialogClose = () => {
  rechargeCouponInfo.value = null
}

const applyRechargeCoupon = async () => {
  if (!rechargeForm.coupon_code) {
    ElMessage.warning('请输入优惠券码')
    return
  }
  rechargeCouponLoading.value = true
  try {
    const res = await pay({ coupon_code: rechargeForm.coupon_code, action: 'verify' })
    rechargeCouponInfo.value = res.data || null
    ElMessage.success('优惠券验证成功')
  } catch (error) {
    rechargeCouponInfo.value = null
  } finally {
    rechargeCouponLoading.value = false
  }
}

const handleRecharge = async () => {
  if (rechargeForm.amount <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  rechargeLoading.value = true
  try {
    const res = await recharge({
      amount: rechargeForm.amount,
      payment_method: rechargeForm.payment_method,
      coupon_code: rechargeForm.coupon_code || undefined
    })
    if (rechargeForm.payment_method === 'alipay' && res.data?.pay_url) {
      window.open(res.data.pay_url, '_blank')
      ElMessage.success('已跳转至支付宝，请完成支付')
    } else {
      ElMessage.success('充值成功')
    }
    rechargeDialogVisible.value = false
    loadOverview()
    loadTransactions()
  } catch (error) {
    // handled by interceptor
  } finally {
    rechargeLoading.value = false
  }
}

onMounted(() => {
  loadOverview()
  loadTransactions()
})
</script>

<style lang="scss" scoped>
.billing-page {
  .balance-section {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .balance-info {
      .balance-label {
        font-size: 14px;
        color: var(--color-text-secondary);
      }

      .balance-amount {
        font-size: 36px;
        font-weight: 700;
        color: var(--color-primary);
        margin: 8px 0;
      }

      .balance-detail {
        font-size: 13px;
        color: var(--color-text-secondary);
      }
    }
  }

  .card-header {
    .card-title {
      font-size: 16px;
      font-weight: 600;
    }

    .filters {
      display: flex;
      align-items: center;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }

  .quick-amounts {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .payment-option {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .coupon-input-row {
    display: flex;
    gap: 8px;
    width: 100%;

    .el-input {
      flex: 1;
    }
  }

  .coupon-result {
    font-size: 13px;
    margin-top: 6px;
  }

  .final-amount {
    .original-price {
      text-decoration: line-through;
      color: var(--color-text-secondary);
      font-size: 14px;
      margin-right: 8px;
    }

    .final-price {
      font-size: 24px;
      font-weight: 700;
      color: var(--color-primary);
    }
  }
}
</style>
