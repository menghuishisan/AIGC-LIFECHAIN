<template>
  <!-- 监管仪表盘 -->
  <div class="regulator-dashboard-page">
    <div class="page-header"><h2>监管概览</h2></div>

    <!-- 统计指标 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6">
        <div class="metric-card">
          <div class="metric-card__label">风险事件</div>
          <div class="metric-card__value">{{ riskCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card">
          <div class="metric-card__label">冻结记录</div>
          <div class="metric-card__value">{{ freezeCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card">
          <div class="metric-card__label">争议案件</div>
          <div class="metric-card__value">{{ disputeCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="metric-card">
          <div class="metric-card__label">监管报告</div>
          <div class="metric-card__value">{{ reportCount }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <div class="lc-card" style="margin-top: 20px;">
      <div class="lc-card__title">快捷操作</div>
      <div class="quick-actions">
        <el-button type="primary" @click="router.push('/regulator/search')">综合搜索</el-button>
        <el-button @click="router.push('/regulator/verify')">验真查询</el-button>
        <el-button @click="router.push('/regulator/risks')">风险事件</el-button>
        <el-button @click="router.push('/regulator/freezes')">冻结管理</el-button>
        <el-button @click="router.push('/regulator/disputes')">争议查看</el-button>
        <el-button @click="router.push('/regulator/reports/create')">生成报告</el-button>
      </div>
    </div>

    <!-- 最近事件列表 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <div class="lc-card">
          <div class="lc-card__title">最近风险事件</div>
          <el-table :data="recentRisks" stripe size="small" v-loading="listLoading">
            <el-table-column prop="riskNo" label="编号" width="170">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/regulator/risks/${row.riskNo}`)">{{ row.riskNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="riskType" label="类型" width="100" />
            <el-table-column prop="riskStatus" label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.riskStatus" type="risk" /></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!listLoading && !recentRisks.length" description="暂无风险事件" :image-size="40" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="lc-card">
          <div class="lc-card__title">最近冻结记录</div>
          <el-table :data="recentFreezes" stripe size="small" v-loading="listLoading">
            <el-table-column prop="freezeNo" label="编号" width="170">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/regulator/freezes/${row.freezeNo}`)">{{ row.freezeNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="targetType" label="目标" width="80" />
            <el-table-column prop="freezeStatus" label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.freezeStatus" type="freeze" /></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!listLoading && !recentFreezes.length" description="暂无冻结记录" :image-size="40" />
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <div class="lc-card">
          <div class="lc-card__title">最近争议案件</div>
          <el-table :data="recentDisputes" stripe size="small" v-loading="listLoading">
            <el-table-column prop="caseNo" label="案件编号" width="170">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/regulator/disputes/${row.caseNo}`)">{{ row.caseNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="disputeType" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" type="dispute" /></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!listLoading && !recentDisputes.length" description="暂无争议案件" :image-size="40" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="lc-card">
          <div class="lc-card__title">最近监管报告</div>
          <el-table :data="recentReports" stripe size="small" v-loading="listLoading">
            <el-table-column prop="reportNo" label="编号" width="170">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/regulator/reports/${row.reportNo}`)">{{ row.reportNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="reportTitle" label="标题" min-width="120" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!listLoading && !recentReports.length" description="暂无监管报告" :image-size="40" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const listLoading = ref(false)
const riskCount = ref(0)
const freezeCount = ref(0)
const disputeCount = ref(0)
const reportCount = ref(0)

const recentRisks = ref<any[]>([])
const recentFreezes = ref<any[]>([])
const recentDisputes = ref<any[]>([])
const recentReports = ref<any[]>([])

function reportStatusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', GENERATING: '生成中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

function reportStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    DRAFT: 'info', GENERATING: 'warning', COMPLETED: 'success', FAILED: 'danger'
  }
  return map[status]
}

async function loadData() {
  loading.value = true
  listLoading.value = true
  try {
    const [riskRes, freezeRes, disputeRes, reportRes] = await Promise.allSettled([
      regulatorApi.getRiskList({ pageNo: 1, pageSize: 5 }),
      regulatorApi.getFreezeList({ pageNo: 1, pageSize: 5 }),
      regulatorApi.getRegulatorDisputes({ pageNo: 1, pageSize: 5 }),
      regulatorApi.getReportList({ pageNo: 1, pageSize: 5 })
    ])
    if (riskRes.status === 'fulfilled') {
      riskCount.value = riskRes.value.data?.total || 0
      recentRisks.value = riskRes.value.data?.records || []
    }
    if (freezeRes.status === 'fulfilled') {
      freezeCount.value = freezeRes.value.data?.total || 0
      recentFreezes.value = freezeRes.value.data?.records || []
    }
    if (disputeRes.status === 'fulfilled') {
      disputeCount.value = disputeRes.value.data?.total || 0
      recentDisputes.value = disputeRes.value.data?.records || []
    }
    if (reportRes.status === 'fulfilled') {
      reportCount.value = reportRes.value.data?.total || 0
      recentReports.value = reportRes.value.data?.records || []
    }
  } catch { /* 降级展示 */ }
  loading.value = false
  listLoading.value = false
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.quick-actions { display: flex; gap: 12px; flex-wrap: wrap; }
</style>
