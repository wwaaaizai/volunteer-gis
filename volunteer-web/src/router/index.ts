import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: '',
        name: 'Map',
        component: () => import('@/views/Map.vue'),
        meta: { title: '活动地图' },
      },
      {
        path: 'activity/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/ActivityDetail.vue'),
        meta: { title: '活动详情' },
      },
      {
        path: 'my-signups',
        name: 'MySignups',
        component: () => import('@/views/MySignups.vue'),
        meta: { title: '我的报名' },
      },
      {
        path: 'checkin',
        name: 'CheckIn',
        component: () => import('@/views/CheckIn.vue'),
        meta: { title: '签到' },
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/Admin.vue'),
        meta: { title: '管理员后台', roles: ['admin'] },
      },
      {
        path: 'admin/create-activity',
        name: 'CreateActivity',
        component: () => import('@/views/CreateActivity.vue'),
        meta: { title: '创建活动', roles: ['admin', 'organizer'] },
      },
      {
        path: 'admin/geofence/:id',
        name: 'GeofenceEdit',
        component: () => import('@/views/GeofenceEdit.vue'),
        meta: { title: '签到围栏', roles: ['admin', 'organizer'] },
      },
      {
        path: 'organizer',
        name: 'OrganizerDashboard',
        component: () => import('@/views/OrganizerDashboard.vue'),
        meta: { title: '组织者后台', roles: ['organizer', 'admin'] },
      },
      {
        path: 'organizer/create',
        name: 'OrganizerCreateActivity',
        component: () => import('@/views/CreateActivity.vue'),
        meta: { title: '创建活动', roles: ['organizer', 'admin'] },
      },
      {
        path: 'organizer/activity/:id',
        name: 'OrganizerActivityDetail',
        component: () => import('@/views/OrganizerActivityDetail.vue'),
        meta: { title: '活动详情', roles: ['organizer', 'admin'] },
      },
      {
        path: 'organizer/geofence/:id',
        name: 'OrganizerGeofenceEdit',
        component: () => import('@/views/GeofenceEdit.vue'),
        meta: { title: '签到围栏', roles: ['organizer', 'admin'] },
      },
      {
        path: 'organizer/profile',
        name: 'OrganizerProfile',
        component: () => import('@/views/OrganizerProfile.vue'),
        meta: { title: '个人信息', roles: ['organizer', 'admin'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 先尝试获取用户信息
  if (userStore.token && !userStore.user) {
    await userStore.fetchUser()
  }

  // P2-UPM-05：统一使用 meta.roles 数组校验角色权限
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const userRole = userStore.user?.role || ''
    if (!requiredRoles.includes(userRole)) {
      return next('/')
    }
  }

  // 已登录用户访问登录/注册页 → 重定向到首页
  if (to.meta.guest && userStore.user) {
    return next('/')
  }

  next()
})

export default router
