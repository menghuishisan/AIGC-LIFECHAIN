<template>
  <!-- 监管报告列表页 -->
  <div class="report-list-page" v-loading="loading">
    <div class="page-header">
      <h2>监管报告</h2>
      <router-link to="/regulator/reports/create">
        <el-button type="primary">创建报告</el-button>
      </router-link>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="query.reportType" placeholder="报告类型" clearable @change="loadList" style="width: 160px" />
      <el-select v-model="query.status" placeholder="状态" clearable @change="loadList" style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="生成中" value="GENERATING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="失败" value="FAILED" />
      </el-select>
    </div>

    <!-- 列表 -->
    <el-table :data="list" stripe>
      <el-table-column prop="reportNo" label="报告编号" width="200">
        <template #default="{ row }">
          <router-link :to="`/regulator/reports/${row.reportNo}`" class="link">{{ row.reportNo }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="reportType" label="报告类型" width="140" />
      <el-table-column prop="reportTitle" label="报告标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="链上状态" width="100">
        <template #default="{ row }">
          <span v-if="row.chainStatus">{{ ChainStatusMap[row.chainStatus] || row.chainStatus }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @current-change="p => { query.pageNo = p; loadList() }"
      style="margin-top: 16px; justify-content: flex-end;"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { regulatorApi } from '@/shared/api'
import { ChainStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import type { ReportVO } from '@/shared/types'

const loading = ref(false)
const list = ref<ReportVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20, reportType: '', status: '' })

/** 报告状态标签 */
function reportStatusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', GENERATING: '生成中', COMPLETED: '已完成', FAILED: '失败'
  }
  return map[status] || status
}

function reportStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    DRAFT: 'info', GENERATING: 'warning', COMPLETED: 'success', FAILED: 'danger'
  }
  return map[status]
}

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: query.pageNo, pageSize: query.pageSize }
    if (query.reportType) params.reportType = query.reportType
    if (query.status) params.status = query.status
    const res = await regulatorApi.getReportList(params as any)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>
