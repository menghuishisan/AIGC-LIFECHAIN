<template>
  <div v-if="message" class="page-load-feedback" role="alert">
    <div>
      <strong>暂时无法加载页面数据</strong>
      <p>{{ message }}</p>
    </div>
    <el-button @click="$emit('retry')">重新加载</el-button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ message: string }>()
defineEmits<{ retry: [] }>()
</script>

<style scoped>
.page-load-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e8cac4;
  background: #fff7f5;
  color: #853d3b;
}
.page-load-feedback strong {
  font-size: 14px;
}
.page-load-feedback p {
  margin: 4px 0 0;
  font-size: 12px;
  overflow-wrap: anywhere;
}
@media (max-width: 480px) {
  .page-load-feedback {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
