<template>
  <!-- 审计日志页（管理员） -->
  <div class="audit-logs-page">
    <div class="page-header"><h2>审计日志</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.targetType" placeholder="目标类型" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in TargetTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-input v-model="query.action" placeholder="操作类型" clearable style="width: 150px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="targetType" label="目标类型" width="100">
        <template #default="{ row }">{{ TargetTypeMap[row.targetType] || row.targetType }}</template>
      </el-table-column>
      <el-table-column prop="targetNo" label="目标编号" min-width="180">
        <template #default="{ row }"><span class="font-mono">{{ row.targetNo }}</span></template>
      </el-table-column>
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column prop="operatorRole" label="操作角色" width="120" />
      <el-table-column prop="actionDetail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column prop="result" label="结果" width="80" />
      <el-table-column prop="logTime" label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.logTime) }}</template>
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
import { appApi } from '@/shared/api'
import { TargetTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import type { AuditLogVO } from '@/shared/types'

const loading = ref(false)
const list = ref<AuditLogVO[]>([])
const total = ref(0)
const query = reactive({ targetType: '', action: '', pageNo: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = await appApi.getAuditLogs({
      ...query,
      targetType: query.targetType || undefined,
      action: query.action || undefined
    })
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
