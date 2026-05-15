<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2>管理面板</h2>
    </div>

    <el-card shadow="hover" class="mb-20">
      <template #header>
        <span class="card-title">系统设置</span>
      </template>
      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">内容审核</div>
          <div class="setting-desc">启用后，所有API请求将进行关键词过滤和注入检测</div>
        </div>
        <el-switch
          v-model="moderationEnabled"
          active-text="开启"
          inactive-text="关闭"
          @change="handleModerationToggle"
        />
      </div>
    </el-card>

    <el-row :gutter="20" class="mb-20">
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-primary">{{ formatTokens(overview.totalUsers) }}</div>
              <div class="stats-label">总用户</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(64, 158, 255, 0.1); color: #409EFF;">
              <el-icon :size="24"><User /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-success">{{ formatTokens(overview.activeUsers) }}</div>
              <div class="stats-label">活跃用户</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(103, 194, 58, 0.1); color: #67C23A;">
              <el-icon :size="24"><UserFilled /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-warning">{{ formatTokens(overview.todayRequests) }}</div>
              <div class="stats-label">今日请求</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(230, 162, 60, 0.1); color: #E6A23C;">
              <el-icon :size="24"><DataLine /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-danger">{{ formatMoney(overview.todayRevenue) }}</div>
              <div class="stats-label">今日收入</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(245, 108, 108, 0.1); color: #F56C6C;">
              <el-icon :size="24"><Wallet /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-info">{{ formatMoney(overview.totalRevenue) }}</div>
              <div class="stats-label">总收入</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(144, 147, 153, 0.1); color: #909399;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value" style="color: #9b59b6;">{{ overview.activeChannels || 0 }}</div>
              <div class="stats-label">活跃渠道</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(155, 89, 182, 0.1); color: #9b59b6;">
              <el-icon :size="24"><Connection /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-20">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">收入趋势</span>
          </template>
          <v-chart :option="revenueChartOption" style="height: 320px;" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">用户增长</span>
          </template>
          <v-chart :option="userGrowthChartOption" style="height: 320px;" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header>
        <span class="card-title">最近错误</span>
      </template>
      <el-table :data="recentErrors" stripe max-height="300">
        <el-table-column prop="channel" label="渠道" width="140" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" width="160" show-overflow-tooltip />
        <el-table-column prop="error" label="错误信息" min-width="250" show-overflow-tooltip />
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboard, getRevenueStats, getUserStats, getRecentErrors, getModerationStatus, toggleModeration } from '@/api/admin'
import { formatMoney, formatTokens, formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const overview = reactive({
  totalUsers: 0,
  activeUsers: 0,
  todayRequests: 0,
  todayRevenue: 0,
  totalRevenue: 0,
  activeChannels: 0
})

const revenueData = ref([])
const userGrowthData = ref([])
const recentErrors = ref([])
const moderationEnabled = ref(false)

const revenueChartOption = computed(() => {
  const dates = revenueData.value.map(item => dayjs(item.date).format('MM/DD'))
  const revenue = revenueData.value.map(item => item.revenue || 0)

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '收入(元)' },
    series: [{
      type: 'line',
      smooth: true,
      data: revenue,
      itemStyle: { color: '#F56C6C' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }
          ]
        }
      }
    }]
  }
})

const userGrowthChartOption = computed(() => {
  const dates = userGrowthData.value.map(item => dayjs(item.date).format('MM/DD'))
  const newUsers = userGrowthData.value.map(item => item.new_users || 0)
  const totalUsers = userGrowthData.value.map(item => item.total_users || 0)

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '总用户'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '新增' },
      { type: 'value', name: '总数' }
    ],
    series: [
      {
        name: '新增用户',
        type: 'bar',
        data: newUsers,
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '总用户',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: totalUsers,
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
})

const loadData = async () => {
  try {
    const [dashRes, revRes, userRes, errRes, modRes] = await Promise.allSettled([
      getDashboard(),
      getRevenueStats({ start_date: dayjs().subtract(14, 'day').format('YYYY-MM-DD'), end_date: dayjs().format('YYYY-MM-DD') }),
      getUserStats({ start_date: dayjs().subtract(14, 'day').format('YYYY-MM-DD'), end_date: dayjs().format('YYYY-MM-DD') }),
      getRecentErrors({ page: 1, page_size: 10 }),
      getModerationStatus()
    ])

    if (dashRes.status === 'fulfilled' && dashRes.value?.data) {
      Object.assign(overview, dashRes.value.data)
    }

    if (revRes.status === 'fulfilled' && revRes.value?.data) {
      revenueData.value = revRes.value.data.list || revRes.value.data || []
    }

    if (userRes.status === 'fulfilled' && userRes.value?.data) {
      userGrowthData.value = userRes.value.data.list || userRes.value.data || []
    }

    if (errRes.status === 'fulfilled' && errRes.value?.data) {
      recentErrors.value = errRes.value.data.list || errRes.value.data || []
    }

    if (modRes.status === 'fulfilled' && modRes.value?.data !== undefined) {
      moderationEnabled.value = modRes.value.data
    }
  } catch (error) {
    // silently handle
  }
}

onMounted(() => {
  loadData()
})

const handleModerationToggle = async (val) => {
  try {
    await toggleModeration(val)
    ElMessage.success(val ? '内容审核已开启' : '内容审核已关闭')
  } catch {
    moderationEnabled.value = !val
  }
}
</script>

<style lang="scss" scoped>
.admin-dashboard {
  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .setting-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;

    .setting-info {
      .setting-label {
        font-size: 15px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .setting-desc {
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-top: 4px;
      }
    }
  }
}
</style>
