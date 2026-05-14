<template>
  <!-- 管理后台仪表盘 -->
  <div class="admin-dashboard-page">
    <div class="page-header">
      <h2>平台概览</h2>
    </div>

    <!-- 概览指标 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="4" v-for="item in metricCards" :key="item.label">
        <div class="metric-card">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 待处理事项 -->
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">待处理事项</div>
          <div class="pending-list">
            <div class="pending-item" @click="router.push('/admin/claims')">
              <span>确权审核</span>
              <el-badge :value="pendingClaims" :hidden="!pendingClaims" :max="99" />
            </div>
            <div class="pending-item" @click="router.push('/admin/listings')">
              <span>上架审核</span>
              <el-badge :value="pendingListings" :hidden="!pendingListings" :max="99" />
            </div>
            <div class="pending-item" @click="router.push('/admin/dids')">
              <span>DID审核</span>
              <el-badge :value="pendingDids" :hidden="!pendingDids" :max="99" />
            </div>
            <div class="pending-item" @click="router.push('/admin/disputes')">
              <span>争议处理</span>
              <el-badge :value="pendingDisputes" :hidden="!pendingDisputes" :max="99" />
            </div>
          </div>
        </div>
      </el-col>
      <!-- 确权趋势 -->
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">确权趋势（近30天）</div>
          <div ref="claimChartRef" style="height: 300px;" />
        </div>
      </el-col>
      <!-- 订单趋势 -->
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">订单趋势（近30天）</div>
          <div ref="orderChartRef" style="height: 300px;" />
        </div>
      </el-col>
    </el-row>

    <!-- 分布统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">作品类型分布</div>
          <div ref="workTypeChartRef" style="height: 260px;" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">订单状态分布</div>
          <div ref="orderStatusChartRef" style="height: 260px;" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">风险状态分布</div>
          <div ref="riskStatusChartRef" style="height: 260px;" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import echarts from '@/shared/lib/echarts'
import type { ECharts } from '@/shared/lib/echarts'
import { appApi, workApi, tradeApi, authApi } from '@/shared/api'
import { WorkTypeMap, OrderStatusMap, RiskStatusMap } from '@/shared/constants'
import { formatCurrency } from '@/shared/utils'
import type { StatsOverviewVO, TrendPointVO, DistributionVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const overview = ref<StatsOverviewVO | null>(null)

/* 待处理事项 */
const pendingClaims = ref(0)
const pendingListings = ref(0)
const pendingDids = ref(0)
const pendingDisputes = ref(0)

/* ECharts 容器 */
const claimChartRef = ref<HTMLElement>()
const orderChartRef = ref<HTMLElement>()
const workTypeChartRef = ref<HTMLElement>()
const orderStatusChartRef = ref<HTMLElement>()
const riskStatusChartRef = ref<HTMLElement>()

let charts: ECharts[] = []

const metricCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '总账户数', value: o.totalAccounts },
    { label: '总作品数', value: o.totalWorks },
    { label: '总订单数', value: o.totalOrders },
    { label: '累计交易额', value: formatCurrency(o.totalTradeAmount) },
    { label: '今日新增账户', value: o.todayNewAccounts },
    { label: '今日新增订单', value: o.todayNewOrders }
  ]
})

/** 渲染折线图 */
function renderLineChart(el: HTMLElement, data: TrendPointVO[], title: string) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value' },
    series: [{ name: title, type: 'line', data: data.map(d => d.count), smooth: true, areaStyle: {} }],
    grid: { top: 20, right: 20, bottom: 30, left: 50 }
  })
  charts.push(chart)
}

/** 渲染饼图 */
function renderPieChart(el: HTMLElement, dist: Record<string, number>, labelMap: Record<string, string>) {
  const chart = echarts.init(el)
  const data = Object.entries(dist).map(([k, v]) => ({ name: labelMap[k] || k, value: v }))
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['40%', '65%'], data, label: { fontSize: 12 } }]
  })
  charts.push(chart)
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, claimTrendRes, orderTrendRes, distRes] = await Promise.all([
      appApi.getStatsOverview(),
      appApi.getClaimsTrend(30),
      appApi.getOrdersTrend(30),
      appApi.getDistribution()
    ])
    overview.value = overviewRes.data

    await nextTick()
    if (claimChartRef.value) renderLineChart(claimChartRef.value, claimTrendRes.data, '确权')
    if (orderChartRef.value) renderLineChart(orderChartRef.value, orderTrendRes.data, '订单')

    const dist: DistributionVO = distRes.data
    if (workTypeChartRef.value) renderPieChart(workTypeChartRef.value, dist.workTypeDistribution, WorkTypeMap)
    if (orderStatusChartRef.value) renderPieChart(orderStatusChartRef.value, dist.orderStatusDistribution, OrderStatusMap)
    if (riskStatusChartRef.value) renderPieChart(riskStatusChartRef.value, dist.riskStatusDistribution, RiskStatusMap)
  } finally {
    loading.value = false
  }

  /* 加载待处理数 */
  Promise.allSettled([
    workApi.getAdminClaims({ pageNo: 1, pageSize: 1, status: 'CLAIM_SUBMITTED' }),
    tradeApi.getAdminListings({ pageNo: 1, pageSize: 1, reviewStatus: 'PENDING_REVIEW' }),
    authApi.getDidList({ pageNo: 1, pageSize: 1, status: 'DID_PENDING' })
  ]).then(([claimsRes, listingsRes, didsRes]) => {
    if (claimsRes.status === 'fulfilled') pendingClaims.value = claimsRes.value.data?.total || 0
    if (listingsRes.status === 'fulfilled') pendingListings.value = listingsRes.value.data?.total || 0
    if (didsRes.status === 'fulfilled') pendingDids.value = didsRes.value.data?.total || 0
  })
}

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}
.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color .2s;
}
.pending-item:hover {
  background-color: var(--el-fill-color-light);
}
</style>
