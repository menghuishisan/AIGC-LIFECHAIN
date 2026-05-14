<template>
  <!-- 创作者工作台首页 -->
  <div class="creator-dashboard">
    <!-- 身份状态横条 -->
    <div class="status-bar">
      <div class="status-bar__item" v-for="item in statusItems" :key="item.label">
        <span class="status-bar__label">{{ item.label }}：</span>
        <StatusTag :status="item.value" :type="item.type as any" />
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
      <el-col :span="16">
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
                <el-button link type="primary" @click="router.push(`/creator/works/${row.workNo}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="lc-card">
          <div class="lc-card__title">快捷入口</div>
          <div class="quick-actions">
            <el-button type="primary" @click="router.push('/creator/works/create')">上传作品</el-button>
            <el-button @click="router.push('/creator/claims')">提交确权</el-button>
            <el-button @click="router.push('/creator/listings')">申请上架</el-button>
            <el-button @click="router.push('/creator/income')">查看收益</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 通知 + 收益趋势 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <!-- 通知卡 -->
        <div class="lc-card">
          <div class="lc-card__title">最近通知</div>
          <div v-if="notices.length" class="notice-list">
            <div class="notice-item" v-for="n in notices" :key="n.noticeNo" @click="router.push(`/notices/${n.noticeNo}`)">
              <div class="notice-item__title">{{ n.title }}</div>
              <div class="notice-item__time">{{ formatTime(n.createdAt) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无通知" :image-size="60" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="lc-card">
          <div class="lc-card__title">收益趋势（近30天）</div>
          <div ref="incomeTrendRef" style="height: 260px;" />
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
import { useUserStore } from '@/app/store/user'
import { workApi, tradeApi, appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatDate, formatTime, formatAmount } from '@/shared/utils'
import type { WorkListVO, MessageNoticeVO, IncomeSummaryVO } from '@/shared/types'

const router = useRouter()
const userStore = useUserStore()

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
  { label: '账户状态', value: userStore.accountStatus, type: 'account' },
  { label: '实名状态', value: userStore.authStatus || 'REGISTERED', type: 'account' },
  { label: 'DID', value: userStore.didStatus, type: 'did' }
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

onMounted(async () => {
  /* 并行加载工作台数据 */
  const [worksRes, noticesRes] = await Promise.all([
    workApi.getMyWorks({ pageNo: 1, pageSize: 5 }),
    appApi.getNotices({ pageNo: 1, pageSize: 5 })
  ])

  recentWorks.value = worksRes.data?.records || []
  totalWorks.value = worksRes.data?.total || 0
  notices.value = noticesRes.data?.records || []

  /* 统计待确权数量 */
  try {
    const readyRes = await workApi.getMyWorks({ pageNo: 1, pageSize: 1, status: 'READY_FOR_CLAIM' })
    pendingClaimWorks.value = readyRes.data?.total || 0
  } catch { /* 忽略 */ }

  /* 统计已上架数量 */
  try {
    const listingRes = await tradeApi.getMyListings({ pageNo: 1, pageSize: 1, status: 'LISTED' })
    listedWorks.value = listingRes.data?.total || 0
  } catch { /* 忽略 */ }

  /* 统计待处理订单数量 */
  try {
    const orderRes = await tradeApi.getMyOrders({ pageNo: 1, pageSize: 1, role: 'CREATOR', status: 'ORDER_CREATED' })
    pendingOrders.value = orderRes.data?.total || 0
  } catch { /* 忽略 */ }

  /* 加载收益汇总 */
  try {
    const incomeRes = await appApi.getIncomeSummary()
    incomeSummary.value = incomeRes.data
  } catch { /* 忽略 */ }

  /* 加载未读通知数 */
  try {
    const unreadRes = await appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false })
    unreadNotices.value = unreadRes.data?.total || 0
  } catch { /* 忽略 */ }

  /* 加载收益趋势 */
  try {
    const detailRes = await appApi.getIncomeDetails({ pageNo: 1, pageSize: 100 })
    const records = detailRes.data?.records || []
    // 按日期聚合
    const dateMap: Record<string, number> = {}
    for (const r of records) {
      const day = r.createdAt?.substring(0, 10)
      if (day) dateMap[day] = (dateMap[day] || 0) + r.amount
    }
    const sortedDates = Object.keys(dateMap).sort()
    await nextTick()
    if (incomeTrendRef.value && sortedDates.length) {
      incomeChart = echarts.init(incomeTrendRef.value)
      incomeChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: sortedDates },
        yAxis: { type: 'value' },
        series: [{ name: '收益', type: 'line', data: sortedDates.map(d => dateMap[d]), smooth: true, areaStyle: {} }],
        grid: { top: 20, right: 20, bottom: 30, left: 60 }
      })
    }
  } catch { /* 忽略 */ }
})

function handleResize() {
  incomeChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
})
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.creator-dashboard {
  .status-bar {
    display: flex;
    gap: 16px;
    padding: 16px 20px;
    background: $content-white;
    border-radius: $border-radius-md;
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
    padding: 10px 0;
    border-bottom: 1px solid #F0F3F5;
    cursor: pointer;

    &:hover {
      background: #FAFBFC;
    }

    &__title {
      font-size: 13px;
      color: #132126;
      margin-bottom: 2px;
    }

    &__time {
      font-size: 11px;
      color: #8A9AA1;
    }
  }
}

.cover-thumb-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #F0F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #8A9AA1;
}
</style>
