<template>
  <!-- 详情页头部组件：统一展示标题、编号、状态、操作按钮 -->
  <div class="detail-header">
    <div class="detail-header__left">
      <h2 class="detail-header__title">{{ title }}</h2>
      <span class="detail-header__no font-mono">{{ bizNo }}</span>
      <StatusTag v-if="status" :status="status" :type="(statusType as any)" />
    </div>
    <div class="detail-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import StatusTag from './StatusTag.vue'

defineProps<{
  /** 页面标题 */
  title: string
  /** 业务编号 */
  bizNo?: string
  /** 当前状态值 */
  status?: string
  /** 状态类型 */
  statusType?: string
  /** 是否显示返回按钮 */
  allowBack?: boolean
}>()
</script>

<style lang="scss" scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 40, 45, 0.06);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: #132126;
  }

  &__no {
    font-size: 13px;
    color: #5E727A;
    padding: 2px 8px;
    background: #F4F7F8;
    border-radius: 4px;
  }

  &__actions {
    display: flex;
    gap: 8px;
  }
}
</style>
