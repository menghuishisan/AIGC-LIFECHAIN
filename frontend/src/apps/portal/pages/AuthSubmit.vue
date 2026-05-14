<template>
  <!-- 实名认证提交页 -->
  <div class="auth-submit-page">
    <div class="page-header">
      <h2>实名认证</h2>
    </div>

    <div class="lc-card" style="max-width: 720px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" label-position="top">
        <!-- 主体类型 -->
        <el-form-item label="主体类型" prop="subjectType">
          <el-radio-group v-model="form.subjectType">
            <el-radio value="PERSONAL">个人</el-radio>
            <el-radio value="ENTERPRISE">企业</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 真实姓名 -->
        <el-form-item label="真实姓名 / 企业名称" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名或企业名称" />
        </el-form-item>

        <!-- 证件类型 -->
        <el-form-item label="证件类型" prop="idCardType">
          <el-select v-model="form.idCardType" placeholder="请选择证件类型" style="width: 100%;">
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="护照" value="PASSPORT" />
            <el-option label="营业执照" value="BUSINESS_LICENSE" />
          </el-select>
        </el-form-item>

        <!-- 证件号码 -->
        <el-form-item label="证件号码" prop="idCardNo">
          <el-input v-model="form.idCardNo" placeholder="请输入证件号码" />
        </el-form-item>

        <!-- 企业字段（仅企业认证显示） -->
        <template v-if="form.subjectType === 'ENTERPRISE'">
          <el-form-item label="统一社会信用代码">
            <el-input v-model="form.enterpriseCode" placeholder="请输入统一社会信用代码" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </template>

        <!-- 认证材料 -->
        <el-form-item label="认证材料URL">
          <el-input v-model="form.authMaterialUrl" placeholder="请输入认证材料附件URL" />
          <div class="el-form-item__tip">请上传证件照片后填入附件地址</div>
        </el-form-item>

        <!-- 提交 -->
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交认证</el-button>
          <el-button @click="router.back()">返回</el-button>
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
import { authApi } from '@/shared/api'
import { generateRequestId } from '@/shared/utils'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  subjectType: 'PERSONAL',
  realName: '',
  idCardType: 'ID_CARD',
  idCardNo: '',
  enterpriseCode: '',
  contactName: '',
  contactPhone: '',
  authMaterialUrl: ''
})

const rules: FormRules = {
  subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCardType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idCardNo: [{ required: true, message: '请输入证件号码', trigger: 'blur' }]
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await authApi.submitAuth({
      subjectType: form.subjectType,
      realName: form.realName,
      idCardType: form.idCardType,
      idCardNo: form.idCardNo,
      enterpriseCode: form.enterpriseCode || undefined,
      contactName: form.contactName || undefined,
      contactPhone: form.contactPhone || undefined,
      authMaterialUrl: form.authMaterialUrl || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('认证材料已提交，请等待审核')
    router.push('/profile')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.el-form-item__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
