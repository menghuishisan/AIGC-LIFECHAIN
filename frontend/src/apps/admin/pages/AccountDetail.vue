<template>
  <!-- 账户详情页（管理员） -->
  <div class="account-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="detail.nickname" :biz-no="detail.accountNo" :status="detail.status" status-type="account">
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('REVIEW_AUTH')" type="primary" @click="showReviewDialog = true">审核认证</el-button>
          <el-button v-if="detail.status !== 'ACCOUNT_FROZEN'" type="danger" @click="handleFreeze">冻结</el-button>
          <el-button v-if="detail.status === 'ACCOUNT_FROZEN'" type="success" @click="handleUnfreeze">解冻</el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">基本信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="账户编号"><span class="font-mono">{{ detail.accountNo }}</span></el-descriptions-item>
              <el-descriptions-item label="手机号">{{ detail.mobile }}</el-descriptions-item>
              <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ AccountTypeMap[detail.accountType] || detail.accountType }}</el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag v-for="r in detail.roles" :key="r" size="small" style="margin: 2px;">{{ RoleMap[r] || r }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="账户状态"><StatusTag :status="detail.status" type="account" /></el-descriptions-item>
              <el-descriptions-item label="认证状态"><StatusTag :status="detail.authStatus" type="auth" /></el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- DID 信息 -->
          <div class="lc-card" style="margin-top: 16px;" v-if="detail.didInfo">
            <div class="lc-card__title">DID 信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="DID 编号"><span class="font-mono">{{ detail.didInfo.didNo }}</span></el-descriptions-item>
              <el-descriptions-item label="DID 值"><span class="font-mono">{{ detail.didInfo.didValue }}</span></el-descriptions-item>
              <el-descriptions-item label="DID 状态"><StatusTag :status="detail.didInfo.status" type="did" /></el-descriptions-item>
              <el-descriptions-item label="链上状态"><StatusTag :status="detail.didInfo.chainStatus" type="chain" /></el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 认证主体 -->
          <div class="lc-card" style="margin-top: 16px;" v-if="detail.subjectInfo">
            <div class="lc-card__title">认证主体</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="主体类型">{{ detail.subjectInfo.subjectType }}</el-descriptions-item>
              <el-descriptions-item label="真实姓名">{{ detail.subjectInfo.realName }}</el-descriptions-item>
              <el-descriptions-item label="证件类型">{{ detail.subjectInfo.idCardType }}</el-descriptions-item>
              <el-descriptions-item label="证件号码"><span class="font-mono">{{ detail.subjectInfo.idCardNo }}</span></el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </template>

    <!-- 认证审核对话框 -->
    <el-dialog v-model="showReviewDialog" title="认证审核" width="480px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.reviewResult">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReviewAuth">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '@/shared/api'
import { DetailHeader, StatusTag } from '@/shared/components'
import { AccountTypeMap, RoleMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'
import type { AccountProfileVO } from '@/shared/types'

const props = defineProps<{ accountNo: string }>()
const loading = ref(false)
const detail = ref<AccountProfileVO | null>(null)
const showReviewDialog = ref(false)
const reviewing = ref(false)
const reviewForm = reactive({ reviewResult: 'APPROVED', reviewComment: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await authApi.getAccountDetail(props.accountNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleReviewAuth() {
  reviewing.value = true
  try {
    await authApi.reviewAuth({
      accountNo: props.accountNo,
      reviewResult: reviewForm.reviewResult,
      reviewComment: reviewForm.reviewComment || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('审核已提交')
    showReviewDialog.value = false
    loadData()
  } finally {
    reviewing.value = false
  }
}

async function handleFreeze() {
  const res = await ElMessageBox.prompt('请输入冻结原因', '冻结账户', { inputType: 'textarea' })
  await authApi.freezeAccount({ accountNo: props.accountNo, reason: res.value, requestId: generateRequestId() })
  ElMessage.success('已冻结')
  loadData()
}

async function handleUnfreeze() {
  await ElMessageBox.confirm('确定要解冻该账户吗？', '解冻')
  await authApi.unfreezeAccount({ accountNo: props.accountNo, reason: '管理员解冻', requestId: generateRequestId() })
  ElMessage.success('已解冻')
  loadData()
}

onMounted(loadData)
</script>
