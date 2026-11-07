<template>
  <!-- 收益管理页 -->
  <div data-page="IncomePage" class="lc-page income-page">
    <PageLoadFeedback :message="summaryError" @retry="loadSummary" />
    <PageLoadFeedback :message="detailError" @retry="loadDetails" />
    <div class="page-header">
      <div>
        <h1>收益管理</h1>
        <p class="page-header__desc">查看订单分配收益与结算明细，分配记录不代表支付渠道已打款。</p>
      </div>
    </div>

    <!-- 收益汇总卡片 -->
    <el-row :gutter="16" class="income-summary" v-loading="summaryLoading">
      <el-col :xs="24" :md="8">
        <div class="metric-card">
          <div class="metric-card__label">累计收益</div>
          <div class="metric-card__value">{{ formatCurrency(summary.totalIncome) }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="metric-card">
          <div class="metric-card__label">成功笔数</div>
          <div class="metric-card__value success">{{ summary.totalSuccessCount }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="metric-card">
          <div class="metric-card__label">总笔数</div>
          <div class="metric-card__value">{{ summary.totalCount }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 明细列表 -->
    <div class="lc-card" style="margin-top: 20px">
      <div class="lc-card__title">收益明细</div>
      <el-table :data="details" v-loading="detailLoading" stripe>
        <el-table-column prop="settleNo" label="结算编号" min-width="180">
          <template #default="{ row }">
            <span class="font-mono">{{ row.settleNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="roleType" label="角色类型" width="120" />
        <el-table-column prop="ratio" label="分成比例" width="100">
          <template #default="{ row }">{{ (row.ratio * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="amount" label="结算金额" width="120">
          <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="settlement" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <PagePagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="loadDetails"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatCurrency, formatTime } from '@/shared/utils'
import type { IncomeSummaryVO, IncomeDetailVO } from '@/shared/types'
const { pageError: summaryError, runPageLoad: runSummaryLoad } = usePageLoad()
const { pageError: detailError, runPageLoad: runDetailLoad } = usePageLoad()

const summaryLoading = ref(false)
const detailLoading = ref(false)

const summary = ref<IncomeSummaryVO>({
  totalIncome: 0,
  totalSuccessCount: 0,
  totalCount: 0
})

const details = ref<IncomeDetailVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

/** 加载汇总 */
async function loadSummary() {
  await runSummaryLoad(async (isCurrent) => {
    summaryLoading.value = true
    try {
      const res = await appApi.getIncomeSummary()
      if (!isCurrent()) return
      summary.value = res.data
    } finally {
      if (isCurrent()) summaryLoading.value = false
    }
  })
}

/** 加载明细 */
async function loadDetails() {
  await runDetailLoad(async (isCurrent) => {
    detailLoading.value = true
    try {
      const res = await appApi.getIncomeDetails(query)
      if (!isCurrent()) return
      details.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) detailLoading.value = false
    }
  })
}

onMounted(() => {
  loadSummary()
  loadDetails()
})
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.income-summary {
  margin-bottom: 8px;
}
.metric-card__value.pending {
  color: #e6a23c;
}
.metric-card__value.success {
  color: #67c23a;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
