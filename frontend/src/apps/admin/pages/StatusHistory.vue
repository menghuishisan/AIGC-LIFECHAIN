<template>
  <!-- 状态变更历史页（管理员） -->
  <div class="status-history-page">
    <div class="page-header"><h2>状态变更历史</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.bizType" placeholder="业务类型" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in BizTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-input v-model="query.bizNo" placeholder="业务编号" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="bizType" label="业务类型" width="100">
        <template #default="{ row }">{{ BizTypeMap[row.bizType] || row.bizType }}</template>
      </el-table-column>
      <el-table-column prop="bizNo" label="业务编号" min-width="180">
        <template #default="{ row }"><span class="font-mono">{{ row.bizNo }}</span></template>
      </el-table-column>
      <el-table-column prop="fromStatus" label="原状态" width="130" />
      <el-table-column prop="toStatus" label="新状态" width="130" />
      <el-table-column prop="changeReason" label="原因" min-width="160" show-overflow-tooltip />
      <el-table-column prop="reasonCode" label="原因码" width="120" />
      <el-table-column prop="changeTime" label="变更时间" width="170">
        <template #default="{ row }">{{ formatTime(row.changeTime) }}</template>
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
import { BizTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import type { StatusHistoryVO } from '@/shared/types'

const loading = ref(false)
const list = ref<StatusHistoryVO[]>([])
const total = ref(0)
const query = reactive({ bizType: '', bizNo: '', pageNo: 1, pageSize: 20 })

async function loadData() {
  if (!query.bizType && !query.bizNo) return
  loading.value = true
  try {
    const res = await appApi.getStatusHistory({
      bizType: query.bizType,
      bizNo: query.bizNo,
      pageNo: query.pageNo,
      pageSize: query.pageSize
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => { /* 初始不加载，等用户输入条件 */ })
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
