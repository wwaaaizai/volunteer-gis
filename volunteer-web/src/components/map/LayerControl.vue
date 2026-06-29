<template>
  <div class="layer-control" :class="{ collapsed: !expanded }">
    <!-- 标题栏 -->
    <div class="layer-control-header" @click="expanded = !expanded">
      <el-icon><Layers /></el-icon>
      <span class="header-text">图层控制</span>
      <el-icon class="arrow" :class="{ rotated: expanded }"><ArrowRight /></el-icon>
    </div>

    <!-- 图层列表 -->
    <div class="layer-list" v-show="expanded">
      <div
        v-for="item in layers"
        :key="item.def.id"
        class="layer-item"
      >
        <div class="layer-item-top">
          <el-switch
            :model-value="item.visible"
            size="small"
            @change="(val: boolean) => toggle(item.def.id, val)"
          />
          <span class="layer-name">{{ item.def.name }}</span>
        </div>
        <div class="layer-item-bottom" v-show="item.visible">
          <span class="opacity-label">透明度</span>
          <el-slider
            :model-value="item.opacity"
            :min="0"
            :max="1"
            :step="0.05"
            size="small"
            @input="(val: number) => setOpacity(item.def.id, val)"
          />
        </div>
      </div>
      <el-empty
        v-if="layers.length === 0"
        description="暂无可用图层"
        :image-size="40"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { Layers, ArrowRight } from '@element-plus/icons-vue'
import { CAMPUS_LAYERS, type CampusLayerDef } from '@/config/map'

export interface LayerState {
  def: CampusLayerDef
  visible: boolean
  opacity: number
}

const props = defineProps<{
  /** 后端返回的可用图层 ID 列表（与 CAMPUS_LAYERS 交集确定实际可用图层） */
  availableLayerIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'layers-change', layers: LayerState[]): void
}>()

const expanded = ref(true)

// 构建图层状态：取后端返回的可用图层 ∩ 前端定义的校园图层
const layers = reactive<LayerState[]>([])

function rebuildLayers(availableIds: string[] | undefined) {
  layers.length = 0
  const filtered = availableIds
    ? CAMPUS_LAYERS.filter(l => availableIds.includes(l.id))
    : CAMPUS_LAYERS

  for (const def of filtered) {
    layers.push({
      def,
      visible: def.defaultVisible,
      opacity: def.defaultOpacity,
    })
  }
  emitChange()
}

// 初始化
rebuildLayers(props.availableLayerIds)

watch(
  () => props.availableLayerIds,
  (ids) => rebuildLayers(ids),
)

function toggle(id: string, visible: boolean) {
  const item = layers.find(l => l.def.id === id)
  if (item) {
    item.visible = visible
    emitChange()
  }
}

function setOpacity(id: string, opacity: number) {
  const item = layers.find(l => l.def.id === id)
  if (item) {
    item.opacity = opacity
    emitChange()
  }
}

function emitChange() {
  emit('layers-change', [...layers])
}
</script>

<style scoped>
.layer-control {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 220px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  overflow: hidden;
  transition: width 0.2s;
  font-size: 13px;
}
.layer-control.collapsed {
  width: auto;
}

.layer-control-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid #eee;
}
.layer-control.collapsed .layer-control-header {
  border-bottom: none;
}
.header-text {
  flex: 1;
  font-weight: 600;
  color: #303133;
}
.arrow {
  transition: transform 0.2s;
  color: #909399;
}
.arrow.rotated {
  transform: rotate(90deg);
}

.layer-list {
  padding: 8px 12px 12px;
  max-height: 320px;
  overflow-y: auto;
}

.layer-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.layer-item:last-child {
  border-bottom: none;
}

.layer-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.layer-name {
  flex: 1;
  color: #303133;
}

.layer-item-bottom {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.opacity-label {
  color: #909399;
  font-size: 11px;
  width: 36px;
  flex-shrink: 0;
}
.layer-item-bottom :deep(.el-slider) {
  flex: 1;
}
</style>
