<template>
  <!-- 管理后台仪表盘 -->
  <div data-page="AdminDashboard" class="lc-page admin-dashboard-page">
    <header class="page-header admin-dashboard-heading">
      <div>
        <p class="admin-dashboard-heading__eyebrow">PLATFORM OPERATIONS / LIFECHAIN</p>
        <h1>运营总览</h1>
        <p>审核队列、交易趋势与平台运行情况。</p>
      </div>
      <el-button :loading="loading" @click="loadData">刷新数据</el-button>
    </header>

    <el-alert
      v-if="loadError"
      class="admin-dashboard-error"
      type="warning"
      title="部分运营数据暂时未能加载。"
      show-icon
      :closable="false"
    />

    <!-- 概览指标 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="12" :sm="8" :lg="4" v-for="item in metricCards" :key="item.label">
        <div class="metric-card">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 待处理事项 -->
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">待处理事项</div>
          <div class="pending-list">
            <button class="pending-item" type="button" @click="router.push('/admin/claims')">
              <span>确权审核</span>
              <el-badge :value="pendingClaims" :hidden="!pendingClaims" :max="99" />
            </button>
            <button class="pending-item" type="button" @click="router.push('/admin/listings')">
              <span>上架审核</span>
              <el-badge :value="pendingListings" :hidden="!pendingListings" :max="99" />
            </button>
            <button class="pending-item" type="button" @click="router.push('/admin/dids')">
              <span>DID审核</span>
              <el-badge :value="pendingDids" :hidden="!pendingDids" :max="99" />
            </button>
            <button class="pending-item" type="button" @click="router.push('/admin/disputes')">
              <span>争议处理</span>
              <el-badge :value="pendingDisputes" :hidden="!pendingDisputes" :max="99" />
            </button>
          </div>
        </div>
      </el-col>
      <!-- 确权趋势 -->
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">确权趋势（近30天）</div>
          <div ref="claimChartRef" style="height: 240px" />
        </div>
      </el-col>
      <!-- 订单趋势 -->
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">订单趋势（近30天）</div>
          <div ref="orderChartRef" style="height: 240px" />
        </div>
      </el-col>
    </el-row>

    <!-- 分布统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">作品类型分布</div>
          <div ref="workTypeChartRef" style="height: 220px" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">订单状态分布</div>
          <div ref="orderStatusChartRef" style="height: 220px" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">风险状态分布</div>
          <div ref="riskStatusChartRef" style="height: 220px" />
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
import { appApi, workApi, tradeApi, authApi, regulatorApi } from '@/shared/api'
import { WorkTypeMap, OrderStatusMap, RiskStatusMap } from '@/shared/constants'
import { formatCurrency } from '@/shared/utils'
import type { StatsOverviewVO, TrendPointVO, DistributionVO } from '@/shared/types'
const router = useRouter()
const loading = ref(false)
let active = true
const loadError = ref(false)
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
    color: ['#925923'],
    aria: { enabled: true },
    animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.date),
      axisLabel: { color: '#55645c', hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#55645c' },
      splitLine: { lineStyle: { color: '#e4e8e3' } }
    },
    series: [
      {
        name: title,
        type: 'line',
        data: data.map((d) => d.count),
        smooth: true,
        areaStyle: { opacity: 0.08 }
      }
    ],
    grid: { top: 20, right: 20, bottom: 30, left: 50 }
  })
  charts.push(chart)
}

/** 渲染饼图 */
function renderPieChart(
  el: HTMLElement,
  dist: Record<string, number>,
  labelMap: Record<string, string>
) {
  const chart = echarts.init(el)
  const data = Object.entries(dist).map(([k, v]) => ({ name: labelMap[k] || k, value: v }))
  chart.setOption({
    color: ['#925923', '#38684c', '#4f7485', '#92534b', '#6d6578', '#7c6f36'],
    aria: { enabled: true, decal: { show: true } },
    animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    tooltip: { trigger: 'item' },
    series: [
      { type: 'pie', radius: ['38%', '60%'], data, label: { fontSize: 11, color: '#55645c' } }
    ]
  })
  charts.push(chart)
}

async function loadData() {
  if (loading.value) return
  loading.value = true
  loadError.value = false
  charts.forEach((chart) => chart.dispose())
  charts = []
  try {
    const [overviewRes, claimTrendRes, orderTrendRes, distRes] = await Promise.allSettled([
      appApi.getStatsOverview(),
      appApi.getClaimsTrend(30),
      appApi.getOrdersTrend(30),
      appApi.getDistribution()
    ])
    if (overviewRes.status === 'fulfilled') overview.value = overviewRes.value.data
    else loadError.value = true

    await nextTick()
    if (!active) return
    if (claimTrendRes.status === 'fulfilled' && claimChartRef.value) {
      renderLineChart(claimChartRef.value, claimTrendRes.value.data, '确权')
    } else if (claimTrendRes.status === 'rejected') loadError.value = true
    if (orderTrendRes.status === 'fulfilled' && orderChartRef.value) {
      renderLineChart(orderChartRef.value, orderTrendRes.value.data, '订单')
    } else if (orderTrendRes.status === 'rejected') loadError.value = true

    if (distRes.status === 'fulfilled') {
      const dist: DistributionVO = distRes.value.data
      if (workTypeChartRef.value)
        renderPieChart(workTypeChartRef.value, dist.workTypeDistribution, WorkTypeMap)
      if (orderStatusChartRef.value)
        renderPieChart(orderStatusChartRef.value, dist.orderStatusDistribution, OrderStatusMap)
      if (riskStatusChartRef.value)
        renderPieChart(riskStatusChartRef.value, dist.riskStatusDistribution, RiskStatusMap)
    } else {
      loadError.value = true
    }

    const [claimsRes, listingsRes, didsRes, disputesRes] = await Promise.allSettled([
      workApi.getAdminClaims({ pageNo: 1, pageSize: 1, status: 'CLAIM_SUBMITTED' }),
      tradeApi.getAdminListings({ pageNo: 1, pageSize: 1, reviewStatus: 'PENDING_REVIEW' }),
      authApi.getDidList({ pageNo: 1, pageSize: 1, status: 'DID_PENDING' }),
      regulatorApi.getRegulatorDisputes({ pageNo: 1, pageSize: 1, status: 'DISPUTE_SUBMITTED' })
    ])
    if (!active) return
    if (claimsRes.status === 'fulfilled') pendingClaims.value = claimsRes.value.data?.total || 0
    else loadError.value = true
    if (listingsRes.status === 'fulfilled')
      pendingListings.value = listingsRes.value.data?.total || 0
    else loadError.value = true
    if (didsRes.status === 'fulfilled') pendingDids.value = didsRes.value.data?.total || 0
    else loadError.value = true
    if (disputesRes.status === 'fulfilled')
      pendingDisputes.value = disputesRes.value.data?.total || 0
    else loadError.value = true
  } finally {
    loading.value = false
  }
}

function handleResize() {
  charts.forEach((c) => c.resize())
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  active = false
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.admin-dashboard-heading__eyebrow {
  margin-bottom: 4px;
  color: #8e592d;
  font: 600 10px/1.4 var(--lc-mono);
}

.admin-dashboard-heading h1 {
  margin: 0;
  color: var(--lc-ink);
  font-size: 24px;
  font-weight: 650;
}

.admin-dashboard-heading p:last-child {
  margin-top: 4px;
  color: var(--lc-muted);
  font-size: 12px;
}

.admin-dashboard-error {
  margin-bottom: 16px;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 46px;
  padding: 8px 4px;
  border: 0;
  border-bottom: 1px solid var(--lc-line);
  background: transparent;
  color: var(--lc-ink);
  cursor: pointer;
  text-align: left;
  transition: background-color 160ms ease;
}

.pending-item:hover {
  background-color: var(--lc-accent-soft);
}
</style>
