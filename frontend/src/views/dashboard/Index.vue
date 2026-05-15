<template>
  <div class="dashboard-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-warning">{{ formatMoney(userStore.balance) }}</div>
              <div class="stats-label">账户余额</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(230, 162, 60, 0.1); color: #E6A23C;">
              <el-icon :size="24"><Wallet /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-primary">{{ formatTokens(usageStats.totalTokens) }}</div>
              <div class="stats-label">已用Token</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(64, 158, 255, 0.1); color: #409EFF;">
              <el-icon :size="24"><Coin /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-success">{{ formatTokens(usageStats.monthlyRequests) }}</div>
              <div class="stats-label">本月请求</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(103, 194, 58, 0.1); color: #67C23A;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-info">{{ subscriptionInfo.planName || '免费版' }}</div>
              <div class="stats-label">当前套餐</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(144, 147, 153, 0.1); color: #909399;">
              <el-icon :size="24"><Tickets /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">近7天用量趋势</span>
            </div>
          </template>
          <v-chart :option="usageChartOption" style="height: 350px;" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="$router.push('/billing')">
              <el-icon :size="28" color="#E6A23C"><Wallet /></el-icon>
              <span>充值</span>
            </div>
            <div class="action-item" @click="$router.push('/apikeys')">
              <el-icon :size="28" color="#409EFF"><Key /></el-icon>
              <span>创建API Key</span>
            </div>
            <div class="action-item" @click="$router.push('/monitor')">
              <el-icon :size="28" color="#67C23A"><DataLine /></el-icon>
              <span>查看监控</span>
            </div>
            <div class="action-item" @click="$router.push('/subscription')">
              <el-icon :size="28" color="#909399"><Tickets /></el-icon>
              <span>升级套餐</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="mt-20">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近活动</span>
            </div>
          </template>
          <div class="activity-list">
            <div v-for="item in recentActivities" :key="item.id" class="activity-item">
              <div class="activity-dot" :class="item.type"></div>
              <div class="activity-content">
                <div class="activity-text">{{ item.description }}</div>
                <div class="activity-time">{{ formatDate(item.created_at) }}</div>
              </div>
            </div>
            <el-empty v-if="recentActivities.length === 0" description="暂无活动记录" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useUserStore } from '@/stores/user'
import { getOverview } from '@/api/billing'
import { getCurrentSubscription } from '@/api/subscription'
import { getDailyUsage } from '@/api/monitor'
import { getTransactions } from '@/api/billing'
import { formatMoney, formatTokens, formatDate } from '@/utils/format'
import dayjs from 'dayjs'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const userStore = useUserStore()

const usageStats = reactive({
  totalTokens: 0,
  monthlyRequests: 0
})

const subscriptionInfo = reactive({
  planName: ''
})

const recentActivities = ref([])
const dailyData = ref([])

const usageChartOption = computed(() => {
  const dates = dailyData.value.map(item => item.date)
  const requests = dailyData.value.map(item => item.request_count || 0)
  const tokens = dailyData.value.map(item => item.token_count || 0)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['请求数', 'Token数'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        formatter: (val) => dayjs(val).format('MM/DD')
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '请求数',
        position: 'left'
      },
      {
        type: 'value',
        name: 'Token数',
        position: 'right'
      }
    ],
    series: [
      {
        name: '请求数',
        type: 'line',
        smooth: true,
        data: requests,
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
            ]
          }
        }
      },
      {
        name: 'Token数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: tokens,
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
            ]
          }
        }
      }
    ]
  }
})

const loadData = async () => {
  try {
    const [overviewRes, subRes, dailyRes, txRes] = await Promise.allSettled([
      getOverview(),
      getCurrentSubscription(),
      getDailyUsage({ start_date: dayjs().subtract(7, 'day').format('YYYY-MM-DD'), end_date: dayjs().format('YYYY-MM-DD') }),
      getTransactions({ page: 1, page_size: 5 })
    ])

    if (overviewRes.status === 'fulfilled' && overviewRes.value?.data) {
      usageStats.totalTokens = overviewRes.value.data.total_tokens || 0
      usageStats.monthlyRequests = overviewRes.value.data.monthly_requests || 0
    }

    if (subRes.status === 'fulfilled' && subRes.value?.data) {
      subscriptionInfo.planName = subRes.value.data.plan_name || ''
    }

    if (dailyRes.status === 'fulfilled' && dailyRes.value?.data) {
      dailyData.value = dailyRes.value.data.list || dailyRes.value.data || []
    }

    if (txRes.status === 'fulfilled' && txRes.value?.data) {
      const list = txRes.value.data.list || txRes.value.data || []
      recentActivities.value = list.map((item, idx) => ({
        id: idx,
        description: item.description || item.type || '交易记录',
        type: item.type === 'recharge' ? 'success' : item.type === 'consume' ? 'primary' : 'info',
        created_at: item.created_at
      }))
    }
  } catch (error) {
    // silently handle
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
    }
  }

  .quick-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px 12px;
      border-radius: 8px;
      background-color: #f5f7fa;
      cursor: pointer;
      transition: all 0.3s ease;
      gap: 10px;

      span {
        font-size: 13px;
        color: var(--color-text-regular);
      }

      &:hover {
        background-color: #ecf5ff;
        transform: translateY(-2px);
      }
    }
  }

  .activity-list {
    .activity-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .activity-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-top: 6px;
        flex-shrink: 0;

        &.success { background-color: var(--color-success); }
        &.primary { background-color: var(--color-primary); }
        &.warning { background-color: var(--color-warning); }
        &.info { background-color: var(--color-info); }
      }

      .activity-content {
        flex: 1;

        .activity-text {
          font-size: 14px;
          color: var(--color-text-primary);
        }

        .activity-time {
          font-size: 12px;
          color: var(--color-text-secondary);
          margin-top: 4px;
        }
      }
    }
  }
}
</style>
