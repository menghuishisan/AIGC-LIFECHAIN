<template>
  <!-- 时间线组件：统一展示生命周期轨迹 -->
  <div class="lc-timeline">
    <div class="lc-timeline__title">{{ title }}</div>
    <el-timeline>
      <el-timeline-item
        v-for="(event, index) in events"
        :key="index"
        :timestamp="formatTime(event.eventTime)"
        placement="top"
        :type="getTimelineType(event.eventType)"
      >
        <div class="lc-timeline__event">
          <div class="lc-timeline__event-desc">{{ event.description }}</div>
          <div v-if="event.operator" class="lc-timeline__event-operator">
            操作人：<span class="font-mono">{{ event.operator }}</span>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!events.length" description="暂无轨迹记录" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import type { TraceEventVO } from '@/shared/types'
import { formatTime } from '@/shared/utils'

defineProps<{
  /** 时间线标题 */
  title?: string
  /** 轨迹事件列表 */
  events: TraceEventVO[]
}>()

/** 根据事件类型返回时间线节点样式 */
function getTimelineType(eventType: string): '' | 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (eventType.includes('SUCCESS') || eventType.includes('CONFIRMED') || eventType.includes('ACTIVE')) return 'success'
  if (eventType.includes('FAILED') || eventType.includes('REJECTED') || eventType.includes('FROZEN')) return 'danger'
  if (eventType.includes('PENDING') || eventType.includes('REVIEWING')) return 'warning'
  return 'primary'
}
</script>

<style lang="scss" scoped>
.lc-timeline {
  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #132126;
    margin-bottom: 16px;
  }

  &__event {
    &-desc {
      font-size: 14px;
      color: #132126;
      margin-bottom: 4px;
    }
    &-operator,
    &-tx {
      font-size: 12px;
      color: #5E727A;
      margin-top: 2px;
    }
  }
}
</style>
