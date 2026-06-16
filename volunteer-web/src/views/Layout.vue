<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <el-button type="text" @click="collapse = !collapse" v-if="false">
          <span v-if="collapse">展开</span>
          <span v-else>收起</span>
        </el-button>
        <span class="title">志愿活动系统</span>
      </div>
      <div class="header-right">
        <el-menu mode="horizontal" :default-active="currentPath" router :ellipsis="false" class="header-menu">
          <el-menu-item index="/">活动地图</el-menu-item>
          <el-menu-item index="/my-signups">我的报名</el-menu-item>
          <el-menu-item index="/checkin">签到</el-menu-item>
          <el-menu-item index="/admin" v-if="isAdmin">管理后台</el-menu-item>
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
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapse = ref(false)

const currentPath = computed(() => route.path)
const isAdmin = computed(() => userStore.user?.role === 'admin')
const userName = computed(() => userStore.user?.studentId || '用户')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
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
</style>
