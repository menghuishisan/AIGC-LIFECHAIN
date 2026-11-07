<template>
  <!-- 详情页头部组件：统一展示标题、编号、状态、操作按钮 -->
  <header class="detail-header">
    <div class="detail-header__left">
      <el-button
        v-if="allowBack"
        class="detail-header__back"
        text
        :icon="ArrowLeft"
        aria-label="返回列表"
        @click="goBack"
        >返回</el-button
      >
      <h1 class="detail-header__title">{{ title }}</h1>
      <span v-if="bizNo" class="detail-header__no font-mono">{{ bizNo }}</span>
      <StatusTag v-if="status" :status="status" :type="statusType" />
    </div>
    <div class="detail-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import StatusTag from './StatusTag.vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { StatusType } from '@/shared/types/ui'
defineProps<{
  /** 页面标题 */
  title: string
  /** 业务编号 */
  bizNo?: string
  /** 当前状态值 */
  status?: string
  /** 状态类型 */
  statusType?: StatusType
  /** 是否显示返回按钮 */
  allowBack?: boolean
}>()

const router = useRouter()
const route = useRoute()

/** 详情可从外部链接直接打开；返回操作始终进入站内所属列表。 */
function goBack() {
  const parent = route.path.slice(0, route.path.lastIndexOf('/'))
  void router.push(parent || '/')
}
</script>

<style lang="scss" scoped>
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  padding: 0 0 18px;
  border-bottom: 1px solid var(--lc-line);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
    flex-wrap: wrap;
  }

  &__title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: var(--lc-ink);
    overflow-wrap: anywhere;
  }

  &__no {
    font-size: 13px;
    color: var(--lc-muted);
    padding: 2px 8px;
    background: var(--lc-surface);
    border-radius: 4px;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

@media (max-width: 760px) {
  .detail-header {
    flex-direction: column;
  }
  .detail-header__actions {
    width: 100%;
  }
}
</style>
