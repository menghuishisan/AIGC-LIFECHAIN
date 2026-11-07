<template>
  <!-- 退款管理列表页（管理员） -->
  <div data-page="RefundList" class="lc-page refund-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>退款管理</h1>
        <p class="page-header__desc">按退款状态跟踪申请与渠道处理结果。</p>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="refundNo" label="退款编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/refunds/${row.refundNo}`)">{{
            row.refundNo
          }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="关联订单" min-width="160" />
      <el-table-column prop="refundAmount" label="退款金额" width="120">
        <template #default="{ row }">{{ formatCurrency(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column prop="refundStatus" label="状态" width="120">
        <template #default="{ row }"
          ><StatusTag :status="row.refundStatus" type="refund"
        /></template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" width="180">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <PagePagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import type { RefundDetailVO } from '@/shared/types'
import { formatCurrency, formatTime } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<RefundDetailVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await tradeApi.getRefundList(query)
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
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
