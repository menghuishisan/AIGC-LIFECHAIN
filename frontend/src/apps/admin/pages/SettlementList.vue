<template>
  <!-- 结算管理列表页（管理员） -->
  <div class="settlement-list-page">
    <div class="page-header"><h2>结算管理</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="结算状态" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in SettlementStatusMap" :key="value" :label="label" :value="value" />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="settleNo" label="结算编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/settlements/${row.settleNo}`)">{{ row.settleNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="关联订单" min-width="160" />
      <el-table-column prop="totalAmount" label="总额" width="110">
        <template #default="{ row }">{{ formatCurrency(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="settleStatus" label="结算状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.settleStatus" type="settlement" /></template>
      </el-table-column>
      <el-table-column prop="chainStatus" label="链上状态" width="120">
        <template #default="{ row }">{{ row.chainStatus || '-' }}</template>
      </el-table-column>
      <el-table-column prop="settleTime" label="结算时间" width="170">
        <template #default="{ row }">{{ formatTime(row.settleTime) }}</template>
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
import { useRouter } from 'vue-router'
import { appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { SettlementStatusMap } from '@/shared/constants'
import { formatCurrency, formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await appApi.getAdminSettlements({ ...query, status: query.status || undefined })
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
