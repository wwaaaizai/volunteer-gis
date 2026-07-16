<template>
  <div class="layer-control" :class="{ collapsed: isCollapsed }">
    <!-- 标题栏 -->
    <div class="lc-header" @click="isCollapsed = !isCollapsed">
      <span class="lc-title">图层管理</span>
      <span class="lc-toggle">{{ isCollapsed ? '▶' : '▼' }}</span>
    </div>

    <!-- 图层列表 -->
    <div v-show="!isCollapsed" class="lc-body">
      <div
        v-for="item in layers"
        :key="item.name"
        class="lc-item"
      >
        <!-- 开关 -->
        <label class="lc-label">
          <input
            type="checkbox"
            :checked="item.visible"
            @change="toggle(item.name)"
          />
          <span
            class="lc-color-swatch"
            :style="{ background: item.color }"
          ></span>
          <span class="lc-name">{{ item.title }}</span>
        </label>

        <!-- 透明度滑块 -->
        <div class="lc-opacity" v-if="item.visible">
          <span class="lc-opacity-label">透明度</span>
          <input
            type="range"
            min="0"
            max="100"
            :value="Math.round((item.opacity ?? 1) * 100)"
            @input="setOpacity(item.name, $event)"
          />
        </div>

        <!-- 状态标签 -->
        <div class="lc-status">
          <span v-if="item.loading" class="lc-loading">加载中...</span>
          <span v-else-if="item.error" class="lc-error" :title="item.error">加载失败</span>
          <span v-else-if="item.ready" class="lc-ready">已就绪</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface LayerControlItem {
  name: string
  title: string
  color: string
  visible: boolean
  opacity: number
  loading: boolean
  ready: boolean
  error: string | null
}

defineProps<{
  layers: LayerControlItem[]
}>()

const emit = defineEmits<{
  (e: 'toggle', layerName: string): void
  (e: 'opacity', layerName: string, opacity: number): void
}>()

const isCollapsed = ref(false)

function toggle(name: string) {
  emit('toggle', name)
}

function setOpacity(name: string, event: Event) {
  const value = Number((event.target as HTMLInputElement).value) / 100
  emit('opacity', name, value)
}
</script>

<style scoped>
.layer-control {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  min-width: 200px;
  font-size: 13px;
  user-select: none;
}

.layer-control.collapsed {
  min-width: auto;
}

.lc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  color: #303133;
}

.lc-toggle {
  font-size: 10px;
  color: #909399;
}

.lc-body {
  padding: 0 12px 10px;
}

.lc-item {
  padding: 6px 0;
  border-top: 1px solid #f0f0f0;
}

.lc-item:first-child {
  border-top: none;
}

.lc-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.lc-label input {
  margin: 0;
  cursor: pointer;
}

.lc-color-swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.lc-name {
  color: #303133;
}

.lc-opacity {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding-left: 22px;
}

.lc-opacity-label {
  font-size: 11px;
  color: #909399;
  flex-shrink: 0;
}

.lc-opacity input[type='range'] {
  flex: 1;
  height: 4px;
  cursor: pointer;
}

.lc-status {
  padding-left: 22px;
  font-size: 11px;
  margin-top: 2px;
}

.lc-loading {
  color: #e6a23c;
}

.lc-error {
  color: #f56c6c;
  cursor: help;
}

.lc-ready {
  color: #67c23a;
}
</style>
