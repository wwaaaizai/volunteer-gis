<template>
  <el-container class="layout" :class="{ 'layout--mobile': appStore.isMobile }">
    <!-- 桌面端：水平导航栏 -->
    <el-header v-if="!appStore.isMobile" class="header">
      <div class="header-left">
        <span class="title">志愿活动系统</span>
      </div>
      <div class="header-right">
        <el-menu mode="horizontal" :default-active="currentPath" :ellipsis="false" class="header-menu">
          <el-menu-item
            v-for="item in navItems"
            :key="item.index"
            :index="item.index"
            @click="navigateTo(item)"
          >{{ item.label }}</el-menu-item>
        </el-menu>
        <el-dropdown>
          <span class="user-info">{{ userName }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-main class="main-content" :class="{ 'main-content--mobile': appStore.isMobile }">
      <router-view />
    </el-main>

    <!-- 移动端：底部两Tab导航栏 -->
    <div v-if="appStore.isMobile" class="mobile-tabs">
      <div
        class="tab-item"
        :class="{ active: isMapActive }"
        @click="router.push('/')"
      >
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
          <line x1="8" y1="2" x2="8" y2="18" />
          <line x1="16" y1="6" x2="16" y2="22" />
        </svg>
        <span class="tab-label">活动地图</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: isMyActive }"
        @click="router.push('/my')"
      >
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span class="tab-label">我的</span>
      </div>
    </div>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

interface NavItem {
  index: string
  path: string
  label: string
  query?: Record<string, string>
}

/** 桌面端导航菜单项（按角色区分） */
const navItems = computed<NavItem[]>(() => {
  const role = userStore.user?.role
  if (role === 'student') return [
    { index: '/', path: '/', label: '活动地图' },
    { index: '/course-schedule', path: '/course-schedule', label: '课程表' },
    { index: '/my-signups', path: '/my-signups', label: '我的报名' },
    { index: '/my-footprint', path: '/my-footprint', label: '志愿足迹' },
    { index: '/my', path: '/my', label: '我的' },
  ]
  if (role === 'organizer') return [
    { index: '/', path: '/', label: '活动地图' },
    { index: '/organizer', path: '/organizer', label: '活动管理' },
    { index: '/organizer/profile', path: '/organizer/profile', label: '组织信息' },
  ]
  if (role === 'admin') return [
    { index: '/', path: '/', label: '活动地图' },
    { index: '/admin', path: '/admin', label: '后台管理' },
    { index: '/organizer', path: '/organizer', label: '活动管理' },
  ]
  return [{ index: '/', path: '/', label: '活动地图' }]
})

/** 当前激活的菜单 index（admin 下包含 query 参数以区分 Tab） */
const currentPath = computed(() => {
  if (route.path === '/admin') return '/admin'
  return route.path
})

function navigateTo(item: NavItem) {
  if (item.query) {
    router.push({ path: item.path, query: item.query })
  } else {
    router.push(item.path)
  }
}

const userName = computed(() => userStore.user?.name || userStore.user?.studentId || '用户')

const isMapActive = computed(() => route.path === '/')
const isMyActive = computed(() => route.path.startsWith('/my'))

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

/* ──── 桌面端头部 ─────────────────────────────── */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
.header-menu {
  border-bottom: none !important;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-info {
  cursor: pointer;
  color: #409eff;
}
.main-content {
  padding: 0;
}

/* ──── 移动端布局 ───────────────────────────── */
.layout--mobile {
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.main-content--mobile {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ──── 移动端底部Tab导航栏 ─────────────────────── */
.mobile-tabs {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1;
  height: 100%;
  cursor: pointer;
  color: #909399;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  transition: color 0.15s;
}
.tab-item.active {
  color: #409eff;
}
.tab-item:active {
  opacity: 0.7;
}

.tab-label {
  font-size: 10px;
  line-height: 1;
}
</style>
