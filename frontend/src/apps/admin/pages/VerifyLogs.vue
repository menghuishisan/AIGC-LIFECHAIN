<template>
  <!-- 验真日志页（管理员） -->
  <div class="verify-logs-page">
    <div class="page-header"><h2>验真日志</h2></div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="queryType" label="查询类型" width="120" />
      <el-table-column prop="queryValue" label="查询值" min-width="200" show-overflow-tooltip>
        <template #default="{ row }"><span class="font-mono">{{ row.queryValue }}</span></template>
      </el-table-column>
      <el-table-column prop="querySource" label="来源" width="100" />
      <el-table-column label="验真结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.matchFound === 1 ? 'success' : 'danger'" size="small">{{ row.matchFound === 1 ? '通过' : '未通过' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resultSummary" label="结果摘要" min-width="180" show-overflow-tooltip />
      <el-table-column prop="queryTime" label="查询时间" width="180">
        <template #default="{ row }">{{ formatTime(row.queryTime) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total"
        layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" @size-change="loadData" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { workApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import type { VerifyQueryLogVO } from '@/shared/types'

const loading = ref(false)
const list = ref<VerifyQueryLogVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getVerifyLogs({ pageNo: query.pageNo, pageSize: query.pageSize })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
