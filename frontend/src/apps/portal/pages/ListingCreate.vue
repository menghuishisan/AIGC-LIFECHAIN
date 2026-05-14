<template>
  <!-- 上架申请页 -->
  <div class="listing-create-page">
    <div class="page-header">
      <h2>申请上架</h2>
    </div>

    <div class="lc-card" style="max-width: 720px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" label-position="top">
        <!-- 关联作品 -->
        <el-form-item label="关联作品编号" prop="workNo">
          <el-input v-model="form.workNo" placeholder="请输入已确权通过的作品编号" :disabled="!!presetWorkNo" />
        </el-form-item>

        <!-- 授权类型 -->
        <el-form-item label="授权类型" prop="licenseType">
          <el-select v-model="form.licenseType" placeholder="请选择授权类型" style="width: 100%">
            <el-option v-for="(label, value) in LicenseTypeMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>

        <!-- 选择授权模板（可选） -->
        <el-form-item label="授权模板（可选）">
          <el-select v-model="form.licenseTemplateCode" placeholder="选择已有模板" clearable style="width: 100%" @change="onTemplateChange">
            <el-option v-for="t in templates" :key="t.templateCode" :label="t.templateName" :value="t.templateCode" />
          </el-select>
        </el-form-item>

        <!-- 价格 -->
        <el-form-item label="价格（元）" prop="priceAmount">
          <el-input-number v-model="priceYuan" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>

        <!-- 授权范围描述 -->
        <el-form-item label="授权范围描述">
          <el-input v-model="form.scopeDescription" type="textarea" :rows="3" placeholder="请描述授权范围" />
        </el-form-item>

        <!-- 授权周期 -->
        <el-form-item label="授权天数">
          <el-input-number v-model="form.durationDays" :min="1" :max="3650" style="width: 100%" placeholder="永久授权不填" />
        </el-form-item>

        <!-- 提交 -->
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交上架申请</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { LicenseTypeMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'
import type { LicenseTemplateVO } from '@/shared/types'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const templates = ref<LicenseTemplateVO[]>([])

/* 从路由查询参数预设作品编号 */
const presetWorkNo = route.query.workNo as string || ''

/* 价格（元），与表单分×100 */
const priceYuan = ref(0)

const form = reactive({
  workNo: presetWorkNo,
  licenseType: '',
  licenseTemplateCode: '',
  priceAmount: 0,
  scopeDescription: '',
  durationDays: undefined as number | undefined
})

/* 价格同步（元→分） */
const formPriceFen = computed(() => Math.round(priceYuan.value * 100))

const rules: FormRules = {
  workNo: [{ required: true, message: '请输入作品编号', trigger: 'blur' }],
  licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  priceAmount: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

/** 加载授权模板 */
async function loadTemplates() {
  try {
    const res = await tradeApi.getLicenseTemplates({ pageNo: 1, pageSize: 100 })
    templates.value = res.data.records
  } catch { /* 可能无模板 */ }
}

/** 模板变更——自动填充 */
function onTemplateChange(code: string) {
  const tpl = templates.value.find(t => t.templateCode === code)
  if (tpl) {
    form.licenseType = tpl.licenseType
    form.scopeDescription = tpl.scopeDescription
    if (tpl.durationDays) form.durationDays = tpl.durationDays
    if (tpl.priceAmount) priceYuan.value = tpl.priceAmount / 100
  }
}

/** 提交 */
async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await tradeApi.createListing(form.workNo, {
      licenseTemplateCode: form.licenseTemplateCode || undefined,
      licenseType: form.licenseType,
      priceAmount: formPriceFen.value,
      scopeDescription: form.scopeDescription || undefined,
      durationDays: form.durationDays,
      requestId: generateRequestId()
    })
    ElMessage.success('上架申请已提交')
    router.push('/creator/listings')
  } finally {
    submitting.value = false
  }
}

onMounted(loadTemplates)
</script>
