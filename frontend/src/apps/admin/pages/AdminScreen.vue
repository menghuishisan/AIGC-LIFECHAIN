<template>
  <!-- 平台实时数据大屏 -->
  <div data-page="AdminScreen" class="lc-page admin-screen-page" v-loading="loading">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>实时数据</h1>
        <p class="page-header__desc">查看平台业务规模与最近变化。</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="4" v-for="item in metrics" :key="item.label">
        <div class="metric-card">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :xs="24" :md="8" v-for="item in todayMetrics" :key="item.label">
        <div class="metric-card today">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value highlight">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="screen-refresh" style="margin-top: 20px; text-align: center">
      <el-button @click="loadData">刷新数据</el-button>
      <span style="margin-left: 12px; font-size: 12px; color: #55645c"
        >最后更新：{{ lastUpdate }}</span
      >
    </div>
  </div>
</template>

<script setup lang="ts">
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, computed, onMounted } from 'vue'
import { appApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import type { ScreenRealtimeVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const loading = ref(false)
const data = ref<ScreenRealtimeVO | null>(null)
const lastUpdate = ref('')

const metrics = computed(() => {
  if (!data.value) return []
  return [
    { label: '账户总数', value: data.value.totalAccounts },
    { label: '作品总数', value: data.value.totalWorks },
    { label: '订单总数', value: data.value.totalOrders },
    { label: '结算总数', value: data.value.totalSettlements },
    { label: '确权总数', value: data.value.totalClaims },
    { label: '风险事件', value: data.value.totalRiskEvents }
  ]
})

const todayMetrics = computed(() => {
  if (!data.value) return []
  return [
    { label: '今日新作品', value: data.value.todayNewWorks },
    { label: '今日新订单', value: data.value.todayNewOrders },
    { label: '今日新确权', value: data.value.todayNewClaims }
  ]
})

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await appApi.getScreenRealtime()
      if (!isCurrent()) return
      data.value = res.data
      lastUpdate.value = formatTime(new Date().toISOString())
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
.metric-card.today {
  border-left: 3px solid var(--lc-accent);
}
.metric-card__value.highlight {
  color: var(--lc-accent);
}
</style>
