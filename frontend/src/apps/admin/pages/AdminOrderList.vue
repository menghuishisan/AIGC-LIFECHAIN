<template>
  <!-- 订单管理列表页（管理员） -->
  <div class="admin-order-list-page">
    <div class="page-header"><h2>订单管理</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in OrderStatusMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="query.payChannel" placeholder="支付渠道" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in PayChannelMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-input v-model="query.buyerAccountNo" placeholder="买家账户" clearable style="width: 180px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/orders/${row.orderNo}`)">{{ row.orderNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="workTitle" label="作品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="orderStatus" label="状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.orderStatus" type="order" /></template>
      </el-table-column>
      <el-table-column prop="payAmount" label="金额" width="110">
        <template #default="{ row }">{{ formatCurrency(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column prop="payChannel" label="渠道" width="100">
        <template #default="{ row }">{{ PayChannelMap[row.payChannel] || row.payChannel }}</template>
      </el-table-column>
      <el-table-column prop="buyerAccountNo" label="买家" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="下单时间" width="170">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { OrderStatusMap, PayChannelMap } from '@/shared/constants'
import { formatCurrency, formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ orderStatus: '', payChannel: '', buyerAccountNo: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getAdminOrders({
      ...query,
      orderStatus: query.orderStatus || undefined,
      payChannel: query.payChannel || undefined,
      buyerAccountNo: query.buyerAccountNo || undefined
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
