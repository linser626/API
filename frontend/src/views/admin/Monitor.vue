<template>
  <div class="admin-monitor">
    <div class="page-header">
      <h2>系统监控</h2>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-success">{{ healthStatus.uptime || '0%' }}</div>
              <div class="stats-label">系统可用率</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(103, 194, 58, 0.1); color: #67C23A;">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-primary">{{ healthStatus.activeChannels || 0 }}</div>
              <div class="stats-label">活跃渠道</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(64, 158, 255, 0.1); color: #409EFF;">
              <el-icon :size="24"><Connection /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-warning">{{ healthStatus.avgLatency || 0 }}ms</div>
              <div class="stats-label">平均延迟</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(230, 162, 60, 0.1); color: #E6A23C;">
              <el-icon :size="24"><Timer /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div>
              <div class="stats-value text-danger">{{ healthStatus.errorCount || 0 }}</div>
              <div class="stats-label">近1小时错误</div>
            </div>
            <div class="stats-icon" style="background-color: rgba(245, 108, 108, 0.1); color: #F56C6C;">
              <el-icon :size="24"><WarningFilled /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="mb-20">
      <template #header>
        <span class="card-title">渠道性能</span>
      </template>
      <el-table :data="channelStats" v-loading="channelLoading" stripe>
        <el-table-column prop="name" label="渠道名称" width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="110" align="center">
          <template #default="{ row }">
            <span :class="row.success_rate >= 95 ? 'text-success' : row.success_rate >= 80 ? 'text-warning' : 'text-danger'">
              {{ formatPercent(row.success_rate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="平均延迟" width="110" align="center">
          <template #default="{ row }">
            <span :class="row.avg_latency <= 1000 ? 'text-success' : row.avg_latency <= 3000 ? 'text-warning' : 'text-danger'">
              {{ row.avg_latency || 0 }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column label="总请求数" width="120" align="center">
          <template #default="{ row }">
            {{ formatTokens(row.total_requests) }}
          </template>
        </el-table-column>
        <el-table-column label="失败请求数" width="120" align="center">
          <template #default="{ row }">
            <span :class="row.failed_requests > 0 ? 'text-danger' : ''">{{ row.failed_requests || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="last_error" label="最近错误" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.last_error || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="flex-between">
          <span class="card-title">最近错误日志</span>
          <el-button type="primary" link @click="loadErrors">刷新</el-button>
        </div>
      </template>
      <el-table :data="errorLogs" v-loading="errorLoading" stripe max-height="400">
        <el-table-column prop="channel" label="渠道" width="140" show-overflow-tooltip />
        <el-table-column prop="model" label="模型" width="160" show-overflow-tooltip />
        <el-table-column prop="error_type" label="错误类型" width="140" show-overflow-tooltip />
        <el-table-column prop="error_message" label="错误信息" min-width="250" show-overflow-tooltip />
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="errorPagination.page"
          v-model:page-size="errorPagination.page_size"
          :total="errorPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadErrors"
          @current-change="loadErrors"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getChannelStats, getRecentErrors } from '@/api/admin'
import { formatPercent, formatTokens, formatDate } from '@/utils/format'

const healthStatus = reactive({
  uptime: '99.9%',
  activeChannels: 0,
  avgLatency: 0,
  errorCount: 0
})

const channelLoading = ref(false)
const channelStats = ref([])

const errorLoading = ref(false)
const errorLogs = ref([])
const errorPagination = reactive({
  page: 1,
  page_size: 20,
  total: 0
})

const loadChannelStats = async () => {
  channelLoading.value = true
  try {
    const res = await getChannelStats()
    channelStats.value = res.data?.list || res.data || []
    if (res.data) {
      healthStatus.activeChannels = channelStats.value.filter(c => c.status === 'active').length
      const totalRequests = channelStats.value.reduce((sum, c) => sum + (c.total_requests || 0), 0)
      const totalLatency = channelStats.value.reduce((sum, c) => sum + (c.avg_latency || 0), 0)
      healthStatus.avgLatency = channelStats.value.length > 0 ? Math.round(totalLatency / channelStats.value.length) : 0
      healthStatus.errorCount = channelStats.value.reduce((sum, c) => sum + (c.failed_requests || 0), 0)
    }
  } catch (error) {
    // handled by interceptor
  } finally {
    channelLoading.value = false
  }
}

const loadErrors = async () => {
  errorLoading.value = true
  try {
    const res = await getRecentErrors({
      page: errorPagination.page,
      page_size: errorPagination.page_size
    })
    errorLogs.value = res.data?.list || res.data || []
    errorPagination.total = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    errorLoading.value = false
  }
}

onMounted(() => {
  loadChannelStats()
  loadErrors()
})
</script>

<style lang="scss" scoped>
.admin-monitor {
  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
