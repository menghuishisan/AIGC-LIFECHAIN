<template>
  <!-- 退款管理列表页（管理员） -->
  <div class="refund-list-page">
    <div class="page-header"><h2>退款管理</h2></div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="refundNo" label="退款编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/refunds/${row.refundNo}`)">{{ row.refundNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="关联订单" min-width="160" />
      <el-table-column prop="refundAmount" label="退款金额" width="120">
        <template #default="{ row }">{{ formatCurrency(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column prop="refundStatus" label="状态" width="120">
        <template #default="{ row }"><el-tag size="small">{{ row.refundStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" width="180">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
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
import { tradeApi } from '@/shared/api'
import { formatCurrency, formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getRefundList(query)
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
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
