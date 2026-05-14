<template>
  <!-- 冻结记录列表页 -->
  <div class="freeze-list-page" v-loading="loading">
    <div class="page-header">
      <h2>冻结管理</h2>
      <el-button type="danger" @click="showFreezeDialog = true">发起冻结</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.targetType" placeholder="目标类型" clearable @change="loadList" style="width: 140px">
        <el-option v-for="(label, key) in TargetTypeMap" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.status" placeholder="冻结状态" clearable @change="loadList" style="width: 140px">
        <el-option v-for="(label, key) in FreezeStatusMap" :key="key" :label="label" :value="key" />
      </el-select>
    </div>

    <!-- 列表 -->
    <el-table :data="list" stripe>
      <el-table-column prop="freezeNo" label="冻结编号" width="200">
        <template #default="{ row }">
          <router-link :to="`/regulator/freezes/${row.freezeNo}`" class="link">{{ row.freezeNo }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="目标类型" width="100">
        <template #default="{ row }">{{ TargetTypeMap[row.targetType] || row.targetType }}</template>
      </el-table-column>
      <el-table-column prop="targetNo" label="目标编号" width="200" />
      <el-table-column label="冻结模式" width="140">
        <template #default="{ row }">{{ FreezeModeMap[row.freezeMode] || row.freezeMode }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.freezeStatus" type="freeze" /></template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.applyTime || row.createdAt) }}</template>
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

    <!-- 发起冻结对话框 -->
    <el-dialog v-model="showFreezeDialog" title="发起冻结" width="520px">
      <el-form :model="freezeForm" label-width="100px">
        <el-form-item label="冻结模式" required>
          <el-radio-group v-model="freezeForm.freezeMode">
            <el-radio value="REVIEW_REQUIRED">需复核</el-radio>
            <el-radio value="REGULATOR_DIRECT">紧急直接冻结</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标类型" required>
          <el-select v-model="freezeForm.targetType" style="width: 100%">
            <el-option v-for="(label, key) in TargetTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标编号" required>
          <el-input v-model="freezeForm.targetNo" placeholder="如作品编号、订单编号等" />
        </el-form-item>
        <el-form-item label="冻结原因" required>
          <el-input v-model="freezeForm.freezeReason" type="textarea" :rows="3" placeholder="请说明冻结原因" />
        </el-form-item>
        <el-form-item label="原因编码">
          <el-input v-model="freezeForm.reasonCode" placeholder="可选，如 RISK_HIGH" />
        </el-form-item>
        <el-form-item label="紧急依据编号" v-if="freezeForm.freezeMode === 'REGULATOR_DIRECT'">
          <el-input v-model="freezeForm.urgentBasisNo" placeholder="紧急冻结的法律/政策依据编号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFreezeDialog = false">取消</el-button>
        <el-button type="danger" :loading="freezeLoading" @click="handleFreeze">提交冻结</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { regulatorApi } from '@/shared/api'
import { TargetTypeMap, FreezeStatusMap, FreezeModeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import type { FreezeRecordVO } from '@/shared/types'

const loading = ref(false)
const list = ref<FreezeRecordVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20, targetType: '', status: '' })

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: query.pageNo, pageSize: query.pageSize }
    if (query.targetType) params.targetType = query.targetType
    if (query.status) params.status = query.status
    const res = await regulatorApi.getFreezeList(params as any)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/* ========== 发起冻结 ========== */
const showFreezeDialog = ref(false)
const freezeLoading = ref(false)
const freezeForm = reactive({
  targetType: 'WORK',
  targetNo: '',
  freezeMode: 'REVIEW_REQUIRED',
  freezeReason: '',
  reasonCode: '',
  urgentBasisNo: ''
})

async function handleFreeze() {
  if (!freezeForm.targetNo || !freezeForm.freezeReason) return
  freezeLoading.value = true
  try {
    const payload = {
      targetType: freezeForm.targetType,
      targetNo: freezeForm.targetNo,
      freezeMode: freezeForm.freezeMode,
      freezeReason: freezeForm.freezeReason,
      reasonCode: freezeForm.reasonCode || undefined,
      urgentBasisNo: freezeForm.urgentBasisNo || undefined,
      requestId: generateRequestId()
    }
    // 根据冻结模式调用不同接口
    if (freezeForm.freezeMode === 'REGULATOR_DIRECT') {
      await regulatorApi.directFreeze(payload)
    } else {
      await regulatorApi.applyFreeze(payload)
    }
    ElMessage.success('冻结申请提交成功')
    showFreezeDialog.value = false
    Object.assign(freezeForm, { targetNo: '', freezeReason: '', reasonCode: '', urgentBasisNo: '' })
    loadList()
  } finally {
    freezeLoading.value = false
  }
}

onMounted(loadList)
</script>
