<template>
  <div class="referral-page">
    <div class="page-header">
      <h2>推荐返利</h2>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-primary">{{ info.totalReferrals || 0 }}</div>
              <div class="stats-label">推荐人数</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(64, 158, 255, 0.1); color: #409EFF;">
              <el-icon :size="24"><User /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-success">¥{{ formatMoney(info.totalEarned) }}</div>
              <div class="stats-label">累计佣金</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(103, 194, 58, 0.1); color: #67C23A;">
              <el-icon :size="24"><Wallet /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-warning">{{ info.commissionRate || 10 }}%</div>
              <div class="stats-label">佣金比例</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(230, 162, 60, 0.1); color: #E6A23C;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value" style="color: #9b59b6;">{{ info.referralCode || '-' }}</div>
              <div class="stats-label">我的推荐码</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(155, 89, 182, 0.1); color: #9b59b6;">
              <el-icon :size="24"><Promotion /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="mb-20">
      <template #header>
        <span class="card-title">推荐链接</span>
      </template>
      <div class="referral-link-section">
        <div class="link-display">
          <el-input
            :model-value="info.referralLink || ''"
            readonly
            size="large"
          >
            <template #append>
              <el-button type="primary" @click="copyLink">
                <el-icon><CopyDocument /></el-icon>
                复制链接
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="share-tips">
          <el-icon :size="16" color="#E6A23C"><InfoFilled /></el-icon>
          <span>分享您的推荐链接给好友，好友注册并消费后您将获得订单金额 {{ info.commissionRate || 10 }}% 的佣金奖励，佣金将直接充入您的账户余额。</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">推荐记录</span>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="referredUsername" label="推荐用户" width="160" />
        <el-table-column label="订单金额" width="140" align="right">
          <template #default="{ row }">
            ¥{{ formatMoney(row.orderAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="佣金" width="140" align="right">
          <template #default="{ row }">
            <span style="color: #67C23A; font-weight: 600;">¥{{ formatMoney(row.commission) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadRecords"
          @size-change="loadRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReferralInfo, getReferralRecords } from '@/api/referral'
import { formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'

const info = reactive({
  referralCode: '',
  referralLink: '',
  totalReferrals: 0,
  totalEarned: 0,
  commissionRate: 10
})

const records = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap = {
  pending: { label: '待结算', type: 'warning' },
  paid: { label: '已结算', type: 'success' },
  cancelled: { label: '已取消', type: 'danger' }
}

const formatMoney = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

const copyLink = async () => {
  if (!info.referralLink) {
    ElMessage.warning('推荐链接未生成')
    return
  }
  try {
    await navigator.clipboard.writeText(info.referralLink)
    ElMessage.success('推荐链接已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = info.referralLink
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('推荐链接已复制到剪贴板')
  }
}

const loadInfo = async () => {
  try {
    const res = await getReferralInfo()
    if (res.data) {
      Object.assign(info, res.data)
    }
  } catch {
    // handled by interceptor
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getReferralRecords({
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadInfo()
  loadRecords()
})
</script>

<style lang="scss" scoped>
.referral-page {
  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stats-value {
    font-size: 24px;
    font-weight: 700;
  }

  .stats-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }

  .stats-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .text-primary { color: #409EFF; }
  .text-success { color: #67C23A; }
  .text-warning { color: #E6A23C; }

  .referral-link-section {
    .link-display {
      max-width: 600px;
    }

    .share-tips {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin-top: 16px;
      padding: 12px 16px;
      background-color: #fdf6ec;
      border-radius: 8px;
      font-size: 13px;
      color: #8a6d3b;
      line-height: 1.6;
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
