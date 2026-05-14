<template>
  <!-- 监管争议列表页 -->
  <div class="regulator-dispute-list-page" v-loading="loading">
    <div class="page-header"><h2>争议管理</h2></div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="争议状态" clearable @change="loadList" style="width: 140px">
        <el-option v-for="(label, key) in DisputeStatusMap" :key="key" :label="label" :value="key" />
      </el-select>
      <el-input v-model="query.disputeType" placeholder="争议类型" clearable @change="loadList" style="width: 160px" />
    </div>

    <!-- 列表 -->
    <el-table :data="list" stripe>
      <el-table-column prop="caseNo" label="案件编号" width="200">
        <template #default="{ row }">
          <router-link :to="`/regulator/disputes/${row.caseNo}`" class="link">{{ row.caseNo }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="disputeType" label="争议类型" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" type="dispute" /></template>
      </el-table-column>
      <el-table-column prop="applicantAccountNo" label="申请方" width="200" show-overflow-tooltip />
      <el-table-column prop="respondentAccountNo" label="被申请方" width="200" show-overflow-tooltip />
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
import { DisputeStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import type { RegulatorDisputeListVO } from '@/shared/types'

const loading = ref(false)
const list = ref<RegulatorDisputeListVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20, status: '', disputeType: '' })

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: query.pageNo, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.disputeType) params.disputeType = query.disputeType
    const res = await regulatorApi.getRegulatorDisputes(params as any)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>
