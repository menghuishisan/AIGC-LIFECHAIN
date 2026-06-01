<template>
  <!-- 创作者订单列表页 -->
  <div class="creator-orders-page">
    <div class="page-header">
      <h2>我的卖出订单</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 180px" @change="loadData">
        <el-option v-for="(label, value) in OrderStatusMap" :key="value" :label="label" :value="value" />
      </el-select>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/orders/${row.orderNo}`)">{{ row.orderNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="workTitle" label="作品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orderStatus" label="订单状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.orderStatus" type="order" />
        </template>
      </el-table-column>
      <el-table-column prop="payAmount" label="金额" width="120">
        <template #default="{ row }">{{ formatCurrency(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column prop="payChannel" label="支付渠道" width="120">
        <template #default="{ row }">{{ PayChannelMap[row.payChannel] || row.payChannel }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="下单时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { OrderStatusMap, PayChannelMap } from '@/shared/constants'
import { formatTime, formatCurrency } from '@/shared/utils'
import type { OrderListVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const list = ref<OrderListVO[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    /* 创作者的卖出订单 */
    const res = await tradeApi.getMyOrders({ ...query, role: 'CREATOR', status: query.status || undefined })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
