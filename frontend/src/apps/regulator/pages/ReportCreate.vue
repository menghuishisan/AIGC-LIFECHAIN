<template>
  <!-- 创建监管报告页 -->
  <div class="report-create-page">
    <div class="page-header">
      <h2>创建监管报告</h2>
    </div>

    <div class="lc-card" style="max-width: 720px;">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="报告类型" prop="reportType">
          <el-select v-model="form.reportType" style="width: 100%">
            <el-option label="日常巡检报告" value="ROUTINE_INSPECTION" />
            <el-option label="风险评估报告" value="RISK_ASSESSMENT" />
            <el-option label="争议处理报告" value="DISPUTE_RESOLUTION" />
            <el-option label="专项治理报告" value="SPECIAL_GOVERNANCE" />
            <el-option label="综合统计报告" value="COMPREHENSIVE_STATS" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告标题" prop="reportTitle">
          <el-input v-model="form.reportTitle" maxlength="200" show-word-limit placeholder="请输入报告标题" />
        </el-form-item>
        <el-form-item label="关联目标类型">
          <el-select v-model="form.targetType" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="(label, key) in TargetTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联目标编号" v-if="form.targetType">
          <el-input v-model="form.targetNo" placeholder="如作品编号、订单编号等" />
        </el-form-item>
        <el-form-item label="报告内容" prop="reportContent">
          <el-input
            v-model="form.reportContent"
            type="textarea"
            :rows="12"
            placeholder="请输入报告内容"
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit(false)">保存草稿</el-button>
          <el-button type="success" :loading="generateLoading" @click="handleSubmit(true)">创建并生成</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { TargetTypeMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const generateLoading = ref(false)

const form = reactive({
  reportType: 'ROUTINE_INSPECTION',
  reportTitle: '',
  reportContent: '',
  targetType: '',
  targetNo: ''
})

const rules: FormRules = {
  reportType: [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  reportTitle: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
  reportContent: [{ required: true, message: '请输入报告内容', trigger: 'blur' }]
}

async function handleSubmit(autoGenerate: boolean) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    reportType: form.reportType,
    reportTitle: form.reportTitle,
    reportContent: form.reportContent,
    targetType: form.targetType || undefined,
    targetNo: form.targetNo || undefined,
    requestId: generateRequestId()
  }

  if (autoGenerate) {
    generateLoading.value = true
    try {
      const res = await regulatorApi.generateReport(payload)
      ElMessage.success('报告创建并生成成功')
      router.push(`/regulator/reports/${res.data.reportNo}`)
    } finally {
      generateLoading.value = false
    }
  } else {
    submitLoading.value = true
    try {
      const res = await regulatorApi.createReport(payload)
      ElMessage.success('报告草稿保存成功')
      router.push(`/regulator/reports/${res.data.reportNo}`)
    } finally {
      submitLoading.value = false
    }
  }
}
</script>
