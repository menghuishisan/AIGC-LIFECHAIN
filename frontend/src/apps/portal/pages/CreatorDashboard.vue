<template>
  <!-- 创作者工作台首页 -->
  <div data-page="CreatorDashboard" class="lc-page creator-dashboard" v-loading="loading">
    <header class="creator-heading">
      <div>
        <p class="creator-heading__eyebrow">CREATOR STUDIO / LIFECHAIN</p>
        <h1>创作工作台</h1>
        <p>管理作品、权属证明与授权进度。</p>
      </div>
      <el-button
        type="primary"
        :icon="Upload"
        :disabled="loading"
        @click="router.push('/creator/works/create')"
        >上传新作品</el-button
      >
    </header>

    <el-alert
      v-if="loadError"
      class="creator-load-error"
      type="warning"
      title="部分工作台数据暂时未能加载。"
      show-icon
      :closable="false"
    >
      <template #default
        ><el-button text type="primary" @click="loadDashboard">重新加载</el-button></template
      >
    </el-alert>

    <!-- 身份状态横条 -->
    <div class="status-bar">
      <div class="status-bar__item" v-for="item in statusItems" :key="item.label">
        <span class="status-bar__label">{{ item.label }}：</span>
        <StatusTag v-if="item.value" :status="item.value" :type="item.type" />
        <span v-else class="status-tag status-tag--draft">未提交</span>
      </div>
    </div>

    <!-- 指标卡行 -->
    <div class="metric-row">
      <div class="metric-card" v-for="metric in metrics" :key="metric.label">
        <div class="metric-card__value">{{ metric.value }}</div>
        <div class="metric-card__label">{{ metric.label }}</div>
      </div>
    </div>

    <!-- 最近作品 + 快捷入口 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <div class="lc-card">
          <div class="lc-card__title">最近作品</div>
          <el-table :data="recentWorks" stripe style="width: 100%" size="default">
            <el-table-column label="封面" width="70">
              <template #default="{ row }">
                <img v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" alt="封面" />
                <div v-else class="cover-thumb-placeholder">无</div>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="120" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <StatusTag :status="row.status" type="work" />
              </template>
            </el-table-column>
            <el-table-column prop="workNo" label="作品编号" width="180">
              <template #default="{ row }">
                <span class="font-mono">{{ row.workNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="120">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/creator/works/${row.workNo}`)"
                  >查看</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="lc-card">
          <div class="lc-card__title">快捷入口</div>
          <div class="quick-actions">
            <el-button type="primary" @click="router.push('/creator/works/create')"
              >上传作品</el-button
            >
            <el-button @click="router.push('/creator/claims')">提交确权</el-button>
            <el-button @click="router.push('/creator/listings')">申请上架</el-button>
            <el-button @click="router.push('/creator/income')">查看收益</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 通知 + 收益趋势 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :lg="12">
        <!-- 通知卡 -->
        <div class="lc-card">
          <div class="lc-card__title">最近通知</div>
          <div v-if="notices.length" class="notice-list">
            <button
              class="notice-item"
              type="button"
              v-for="n in notices"
              :key="n.noticeNo"
              @click="router.push(`/notices/${n.noticeNo}`)"
            >
              <div class="notice-item__title">{{ n.title }}</div>
              <div class="notice-item__time">{{ formatTime(n.createdAt) }}</div>
            </button>
          </div>
          <el-empty v-else description="暂无通知" :image-size="60" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="lc-card">
          <div class="lc-card__title">净收益趋势 · 最近30日（UTC）</div>
          <div v-if="hasIncomeTrend" ref="incomeTrendRef" class="income-chart" />
          <el-empty v-else-if="!loading" description="暂无收益记录" :image-size="48" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Upload } from '@element-plus/icons-vue'
import echarts from '@/shared/lib/echarts'
import type { ECharts } from '@/shared/lib/echarts'
import { useUserStore } from '@/app/store/user'
import { workApi, tradeApi, appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatDate, formatTime, formatAmount } from '@/shared/utils'
import type { WorkListVO, MessageNoticeVO, IncomeSummaryVO } from '@/shared/types'
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
let active = true
const loadError = ref(false)
const hasIncomeTrend = ref(false)
const recentWorks = ref<WorkListVO[]>([])
const notices = ref<MessageNoticeVO[]>([])
const incomeSummary = ref<IncomeSummaryVO>({ totalIncome: 0, totalSuccessCount: 0, totalCount: 0 })
const totalWorks = ref(0)
const pendingClaimWorks = ref(0)
const listedWorks = ref(0)
const pendingOrders = ref(0)
const unreadNotices = ref(0)
const incomeTrendRef = ref<HTMLElement>()
let incomeChart: ECharts | null = null

/** 身份状态横条数据 */
const statusItems = computed(() => [
  { label: '账户状态', value: userStore.accountStatus, type: 'account' as const },
  { label: '实名状态', value: userStore.authStatus, type: 'account' as const },
  { label: 'DID', value: userStore.didStatus, type: 'did' as const }
])

/** 指标卡数据 */
const metrics = computed(() => [
  { label: '作品总数', value: totalWorks.value },
  { label: '待确权', value: pendingClaimWorks.value },
  { label: '已上架', value: listedWorks.value },
  { label: '待处理订单', value: pendingOrders.value },
  { label: '累计收益', value: formatAmount(incomeSummary.value.totalIncome) },
  { label: '未读通知', value: unreadNotices.value }
])

async function loadDashboard() {
  if (loading.value) return
  loading.value = true
  loadError.value = false
  hasIncomeTrend.value = false
  incomeChart?.dispose()
  incomeChart = null
  const results = await Promise.allSettled([
    workApi.getMyWorks({ pageNo: 1, pageSize: 5 }),
    appApi.getNotices({ pageNo: 1, pageSize: 5 }),
    workApi.getMyWorks({ pageNo: 1, pageSize: 1, status: 'READY_FOR_CLAIM' }),
    tradeApi.getMyListings({ pageNo: 1, pageSize: 1, status: 'LISTED' }),
    tradeApi.getMyOrders({ pageNo: 1, pageSize: 1, role: 'CREATOR', status: 'ORDER_CREATED' }),
    appApi.getIncomeSummary(),
    appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false }),
    appApi.getIncomeTrend()
  ])

  const [works, noticeList, readyClaims, listings, orders, income, unread, incomeTrend] = results
  if (!active) return
  loadError.value = results.some((result) => result.status === 'rejected')
  if (works.status === 'fulfilled') {
    recentWorks.value = works.value.data?.records || []
    totalWorks.value = works.value.data?.total || 0
  }
  if (noticeList.status === 'fulfilled') notices.value = noticeList.value.data?.records || []
  if (readyClaims.status === 'fulfilled')
    pendingClaimWorks.value = readyClaims.value.data?.total || 0
  if (listings.status === 'fulfilled') listedWorks.value = listings.value.data?.total || 0
  if (orders.status === 'fulfilled') pendingOrders.value = orders.value.data?.total || 0
  if (income.status === 'fulfilled') incomeSummary.value = income.value.data
  if (unread.status === 'fulfilled') unreadNotices.value = unread.value.data?.total || 0

  if (incomeTrend.status === 'fulfilled') {
    const dateMap: Record<string, number> = {}
    for (const record of incomeTrend.value.data || []) {
      const day = record.date
      if (day) dateMap[day] = (dateMap[day] || 0) + record.amount
    }
    const sortedDates = Object.keys(dateMap).sort()
    hasIncomeTrend.value = Object.values(dateMap).some((amount) => amount > 0)
    await nextTick()
    if (!active) return
    if (incomeTrendRef.value && sortedDates.length) {
      const chart = echarts.init(incomeTrendRef.value)
      chart.setOption({
        color: ['#38684c'],
        aria: { enabled: true },
        animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: sortedDates, axisLabel: { color: '#55645c' } },
        yAxis: {
          type: 'value',
          name: '元',
          axisLabel: { color: '#55645c' },
          splitLine: { lineStyle: { color: '#e4e8e3' } }
        },
        series: [
          {
            name: '收益',
            type: 'line',
            data: sortedDates.map((day) => dateMap[day] / 100),
            smooth: true,
            areaStyle: { opacity: 0.08 }
          }
        ],
        grid: { top: 20, right: 16, bottom: 30, left: 48 }
      })
      incomeChart = chart
    }
  } else {
    hasIncomeTrend.value = false
  }
  loading.value = false
}

onMounted(loadDashboard)

function handleResize() {
  incomeChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  active = false
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
})
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.creator-dashboard {
  min-width: 0;

  .creator-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;

    h1 {
      margin: 3px 0 2px;
      color: $text-primary;
      font-size: 24px;
      font-weight: 650;
    }

    p {
      color: $text-secondary;
      font-size: 12px;
    }

    &__eyebrow {
      color: $brand-dark;
      font: 600 10px/1.4 $font-family-mono;
    }
  }

  .creator-load-error {
    margin-bottom: 16px;
  }

  .status-bar {
    display: flex;
    gap: 16px;
    padding: 16px 20px;
    background: $content-white;
    border-radius: $border-radius-sm;
    margin-bottom: $card-gap;
    box-shadow: $card-shadow;

    &__item {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    &__label {
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .el-button {
    width: 100%;
    justify-content: flex-start;
  }
}

.notice-list {
  .notice-item {
    display: block;
    width: 100%;
    padding: 10px 0;
    border-bottom: 1px solid #f0f3f5;
    border-top: 0;
    border-right: 0;
    border-left: 0;
    background: transparent;
    cursor: pointer;
    text-align: left;

    &:hover {
      background: #fafbfc;
    }

    &__title {
      font-size: 13px;
      color: #132126;
      margin-bottom: 2px;
    }

    &__time {
      font-size: 11px;
      color: #55645c;
    }
  }
}

.income-chart {
  width: 100%;
  height: 260px;
}

.cover-thumb-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #f0f3f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #55645c;
}

@media (max-width: 640px) {
  .creator-dashboard .creator-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
