import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'apikeys',
        name: 'ApiKeys',
        component: () => import('@/views/apikey/Index.vue'),
        meta: { title: 'API密钥' }
      },
      {
        path: 'subscription',
        name: 'Subscription',
        component: () => import('@/views/subscription/Index.vue'),
        meta: { title: '订阅方案' }
      },
      {
        path: 'billing',
        name: 'Billing',
        component: () => import('@/views/billing/Index.vue'),
        meta: { title: '账单' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/Index.vue'),
        meta: { title: '用量监控' }
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('@/views/coupon/Index.vue'),
        meta: { title: '优惠券' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '管理面板' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'channels',
        name: 'AdminChannels',
        component: () => import('@/views/admin/Channels.vue'),
        meta: { title: '渠道管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'coupons',
        name: 'AdminCoupons',
        component: () => import('@/views/admin/Coupons.vue'),
        meta: { title: '优惠券管理' }
      },
      {
        path: 'model-prices',
        name: 'AdminModelPrices',
        component: () => import('@/views/admin/ModelPrices.vue'),
        meta: { title: '模型定价' }
      },
      {
        path: 'monitor',
        name: 'AdminMonitor',
        component: () => import('@/views/admin/Monitor.vue'),
        meta: { title: '系统监控' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  document.title = to.meta.title ? `${to.meta.title} - AI Relay` : 'AI Relay - AI大模型中转平台'

  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && token) {
    next({ name: 'Dashboard' })
  } else if (to.meta.requiresAdmin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.role !== 'admin') {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
