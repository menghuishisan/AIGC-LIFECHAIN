<template>
  <!-- 风险事件列表页 -->
  <div class="risk-list-page" v-loading="loading">
    <div class="page-header">
      <h2>风险事件管理</h2>
      <el-button type="danger" @click="showMarkDialog = true">标记风险</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="风险状态" clearable @change="loadList" style="width: 160px">
        <el-option v-for="(label, key) in RiskStatusMap" :key="key" :label="label" :value="key" />
      </el-select>
    </div>

    <!-- 列表 -->
    <el-table :data="list" stripe>
      <el-table-column prop="riskNo" label="风险编号" width="200">
        <template #default="{ row }">
          <router-link :to="`/regulator/risks/${row.riskNo}`" class="link">{{ row.riskNo }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="目标类型" width="100">
        <template #default="{ row }">{{ TargetTypeMap[row.targetType] || row.targetType }}</template>
      </el-table-column>
      <el-table-column prop="targetNo" label="目标编号" width="200" />
      <el-table-column prop="riskType" label="风险类型" width="120" />
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="riskLevelType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" type="risk" /></template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @current-change="p => { query.pageNo = p; loadList() }"
      style="margin-top: 16px; justify-content: flex-end;"
    />

    <!-- 标记风险对话框 -->
    <el-dialog v-model="showMarkDialog" title="标记风险事件" width="520px">
      <el-form :model="markForm" label-width="100px">
        <el-form-item label="目标类型" required>
          <el-select v-model="markForm.targetType" style="width: 100%">
            <el-option v-for="(label, key) in TargetTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标编号" required>
          <el-input v-model="markForm.targetNo" placeholder="如作品编号、订单编号等" />
        </el-form-item>
        <el-form-item label="风险等级" required>
          <el-select v-model="markForm.riskLevel" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="极高" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险类型" required>
          <el-input v-model="markForm.riskType" placeholder="如 PLAGIARISM、FRAUD 等" />
        </el-form-item>
        <el-form-item label="风险描述" required>
          <el-input v-model="markForm.riskDescription" type="textarea" :rows="3" placeholder="请描述风险详情" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMarkDialog = false">取消</el-button>
        <el-button type="danger" :loading="markLoading" @click="handleMark">标记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { regulatorApi } from '@/shared/api'
import { RiskStatusMap, TargetTypeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import type { RiskEventVO } from '@/shared/types'

const loading = ref(false)
const list = ref<RiskEventVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20, status: '' })

/** 风险等级标签颜色 */
function riskLevelType(level: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[level]
}

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: query.pageNo, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    const res = await regulatorApi.getRiskList(params as any)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/* ========== 标记风险表单 ========== */
const showMarkDialog = ref(false)
const markLoading = ref(false)
const markForm = reactive({
  targetType: 'WORK',
  targetNo: '',
  riskLevel: 'MEDIUM',
  riskType: '',
  riskDescription: ''
})

async function handleMark() {
  if (!markForm.targetNo || !markForm.riskType || !markForm.riskDescription) return
  markLoading.value = true
  try {
    await regulatorApi.markRisk({ ...markForm, requestId: generateRequestId() })
    ElMessage.success('风险标记成功')
    showMarkDialog.value = false
    Object.assign(markForm, { targetNo: '', riskType: '', riskDescription: '' })
    loadList()
  } finally {
    markLoading.value = false
  }
}

onMounted(loadList)
</script>
