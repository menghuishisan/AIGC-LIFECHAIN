<template>
  <!-- 上传作品页：三段式步骤表单 -->
  <div class="work-create-page">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">上传作品</h2>
        <p class="page-header__desc">上传文件 → 填写基本信息 → 填写 AIGC 信息</p>
      </div>
    </div>

    <!-- 步骤条 -->
    <el-steps :active="currentStep" align-center style="margin-bottom: 24px;">
      <el-step title="上传文件" />
      <el-step title="基本信息" />
      <el-step title="AIGC 信息" />
    </el-steps>

    <!-- 步骤1：文件上传 -->
    <div class="lc-card" v-show="currentStep === 0">
      <div class="lc-card__title">文件上传</div>
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        :file-list="fileList"
        accept="image/*,audio/*,video/*,.pdf,.doc,.docx"
      >
        <el-icon style="font-size: 48px; color: #8A9AA1"><UploadFilled /></el-icon>
        <div style="margin-top: 8px; color: #5E727A">拖拽文件到这里 或 点击上传</div>
        <template #tip>
          <div style="font-size: 12px; color: #8A9AA1; margin-top: 8px">支持图片 / 音频 / 视频 / 文档</div>
        </template>
      </el-upload>
      <div style="margin-top: 24px; text-align: right">
        <el-button type="primary" @click="currentStep = 1" :disabled="!fileList.length">下一步</el-button>
      </div>
    </div>

    <!-- 步骤2：基本信息 -->
    <div class="lc-card" v-show="currentStep === 1">
      <div class="lc-card__title">基本信息</div>
      <el-form ref="basicFormRef" :model="form" :rules="basicRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入作品标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="workType">
          <el-select v-model="form.workType" placeholder="请选择作品类型">
            <el-option v-for="(label, key) in WorkTypeMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入作品描述" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="coverUrl" placeholder="请输入封面图片 URL（可选）" />
        </el-form-item>
      </el-form>
      <div style="margin-top: 24px; text-align: right; display: flex; gap: 12px; justify-content: flex-end">
        <el-button @click="currentStep = 0">上一步</el-button>
        <el-button type="primary" @click="goStep3">下一步</el-button>
      </div>
    </div>

    <!-- 步骤3：AIGC 信息 -->
    <div class="lc-card" v-show="currentStep === 2">
      <div class="lc-card__title">AIGC 信息</div>
      <el-form ref="aigcFormRef" :model="form" :rules="aigcRules" label-width="120px">
        <el-form-item label="模型名称">
          <el-input v-model="form.aigcModel" placeholder="如 GPT-4、Stable Diffusion" />
        </el-form-item>
        <el-form-item label="AIGC 工具">
          <el-input v-model="form.aigcTool" placeholder="使用的 AIGC 工具" />
        </el-form-item>
        <el-form-item label="生成时间">
          <el-date-picker v-model="form.generationTime" type="datetime" placeholder="选择生成时间" />
        </el-form-item>
        <el-form-item label="Prompt 摘要">
          <el-input v-model="form.promptSummary" type="textarea" :rows="3" placeholder="生成时使用的 Prompt 摘要" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <div style="margin-top: 24px; text-align: right; display: flex; gap: 12px; justify-content: flex-end">
        <el-button @click="currentStep = 1">上一步</el-button>
        <el-button @click="handleSaveDraft" :loading="loading">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">提交作品</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadFile } from 'element-plus'
import { workApi } from '@/shared/api'
import { WorkTypeMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const currentStep = ref(0)
const basicFormRef = ref<FormInstance>()
const aigcFormRef = ref<FormInstance>()
const fileList = ref<UploadFile[]>([])
const coverUrl = ref('')

const form = reactive({
  title: '',
  workType: '',
  description: '',
  aigcTool: '',
  aigcModel: '',
  generationTime: '',
  promptSummary: ''
})

const basicRules = {
  title: [{ required: true, message: '请输入作品标题', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择作品类型', trigger: 'change' }]
}

const aigcRules = {
  aigcModel: [{ max: 100, message: '模型名称不超过100个字符', trigger: 'blur' }],
  aigcTool: [{ max: 100, message: 'AIGC工具不超过100个字符', trigger: 'blur' }],
  promptSummary: [{ max: 1000, message: 'Prompt摘要不超过1000个字符', trigger: 'blur' }]
}

function handleFileChange(file: UploadFile) {
  fileList.value = [file]
}

async function goStep3() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (valid) currentStep.value = 2
}

/** 提交作品 */
async function handleSubmit() {
  const aigcValid = await aigcFormRef.value?.validate().catch(() => false)
  if (aigcValid === false) return
  loading.value = true
  try {
    const formData = new FormData()
    if (fileList.value[0]?.raw) {
      formData.append('files', fileList.value[0].raw)
    }

    /* 构建 request JSON part（后端 @RequestPart("request") 需要 JSON Blob） */
    const requestPayload: Record<string, any> = {
      title: form.title,
      workType: form.workType,
      requestId: generateRequestId()
    }
    if (form.description) requestPayload.description = form.description
    if (coverUrl.value) requestPayload.coverUrl = coverUrl.value

    /* AIGC 元数据 */
    if (form.aigcModel || form.aigcTool || form.promptSummary) {
      const aigcMeta: Record<string, any> = {}
      if (form.aigcModel) aigcMeta.aigcModel = form.aigcModel
      if (form.aigcTool) aigcMeta.aigcTool = form.aigcTool
      if (form.promptSummary) aigcMeta.promptSummary = form.promptSummary
      if (form.generationTime) aigcMeta.generationTime = new Date(form.generationTime).toISOString()
      requestPayload.aigcMeta = aigcMeta
    }

    formData.append('request', new Blob([JSON.stringify(requestPayload)], { type: 'application/json' }))

    const res = await workApi.uploadWork(formData)
    ElMessage.success('作品上传成功')
    router.push(`/creator/works/${res.data.basicInfo.workNo}`)
  } finally {
    loading.value = false
  }
}

/** 保存草稿（同提交但显示不同消息） */
async function handleSaveDraft() {
  await handleSubmit()
}
</script>
