<template>
  <!-- 新建授权模板页 -->
  <div class="license-template-create-page">
    <div class="page-header">
      <h2>新建授权模板</h2>
    </div>

    <div class="lc-card" style="max-width: 720px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" label-position="top">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>

        <el-form-item label="授权类型" prop="licenseType">
          <el-select v-model="form.licenseType" placeholder="请选择授权类型" style="width: 100%">
            <el-option v-for="(label, value) in LicenseTypeMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>

        <el-form-item label="授权范围描述" prop="scopeDescription">
          <el-input v-model="form.scopeDescription" type="textarea" :rows="3" placeholder="请描述授权范围" />
        </el-form-item>

        <el-form-item label="授权天数">
          <el-input-number v-model="form.durationDays" :min="1" :max="3650" style="width: 100%" placeholder="永久授权不填" />
        </el-form-item>

        <el-form-item label="建议价格（元）">
          <el-input-number v-model="priceYuan" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="描述说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="备注说明（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">创建模板</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { LicenseTypeMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const priceYuan = ref(0)

const form = reactive({
  templateName: '',
  licenseType: '',
  scopeDescription: '',
  durationDays: undefined as number | undefined,
  description: ''
})

const rules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  scopeDescription: [{ required: true, message: '请描述授权范围', trigger: 'blur' }]
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await tradeApi.createLicenseTemplate({
      templateName: form.templateName,
      licenseType: form.licenseType,
      scopeDescription: form.scopeDescription,
      durationDays: form.durationDays,
      priceAmount: priceYuan.value ? Math.round(priceYuan.value * 100) : undefined,
      description: form.description || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('授权模板创建成功')
    router.push('/creator/license-templates')
  } finally {
    submitting.value = false
  }
}
</script>
