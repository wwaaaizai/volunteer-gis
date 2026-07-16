<template>
  <div class="my-page">
    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-avatar">
        <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </div>
      <div class="user-text">
        <div class="user-name">{{ userName }}</div>
        <div class="user-role">{{ roleLabel }}</div>
      </div>
      <div class="user-hours">{{ totalHoursText }}</div>
    </div>

    <!-- 功能菜单（按角色） -->
    <div class="menu-section">
      <!-- ===== 学生 ===== -->
      <template v-if="role === 'student'">
        <div class="menu-item" @click="router.push('/my-signups')">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="8" y1="9" x2="16" y2="9" />
              <line x1="8" y1="13" x2="16" y2="13" />
              <line x1="8" y1="17" x2="12" y2="17" />
            </svg>
          </span>
          <span class="menu-label">我的报名</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
        <div class="menu-item" @click="router.push('/checkin')">
          <span class="menu-icon" style="color:#67c23a">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M9 11l3 3L22 4" />
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
            </svg>
          </span>
          <span class="menu-label">签到 / 签退</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
        <div class="menu-item" @click="router.push('/my-footprint')">
          <span class="menu-icon" style="color:#e6a23c">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="4" r="1.5" />
              <circle cx="4.5" cy="9.5" r="2.5" />
              <circle cx="20" cy="10" r="1.5" />
              <circle cx="19.5" cy="14" r="2.5" />
              <path d="M8 20c1.5-2.5 3.5-4 5.5-4s3 1.5 4.5 4" />
            </svg>
          </span>
          <span class="menu-label">志愿足迹</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
      </template>

      <!-- ===== 组织者 ===== -->
      <template v-if="role === 'organizer'">
        <div class="menu-item" @click="router.push('/organizer')">
          <span class="menu-icon" style="color:#409eff">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="8" y1="9" x2="16" y2="9" />
              <line x1="8" y1="13" x2="16" y2="13" />
              <line x1="8" y1="17" x2="12" y2="17" />
            </svg>
          </span>
          <span class="menu-label">活动管理</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
        <div class="menu-item" @click="router.push('/organizer/profile')">
          <span class="menu-icon" style="color:#67c23a">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span class="menu-label">组织信息</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
      </template>

      <!-- ===== 管理员 ===== -->
      <template v-if="role === 'admin'">
        <div class="menu-item" @click="router.push('/admin')">
          <span class="menu-icon" style="color:#409eff">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="8" y1="9" x2="16" y2="9" />
              <line x1="8" y1="13" x2="16" y2="13" />
              <line x1="8" y1="17" x2="12" y2="17" />
            </svg>
          </span>
          <span class="menu-label">活动管理</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
        <div class="menu-item" @click="router.push({ path: '/admin', query: { tab: 'users' } })">
          <span class="menu-icon" style="color:#e6a23c">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span class="menu-label">账号管理</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
        <div class="menu-item" @click="router.push({ path: '/admin', query: { tab: 'hours' } })">
          <span class="menu-icon" style="color:#67c23a">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </span>
          <span class="menu-label">时长审核</span>
          <span class="menu-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
          </span>
        </div>
      </template>
    </div>

    <!-- 管理员移动端提示 -->
    <div v-if="role === 'admin'" class="admin-mobile-hint">
      <el-alert title="请在网页端进行用户管理和时长审核操作" type="info" :closable="false" show-icon />
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <el-button class="logout-btn" @click="handleLogout" plain>退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.user?.name || userStore.user?.studentId || '用户')
const role = computed(() => userStore.user?.role || 'student')

const totalHoursText = computed(() => {
  const hours = userStore.user?.totalHours || '0'
  return `志愿时长: ${hours} 小时`
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { admin: '管理员', organizer: '组织者', student: '学生' }
  return map[userStore.user?.role || ''] || '学生'
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.my-page {
  min-height: 100%;
  background: #f5f7fa;
  padding-bottom: 20px;
}

/* ──── 用户信息卡片 ────────────────────────────── */
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 16px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
}
.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-text {
  flex: 1;
}
.user-name {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
}
.user-role {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}
.user-hours {
  font-size: 13px;
  font-weight: 500;
  opacity: 0.9;
  flex-shrink: 0;
}

/* ──── 功能菜单 ───────────────────────────────── */
.menu-section {
  margin: 12px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-item:active {
  background: #f5f7fa;
}

.menu-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  flex-shrink: 0;
}
.menu-label {
  flex: 1;
  font-size: 15px;
  color: #303133;
}
.menu-arrow {
  color: #c0c4cc;
  display: flex;
  align-items: center;
}

/* ──── 退出登录 ───────────────────────────────── */
.logout-section {
  margin: 20px 12px 0;
}
.logout-btn {
  width: 100%;
  color: #909399;
}

/* ──── 管理员移动端提示 ───────────────────────── */
.admin-mobile-hint {
  margin: 8px 12px 0;
}
</style>
