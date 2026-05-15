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
              <span v-if="currentSubscription.end_time">到期时间: {{ formatDate(currentSubscription.end_time) }}</span>
              <span v-else>永久有效</span>
            </div>
          </div>
        </div>
        <div class="current-sub-actions">
          <div class="auto-renew-section">
            <span class="auto-renew-label">自动续费</span>
            <el-switch
              v-model="autoRenewEnabled"
              @change="handleAutoRenewChange"
              :loading="autoRenewLoading"
              active-text="已开启"
              inactive-text="已关闭"
            />
          </div>
          <el-button v-if="currentSubscription.plan_name && currentSubscription.plan_name !== '免费版'" type="danger" plain @click="handleCancel">
            取消订阅
          </el-button>
        </div>
      </div>
      <div v-if="autoRenewEnabled && currentSubscription.end_time" class="auto-renew-info">
        <el-icon :size="14" color="#E6A23C"><Warning /></el-icon>
        <span>下次续费日期: {{ formatDate(currentSubscription.end_time) }}，将从余额自动扣费</span>
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

    <el-dialog v-model="payDialogVisible" title="确认订阅" width="480px" @close="handlePayDialogClose">
      <div class="pay-info">
        <div class="pay-plan">
          <span>套餐:</span>
          <strong>{{ selectedPlan?.name }}</strong>
        </div>
        <div class="pay-amount">
          <span>原价:</span>
          <strong>¥{{ selectedPlan?.price }}</strong>
        </div>
        <div class="pay-discount" v-if="discountAmount > 0">
          <span>优惠:</span>
          <strong class="text-success">-¥{{ discountAmount.toFixed(2) }}</strong>
        </div>
        <div class="pay-final">
          <span>实付:</span>
          <strong class="text-danger">¥{{ finalPrice.toFixed(2) }}</strong>
        </div>
      </div>
      <el-form :model="payForm" label-width="90px" class="mt-20">
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.payment_method">
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
            <el-input v-model="payForm.coupon_code" placeholder="可选，输入优惠券码" clearable />
            <el-button type="primary" plain @click="applyCoupon" :loading="couponLoading">验证</el-button>
          </div>
          <div v-if="couponInfo" class="coupon-result text-success">
            优惠券已生效: {{ couponInfo.description || `优惠 ¥${discountAmount.toFixed(2)}` }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="confirmSubscribe">确认支付 ¥{{ finalPrice.toFixed(2) }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrDialogVisible" title="微信支付" width="360px" :close-on-click-modal="false">
      <div class="qr-pay-section">
        <div class="qr-amount">支付金额: <strong class="text-danger">¥{{ finalPrice.toFixed(2) }}</strong></div>
        <div class="qr-placeholder">
          <el-icon :size="120" color="#07c160"><ChatDotRound /></el-icon>
          <p>请使用微信扫描二维码完成支付</p>
        </div>
        <el-button type="primary" class="qr-confirm-btn" @click="handleQrConfirm">我已完成支付</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="autoRenewDialogVisible" title="开启自动续费" width="440px">
      <div class="auto-renew-dialog-content">
        <p>开启自动续费后，系统将在订阅到期前自动从您的余额扣费续期。</p>
        <el-form :model="autoRenewForm" label-width="90px" class="mt-16">
          <el-form-item label="扣费方式">
            <el-radio-group v-model="autoRenewForm.paymentMethod">
              <el-radio value="balance">
                <div class="payment-option">
                  <el-icon :size="18" color="#E6A23C"><Wallet /></el-icon>
                  <span>余额扣费</span>
                </div>
              </el-radio>
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
        </el-form>
        <div v-if="currentSubscription" class="auto-renew-summary">
          <div>续费金额: <strong>¥{{ currentSubscription.plan_price || '0.00' }}</strong></div>
          <div v-if="currentSubscription.end_time">下次续费: {{ formatDate(currentSubscription.end_time) }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="autoRenewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="autoRenewLoading" @click="confirmEnableAutoRenew">确认开启</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listPlans, getCurrentSubscription, subscribe, cancelSubscription, enableAutoRenew, disableAutoRenew } from '@/api/subscription'
import { pay } from '@/api/payment'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const plans = ref([])
const currentSubscription = ref(null)
const payDialogVisible = ref(false)
const payLoading = ref(false)
const selectedPlan = ref(null)
const qrDialogVisible = ref(false)
const couponLoading = ref(false)
const couponInfo = ref(null)
const autoRenewEnabled = ref(false)
const autoRenewLoading = ref(false)
const autoRenewDialogVisible = ref(false)

const autoRenewForm = reactive({
  paymentMethod: 'balance'
})

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

const discountAmount = computed(() => {
  if (!couponInfo.value || !selectedPlan.value) return 0
  if (couponInfo.value.type === 'percent') {
    return selectedPlan.value.price * (couponInfo.value.value / 100)
  }
  return couponInfo.value.value || 0
})

const finalPrice = computed(() => {
  const price = selectedPlan.value?.price || 0
  return Math.max(0, price - discountAmount.value)
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
      autoRenewEnabled.value = subRes.value.data.auto_renew === 1
    }
  } catch (error) {
    // silently handle
  }
}

const handleAutoRenewChange = (val) => {
  if (val) {
    autoRenewEnabled.value = false
    autoRenewForm.paymentMethod = 'balance'
    autoRenewDialogVisible.value = true
  } else {
    handleDisableAutoRenew()
  }
}

const confirmEnableAutoRenew = async () => {
  autoRenewLoading.value = true
  try {
    await enableAutoRenew({ paymentMethod: autoRenewForm.paymentMethod })
    autoRenewEnabled.value = true
    autoRenewDialogVisible.value = false
    ElMessage.success('自动续费已开启')
    loadData()
  } catch (error) {
    autoRenewEnabled.value = false
  } finally {
    autoRenewLoading.value = false
  }
}

const handleDisableAutoRenew = async () => {
  autoRenewLoading.value = true
  try {
    await disableAutoRenew()
    autoRenewEnabled.value = false
    ElMessage.success('自动续费已关闭')
    loadData()
  } catch (error) {
    autoRenewEnabled.value = true
  } finally {
    autoRenewLoading.value = false
  }
}

const handleSubscribe = (plan) => {
  selectedPlan.value = plan
  payForm.payment_method = 'alipay'
  payForm.coupon_code = ''
  couponInfo.value = null
  payDialogVisible.value = true
}

const applyCoupon = async () => {
  if (!payForm.coupon_code) {
    ElMessage.warning('请输入优惠券码')
    return
  }
  couponLoading.value = true
  try {
    const res = await pay({ coupon_code: payForm.coupon_code, action: 'verify' })
    couponInfo.value = res.data || null
    ElMessage.success('优惠券验证成功')
  } catch (error) {
    couponInfo.value = null
  } finally {
    couponLoading.value = false
  }
}

const handlePayDialogClose = () => {
  couponInfo.value = null
}

const confirmSubscribe = async () => {
  payLoading.value = true
  try {
    const res = await subscribe({
      plan_id: selectedPlan.value.id,
      payment_method: payForm.payment_method,
      coupon_code: payForm.coupon_code || undefined
    })
    payDialogVisible.value = false

    if (payForm.payment_method === 'alipay' && res.data?.pay_url) {
      window.open(res.data.pay_url, '_blank')
      ElMessage.success('已跳转至支付宝，请完成支付')
    } else if (payForm.payment_method === 'wechat') {
      qrDialogVisible.value = true
    } else {
      ElMessage.success('订阅成功')
    }
    loadData()
  } catch (error) {
    // handled by interceptor
  } finally {
    payLoading.value = false
  }
}

const handleQrConfirm = async () => {
  qrDialogVisible.value = false
  loadData()
  ElMessage.success('支付结果确认中')
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

      .current-sub-actions {
        display: flex;
        align-items: center;
        gap: 16px;

        .auto-renew-section {
          display: flex;
          align-items: center;
          gap: 8px;

          .auto-renew-label {
            font-size: 14px;
            color: var(--color-text-regular);
            font-weight: 500;
          }
        }
      }
    }

    .auto-renew-info {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #f0f0f0;
      font-size: 13px;
      color: var(--color-text-secondary);
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
    .pay-plan, .pay-amount, .pay-discount, .pay-final {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      font-size: 15px;
    }

    .pay-final {
      border-top: 1px solid #f0f0f0;
      margin-top: 4px;
      padding-top: 12px;
      font-size: 16px;
    }
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

  .qr-pay-section {
    text-align: center;

    .qr-amount {
      font-size: 16px;
      margin-bottom: 20px;
    }

    .qr-placeholder {
      padding: 30px 0;

      p {
        margin-top: 16px;
        font-size: 14px;
        color: var(--color-text-secondary);
      }
    }

    .qr-confirm-btn {
      width: 100%;
      margin-top: 20px;
    }
  }

  .auto-renew-dialog-content {
    p {
      font-size: 14px;
      color: var(--color-text-regular);
      line-height: 1.6;
    }

    .auto-renew-summary {
      margin-top: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 8px;
      font-size: 14px;

      div {
        margin-bottom: 4px;

        strong {
          color: var(--color-primary);
        }
      }
    }
  }
}
</style>
