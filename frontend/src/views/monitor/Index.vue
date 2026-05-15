<template>
  <div class="monitor-page">
    <div class="page-header flex-between">
      <h2>用量监控</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="loadData"
        />
        <el-button type="success" plain @click="handleExportUsage" :loading="exportLoading">
          <el-icon><Download /></el-icon>
          导出CSV
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-primary">{{ formatTokens(stats.totalRequests) }}</div>
              <div class="stats-label">总请求数</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(64, 158, 255, 0.1); color: #409EFF;">
              <el-icon :size="24"><DataLine /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-success">{{ formatTokens(stats.totalTokens) }}</div>
              <div class="stats-label">总Token数</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(103, 194, 58, 0.1); color: #67C23A;">
              <el-icon :size="24"><Coin /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-warning">{{ formatMoney(stats.totalCost) }}</div>
              <div class="stats-label">总费用</div>
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
              <div class="stats-value text-info">{{ stats.avgLatency || 0 }}ms</div>
              <div class="stats-label">平均延迟</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(144, 147, 153, 0.1); color: #909399;">
              <el-icon :size="24"><Timer /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="mb-20">
      <template #header>
        <span class="card-title">每日用量趋势</span>
      </template>
      <v-chart :option="dailyChartOption" style="height: 350px;" autoresize />
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">模型用量分布</span>
          </template>
          <el-table :data="modelUsage" stripe>
            <el-table-column prop="model" label="模型" min-width="150" />
            <el-table-column label="请求数" width="120" align="center">
              <template #default="{ row }">
                {{ formatTokens(row.request_count) }}
              </template>
            </el-table-column>
            <el-table-column label="Token数" width="120" align="center">
              <template #default="{ row }">
                {{ formatTokens(row.token_count) }}
              </template>
            </el-table-column>
            <el-table-column label="费用" width="120" align="center">
              <template #default="{ row }">
                {{ formatMoney(row.cost) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="flex-between">
              <span class="card-title">请求日志</span>
              <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px;" @change="loadData">
                <el-option label="全部" value="" />
                <el-option label="成功" value="success" />
                <el-option label="失败" value="error" />
              </el-select>
            </div>
          </template>
          <el-table :data="requestLogs" stripe max-height="400">
            <el-table-column prop="model" label="模型" width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'success' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Token" width="100" align="center">
              <template #default="{ row }">
                {{ formatTokens(row.token_count) }}
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="80" align="center">
              <template #default="{ row }">
                {{ row.latency || 0 }}ms
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160" align="center">
              <template #default="{ row }">
                {{ formatDate(row.created_at) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getUsageStats, getModelUsage, getDailyUsage, exportUsage } from '@/api/monitor'
import { formatMoney, formatTokens, formatDate, downloadFile } from '@/utils/format'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const dateRange = ref([
  dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])
const statusFilter = ref('')
const exportLoading = ref(false)

const stats = reactive({
  totalRequests: 0,
  totalTokens: 0,
  totalCost: 0,
  avgLatency: 0
})

const modelUsage = ref([])
const dailyData = ref([])
const requestLogs = ref([])

const dailyChartOption = computed(() => {
  const dates = dailyData.value.map(item => dayjs(item.date).format('MM/DD'))
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
      data: dates
    },
    yAxis: [
      { type: 'value', name: '请求数' },
      { type: 'value', name: 'Token数' }
    ],
    series: [
      {
        name: '请求数',
        type: 'bar',
        data: requests,
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: 'Token数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: tokens,
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
})

const loadData = async () => {
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.start_date = dateRange.value[0]
    params.end_date = dateRange.value[1]
  }
  if (statusFilter.value) {
    params.status = statusFilter.value
  }

  try {
    const [statsRes, modelRes, dailyRes] = await Promise.allSettled([
      getUsageStats(params),
      getModelUsage(params),
      getDailyUsage(params)
    ])

    if (statsRes.status === 'fulfilled' && statsRes.value?.data) {
      stats.totalRequests = statsRes.value.data.total_requests || 0
      stats.totalTokens = statsRes.value.data.total_tokens || 0
      stats.totalCost = statsRes.value.data.total_cost || 0
      stats.avgLatency = statsRes.value.data.avg_latency || 0
      requestLogs.value = statsRes.value.data.recent_logs || []
    }

    if (modelRes.status === 'fulfilled' && modelRes.value?.data) {
      modelUsage.value = modelRes.value.data.list || modelRes.value.data || []
    }

    if (dailyRes.status === 'fulfilled' && dailyRes.value?.data) {
      dailyData.value = dailyRes.value.data.list || dailyRes.value.data || []
    }
  } catch (error) {
    // silently handle
  }
}

const handleExportUsage = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + 'T00:00:00'
      params.endTime = dateRange.value[1] + 'T23:59:59'
    }

    const response = await exportUsage(params)
    const blob = new Blob([response], { type: 'text/csv;charset=utf-8' })
    const filename = `export_usage_${dayjs().format('YYYYMMDD')}.csv`
    downloadFile(blob, filename)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.monitor-page {
  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
