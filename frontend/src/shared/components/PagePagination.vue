<template>
  <div class="page-pagination">
    <label v-if="layout.includes('sizes')" class="page-pagination__size">
      每页条数
      <select :value="pageSize" @change="changeSize">
        <option v-for="size in pageSizes" :key="size" :value="size">{{ size }} 条</option>
      </select>
    </label>
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :layout="
        layout
          .split(',')
          .map((item) => item.trim())
          .filter((item) => item !== 'sizes')
          .join(', ')
      "
      :pager-count="5"
      @update:current-page="changePage"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick } from 'vue'
const props = withDefaults(
  defineProps<{
    currentPage?: number
    pageSize?: number
    total: number
    layout?: string
    pageSizes?: number[]
  }>(),
  {
    currentPage: 1,
    pageSize: 20,
    layout: 'total, prev, pager, next',
    pageSizes: () => [10, 20, 50]
  }
)
const emit = defineEmits<{
  'update:currentPage': [page: number]
  'update:pageSize': [size: number]
  change: [page: number, size: number]
}>()

async function changePage(page: number) {
  emit('update:currentPage', page)
  await nextTick()
  emit('change', page, props.pageSize)
}

async function changeSize(event: Event) {
  const size = Number((event.target as HTMLSelectElement).value)
  // 修改每页条数后回到第一页，只发出一次读取事件，避免并发请求覆盖结果。
  emit('update:pageSize', size)
  emit('update:currentPage', 1)
  await nextTick()
  emit('change', 1, size)
}
</script>

<style scoped>
.page-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.page-pagination__size {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--lc-muted);
  font-size: 12px;
}
select {
  min-height: 32px;
  padding: 4px 8px;
  border: 1px solid var(--lc-line);
  border-radius: 4px;
  background: var(--lc-surface);
  color: var(--lc-ink);
}
@media (max-width: 760px) {
  .page-pagination {
    justify-content: center;
  }
}
</style>
