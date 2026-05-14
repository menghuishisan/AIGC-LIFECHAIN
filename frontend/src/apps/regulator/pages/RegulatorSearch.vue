<template>
  <!-- 监管综合搜索页 -->
  <div class="regulator-search-page">
    <div class="page-header"><h2>综合搜索</h2></div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="输入编号、名称等关键词" clearable style="width: 320px" @keyup.enter="loadData" />
      <el-select v-model="query.targetType" placeholder="目标类型" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in TargetTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="targetType" label="类型" width="100">
        <template #default="{ row }">{{ TargetTypeMap[row.targetType] || row.targetType }}</template>
      </el-table-column>
      <el-table-column prop="targetNo" label="编号" min-width="200">
        <template #default="{ row }"><span class="font-mono">{{ row.targetNo }}</span></template>
      </el-table-column>
      <el-table-column prop="title" label="名称/摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total"
        layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" @size-change="loadData" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { appApi } from '@/shared/api'
import { TargetTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ keyword: '', targetType: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await appApi.regulatorSearch({
      ...query,
      keyword: query.keyword || undefined,
      targetType: query.targetType || undefined
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
