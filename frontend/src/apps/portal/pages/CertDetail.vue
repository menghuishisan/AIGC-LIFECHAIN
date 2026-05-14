<template>
  <!-- 证书详情页 -->
  <div class="cert-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader
        :title="`版权证书 ${detail.basicInfo.certNo}`"
        :biz-no="detail.basicInfo.certNo"
        :status="detail.statusInfo.status"
        status-type="cert"
      >
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('DOWNLOAD')" type="primary" @click="handleDownload">
            下载证书
          </el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <!-- 证书基础信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">证书信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="证书编号"><span class="font-mono">{{ detail.basicInfo.certNo }}</span></el-descriptions-item>
              <el-descriptions-item label="证书哈希"><span class="font-mono">{{ detail.basicInfo.certHash || '-' }}</span></el-descriptions-item>
              <el-descriptions-item label="版本">v{{ detail.basicInfo.version || 1 }}</el-descriptions-item>
              <el-descriptions-item label="证书文件">
                <el-button v-if="detail.basicInfo.certFileUrl" link type="primary" @click="handleDownload">点击下载</el-button>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- 状态与时间 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态与时间</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态">
                <StatusTag :status="detail.statusInfo.status" type="cert" />
              </el-descriptions-item>
              <el-descriptions-item label="签发时间">{{ formatTime(detail.timeInfo.issueTime) }}</el-descriptions-item>
              <el-descriptions-item label="过期时间">{{ formatTime(detail.timeInfo.expireTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 关联信息 -->
      <div class="lc-card" style="margin-top: 20px;">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="关联作品">
            <el-button link type="primary" @click="router.push(`/creator/works/${detail.relationInfo.workNo}`)">
              {{ detail.relationInfo.workNo }}
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="确权编号">
            <el-button link type="primary" @click="router.push(`/creator/claims/${detail.relationInfo.claimNo}`)">
              {{ detail.relationInfo.claimNo }}
            </el-button>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <div style="margin-top: 20px;">
        <ChainInfoCard :chain-info="detail.chainInfo" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { workApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard } from '@/shared/components'
import { formatTime } from '@/shared/utils'
import type { CertDetailVO } from '@/shared/types'

const props = defineProps<{ certNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<CertDetailVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getCertDetail(props.certNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

/** 下载证书 */
async function handleDownload() {
  await workApi.downloadCert(props.certNo)
}

onMounted(loadData)
</script>
