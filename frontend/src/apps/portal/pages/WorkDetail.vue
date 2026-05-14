<template>
  <!-- 作品详情页 -->
  <div class="work-detail-page" v-loading="loading">
    <template v-if="detail">
      <!-- 详情头部 -->
      <DetailHeader
        :title="detail.basicInfo.title"
        :biz-no="detail.basicInfo.workNo"
        :status="detail.statusInfo.status"
        status-type="work"
      >
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('EDIT')" @click="editWork">编辑</el-button>
          <el-button v-if="detail.allowedActions.includes('FEATURE_EXTRACT')" type="primary" @click="handleFeatureExtract">提取特征</el-button>
          <el-button v-if="detail.allowedActions.includes('SUBMIT_CLAIM')" type="primary" @click="handleSubmitClaim">提交确权</el-button>
          <el-button v-if="detail.allowedActions.includes('LISTING')" type="primary" @click="handleListing">申请上架</el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <!-- 基础信息卡 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">基础信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="类型">{{ WorkTypeMap[detail.basicInfo.workType] || detail.basicInfo.workType }}</el-descriptions-item>
              <el-descriptions-item label="描述">{{ detail.basicInfo.description || '-' }}</el-descriptions-item>
              <el-descriptions-item label="封面">
                <el-image v-if="detail.basicInfo.coverUrl" :src="detail.basicInfo.coverUrl" style="width: 120px; border-radius: 8px;" fit="cover" :preview-src-list="[detail.basicInfo.coverUrl]" />
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- 状态与动作卡 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态与动作</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态">
                <StatusTag :status="detail.statusInfo.status" type="work" />
              </el-descriptions-item>
              <el-descriptions-item label="允许操作">
                <el-tag v-for="action in detail.allowedActions" :key="action" size="small" style="margin: 2px">{{ action }}</el-tag>
                <span v-if="!detail.allowedActions.length">-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 关联对象卡 -->
          <div class="lc-card" style="margin-top: 20px;">
            <div class="lc-card__title">关联对象</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="确权编号">
                <el-button v-if="detail.relationInfo.claimNo" link type="primary" @click="router.push(`/creator/claims/${detail.relationInfo.claimNo}`)">
                  {{ detail.relationInfo.claimNo }}
                </el-button>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="证书编号">
                <el-button v-if="detail.relationInfo.certNo" link type="primary" @click="router.push(`/creator/certificates/${detail.relationInfo.certNo}`)">
                  {{ detail.relationInfo.certNo }}
                </el-button>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="上架编号">
                <span class="font-mono">{{ detail.relationInfo.listingNo || '-' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- AIGC 元数据 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="detail.basicInfo.aigcMeta">
        <div class="lc-card__title">AIGC 元数据</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模型名称">{{ detail.basicInfo.aigcMeta.aigcModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AIGC 工具">{{ detail.basicInfo.aigcMeta.aigcTool || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatTime(detail.basicInfo.aigcMeta.generationTime) }}</el-descriptions-item>
          <el-descriptions-item label="Prompt 摘要">{{ detail.basicInfo.aigcMeta.promptSummary || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 特征提取结果 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="feature">
        <div class="lc-card__title">特征提取结果</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="特征类型">{{ feature.featureType }}</el-descriptions-item>
          <el-descriptions-item label="感知哈希"><span class="font-mono">{{ feature.perceptualHash || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="提取状态">{{ feature.extractStatus }}</el-descriptions-item>
          <el-descriptions-item label="提取时间">{{ formatTime(feature.extractTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <div style="margin-top: 20px;">
        <ChainInfoCard :chain-info="detail.chainInfo" />
      </div>

      <!-- 生命周期轨迹 -->
      <div class="lc-card" style="margin-top: 20px;">
        <TraceTimeline title="生命周期轨迹" :events="traces" />
      </div>
    </template>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑作品信息" width="520px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="请输入作品标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入作品描述" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="editForm.coverUrl" placeholder="封面图片地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi, appApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard, TraceTimeline } from '@/shared/components'
import { WorkTypeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { WorkDetailVO, WorkFeatureVO, TraceEventVO } from '@/shared/types'

const props = defineProps<{ workNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<WorkDetailVO | null>(null)
const feature = ref<WorkFeatureVO | null>(null)
const traces = ref<TraceEventVO[]>([])

async function loadData() {
  loading.value = true
  try {
    const [detailRes, traceRes] = await Promise.all([
      workApi.getWorkDetail(props.workNo),
      appApi.getWorkTraces(props.workNo)
    ])
    detail.value = detailRes.data
    traces.value = traceRes.data || []

    /* 尝试加载特征 */
    try {
      const featureRes = await workApi.getWorkFeature(props.workNo)
      feature.value = featureRes.data
    } catch { /* 可能未提取 */ }
  } finally {
    loading.value = false
  }
}

function editWork() {
  if (!detail.value) return
  editForm.title = detail.value.basicInfo.title || ''
  editForm.description = detail.value.basicInfo.description || ''
  editForm.coverUrl = detail.value.basicInfo.coverUrl || ''
  showEditDialog.value = true
}

/* ========== 编辑表单 ========== */
const showEditDialog = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  title: '',
  description: '',
  coverUrl: ''
})

async function submitEdit() {
  editLoading.value = true
  try {
    await workApi.updateWorkMeta(props.workNo, {
      title: editForm.title || undefined,
      description: editForm.description || undefined,
      coverUrl: editForm.coverUrl || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('作品信息已更新')
    showEditDialog.value = false
    loadData()
  } finally {
    editLoading.value = false
  }
}

async function handleFeatureExtract() {
  await workApi.extractFeature(props.workNo, generateRequestId())
  ElMessage.success('特征提取请求已提交')
  loadData()
}

async function handleSubmitClaim() {
  await workApi.submitClaim({ workNo: props.workNo, requestId: generateRequestId() })
  ElMessage.success('确权申请已提交')
  loadData()
}

function handleListing() {
  router.push({ path: '/creator/listings/create', query: { workNo: props.workNo } })
}

onMounted(loadData)
</script>
