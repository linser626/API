<template>
  <div class="subscription-page">
    <div class="page-header">
      <h2>订阅方案</h2>
    </div>

    <el-card v-if="currentSubscription" shadow="hover" class="mb-20 current-sub-card">
      <div class="current-sub">
        <div class="current-sub-info">
          <el-icon :size="24" color="#409EFF"><Tickets /></el-icon>
          <div>
            <div class="sub-name">当前套餐: {{ currentSubscription.plan_name || '免费版' }}</div>
            <div class="sub-detail">
              <span v-if="currentSubscription.expires_at">到期时间: {{ formatDate(currentSubscription.expires_at) }}</span>
              <span v-else>永久有效</span>
            </div>
          </div>
        </div>
        <el-button v-if="currentSubscription.plan_name && currentSubscription.plan_name !== '免费版'" type="danger" plain @click="handleCancel">
          取消订阅
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col v-for="plan in plans" :key="plan.id" :xs="24" :sm="12" :lg="6">
        <el-card
          shadow="hover"
          class="plan-card"
          :class="{ active: currentSubscription?.plan_name === plan.name }"
        >
          <div class="plan-header">
            <div class="plan-icon">
              <el-icon :size="32">
                <component :is="planIcons[plan.name] || 'Ticket'" />
              </el-icon>
            </div>
            <h3 class="plan-name">{{ plan.name }}</h3>
            <div class="plan-price">
              <span class="price-amount">¥{{ plan.price }}</span>
              <span class="price-period">/月</span>
            </div>
          </div>
          <div class="plan-features">
            <div v-for="(feature, idx) in plan.features || defaultFeatures" :key="idx" class="feature-item">
              <el-icon color="#67C23A"><Check /></el-icon>
              <span>{{ feature }}</span>
            </div>
          </div>
          <el-button
            :type="currentSubscription?.plan_name === plan.name ? 'info' : 'primary'"
            :disabled="currentSubscription?.plan_name === plan.name"
            class="subscribe-btn"
            @click="handleSubscribe(plan)"
          >
            {{ currentSubscription?.plan_name === plan.name ? '当前套餐' : '立即订阅' }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="payDialogVisible" title="确认订阅" width="480px">
      <div class="pay-info">
        <div class="pay-plan">
          <span>套餐:</span>
          <strong>{{ selectedPlan?.name }}</strong>
        </div>
        <div class="pay-amount">
          <span>金额:</span>
          <strong class="text-danger">¥{{ selectedPlan?.price }}</strong>
        </div>
      </div>
      <el-form :model="payForm" label-width="90px" class="mt-20">
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.payment_method">
            <el-radio value="alipay">支付宝</el-radio>
            <el-radio value="wechat">微信支付</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠券码">
          <el-input v-model="payForm.coupon_code" placeholder="可选，输入优惠券码" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="confirmSubscribe">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listPlans, getCurrentSubscription, subscribe, cancelSubscription } from '@/api/subscription'
import { pay } from '@/api/payment'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const plans = ref([])
const currentSubscription = ref(null)
const payDialogVisible = ref(false)
const payLoading = ref(false)
const selectedPlan = ref(null)

const planIcons = {
  '免费版': 'Ticket',
  '基础版': 'Medal',
  '专业版': 'Trophy',
  '企业版': 'Star'
}

const defaultFeatures = [
  'GPT-3.5 模型访问',
  '基础速率限制',
  '社区支持'
]

const payForm = reactive({
  payment_method: 'alipay',
  coupon_code: ''
})

const loadData = async () => {
  try {
    const [plansRes, subRes] = await Promise.allSettled([
      listPlans(),
      getCurrentSubscription()
    ])

    if (plansRes.status === 'fulfilled' && plansRes.value?.data) {
      plans.value = plansRes.value.data.list || plansRes.value.data || []
    }

    if (subRes.status === 'fulfilled' && subRes.value?.data) {
      currentSubscription.value = subRes.value.data
    }
  } catch (error) {
    // silently handle
  }
}

const handleSubscribe = (plan) => {
  selectedPlan.value = plan
  payForm.payment_method = 'alipay'
  payForm.coupon_code = ''
  payDialogVisible.value = true
}

const confirmSubscribe = async () => {
  payLoading.value = true
  try {
    await subscribe({
      plan_id: selectedPlan.value.id,
      payment_method: payForm.payment_method,
      coupon_code: payForm.coupon_code || undefined
    })
    ElMessage.success('订阅成功')
    payDialogVisible.value = false
    loadData()
  } catch (error) {
    // handled by interceptor
  } finally {
    payLoading.value = false
  }
}

const handleCancel = () => {
  ElMessageBox.confirm('确定要取消当前订阅吗？取消后将在到期日停止服务。', '取消订阅', {
    confirmButtonText: '确定取消',
    cancelButtonText: '保留订阅',
    type: 'warning'
  }).then(async () => {
    try {
      await cancelSubscription()
      ElMessage.success('订阅已取消')
      loadData()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.subscription-page {
  .current-sub-card {
    .current-sub {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .current-sub-info {
        display: flex;
        align-items: center;
        gap: 16px;

        .sub-name {
          font-size: 16px;
          font-weight: 600;
          color: var(--color-text-primary);
        }

        .sub-detail {
          font-size: 13px;
          color: var(--color-text-secondary);
          margin-top: 4px;
        }
      }
    }
  }

  .plan-card {
    text-align: center;
    transition: all 0.3s ease;
    margin-bottom: 20px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    &.active {
      border: 2px solid var(--color-primary);
    }

    .plan-header {
      padding: 20px 0;

      .plan-icon {
        margin-bottom: 12px;
        color: var(--color-primary);
      }

      .plan-name {
        font-size: 20px;
        font-weight: 700;
        color: var(--color-text-primary);
        margin-bottom: 12px;
      }

      .plan-price {
        .price-amount {
          font-size: 36px;
          font-weight: 700;
          color: var(--color-primary);
        }

        .price-period {
          font-size: 14px;
          color: var(--color-text-secondary);
        }
      }
    }

    .plan-features {
      text-align: left;
      padding: 16px 0;
      border-top: 1px solid #f0f0f0;
      border-bottom: 1px solid #f0f0f0;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 0;
        font-size: 14px;
        color: var(--color-text-regular);
      }
    }

    .subscribe-btn {
      width: 100%;
      margin-top: 20px;
      height: 40px;
    }
  }

  .pay-info {
    .pay-plan, .pay-amount {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      font-size: 15px;
    }
  }
}
</style>
