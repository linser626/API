<template>
  <div class="coupon-page">
    <div class="page-header">
      <h2>优惠券</h2>
    </div>

    <el-card shadow="hover" class="mb-20">
      <div class="redeem-section">
        <div class="redeem-info">
          <el-icon :size="24" color="#E6A23C"><Present /></el-icon>
          <div>
            <div class="redeem-title">兑换优惠券</div>
            <div class="redeem-desc">输入优惠券码即可兑换相应优惠</div>
          </div>
        </div>
        <div class="redeem-action">
          <el-input
            v-model="couponCode"
            placeholder="请输入优惠券码"
            style="width: 280px;"
            clearable
            @keyup.enter="handleRedeem"
          />
          <el-button type="primary" :loading="redeemLoading" @click="handleRedeem">
            兑换
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <span class="card-title">我的优惠券</span>
      </template>

      <div v-if="coupons.length > 0" class="coupon-list">
        <div v-for="coupon in coupons" :key="coupon.id" class="coupon-item" :class="coupon.status">
          <div class="coupon-left">
            <div class="coupon-value">
              <template v-if="coupon.type === 'percentage'">
                <span class="value-number">{{ coupon.value }}</span>
                <span class="value-unit">%</span>
              </template>
              <template v-else>
                <span class="value-symbol">¥</span>
                <span class="value-number">{{ coupon.value }}</span>
              </template>
            </div>
            <div class="coupon-condition">
              {{ coupon.min_amount > 0 ? `满${coupon.min_amount}元可用` : '无门槛' }}
            </div>
          </div>
          <div class="coupon-right">
            <div class="coupon-name">{{ coupon.name || '优惠券' }}</div>
            <div class="coupon-desc">{{ coupon.description || '' }}</div>
            <div class="coupon-time">
              {{ formatDate(coupon.start_at) }} - {{ formatDate(coupon.end_at) }}
            </div>
            <el-tag
              :type="couponStatusMap[coupon.status]?.type || 'info'"
              size="small"
              class="coupon-status"
            >
              {{ couponStatusMap[coupon.status]?.label || coupon.status }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无优惠券" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { redeemCoupon, getMyCoupons } from '@/api/coupon'
import { formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'

const couponCode = ref('')
const redeemLoading = ref(false)
const coupons = ref([])

const couponStatusMap = {
  unused: { label: '未使用', type: 'success' },
  used: { label: '已使用', type: 'info' },
  expired: { label: '已过期', type: 'danger' }
}

const handleRedeem = async () => {
  if (!couponCode.value.trim()) {
    ElMessage.warning('请输入优惠券码')
    return
  }
  redeemLoading.value = true
  try {
    await redeemCoupon({ code: couponCode.value.trim() })
    ElMessage.success('优惠券兑换成功')
    couponCode.value = ''
    loadCoupons()
  } catch (error) {
    // handled by interceptor
  } finally {
    redeemLoading.value = false
  }
}

const loadCoupons = async () => {
  try {
    const res = await getMyCoupons({ page: 1, page_size: 50 })
    coupons.value = res.data?.list || res.data || []
  } catch (error) {
    // handled by interceptor
  }
}

onMounted(() => {
  loadCoupons()
})
</script>

<style lang="scss" scoped>
.coupon-page {
  .redeem-section {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .redeem-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .redeem-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .redeem-desc {
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-top: 4px;
      }
    }

    .redeem-action {
      display: flex;
      gap: 12px;
    }
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .coupon-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .coupon-item {
    display: flex;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e4e7ed;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    &.unused {
      .coupon-left {
        background: linear-gradient(135deg, #409EFF, #66b1ff);
      }
    }

    &.used {
      .coupon-left {
        background: linear-gradient(135deg, #909399, #b1b3b8);
      }
    }

    &.expired {
      .coupon-left {
        background: linear-gradient(135deg, #F56C6C, #fab6b6);
      }
    }

    .coupon-left {
      width: 140px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #fff;

      .coupon-value {
        .value-symbol {
          font-size: 16px;
        }

        .value-number {
          font-size: 32px;
          font-weight: 700;
        }

        .value-unit {
          font-size: 16px;
        }
      }

      .coupon-condition {
        font-size: 12px;
        margin-top: 4px;
        opacity: 0.9;
      }
    }

    .coupon-right {
      flex: 1;
      padding: 16px 20px;
      position: relative;

      .coupon-name {
        font-size: 16px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .coupon-desc {
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-top: 4px;
      }

      .coupon-time {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 8px;
      }

      .coupon-status {
        position: absolute;
        top: 16px;
        right: 20px;
      }
    }
  }
}
</style>
