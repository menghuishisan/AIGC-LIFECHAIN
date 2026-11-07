<template>
  <!-- 证书管理页（管理员） -->
  <div data-page="CertManage" class="lc-page cert-manage-page">
    <div class="page-header">
      <div>
        <h1>证书管理</h1>
        <p class="page-header__desc">按证书编号查询登记内容与证书状态。</p>
      </div>
      <el-button type="primary" @click="router.push('/admin/certificates/generate')"
        >生成证书</el-button
      >
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchCertNo"
        placeholder="输入证书编号查询"
        clearable
        style="width: 300px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <PageLoadFeedback :message="pageError" @retry="handleSearch" />
    <div v-if="detail" class="lc-card" v-loading="loading" style="margin-top: 16px">
      <div class="lc-card__title">证书信息</div>
      <el-descriptions :column="compact ? 1 : 2" border size="small">
        <el-descriptions-item label="证书编号"
          ><span class="font-mono">{{ detail.basicInfo.certNo }}</span></el-descriptions-item
        >
        <el-descriptions-item label="状态"
          ><StatusTag :status="detail.statusInfo.status" type="cert"
        /></el-descriptions-item>
        <el-descriptions-item label="作品编号"
          ><span class="font-mono">{{ detail.relationInfo.workNo }}</span></el-descriptions-item
        >
        <el-descriptions-item label="确权编号"
          ><span class="font-mono">{{ detail.relationInfo.claimNo }}</span></el-descriptions-item
        >
        <el-descriptions-item label="证书哈希"
          ><span class="font-mono">{{
            detail.basicInfo.certHash || '-'
          }}</span></el-descriptions-item
        >
        <el-descriptions-item label="版本">{{
          detail.basicInfo.version || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="签发时间">{{
          formatTime(detail.timeInfo.issueTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{
          formatTime(detail.timeInfo.expireTime)
        }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.chainInfo" style="margin-top: 16px">
        <ChainInfoCard :chainInfo="detail.chainInfo" />
      </div>

      <div style="margin-top: 16px; text-align: right">
        <el-button @click="handleDownload">下载证书</el-button>
      </div>
    </div>

    <el-empty v-if="searched && !detail && !loading && !pageError" description="未找到该证书" />
  </div>
</template>

<script setup lang="ts">
import { useCompactViewport } from '@/shared/composables/useCompactViewport'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import ChainInfoCard from '@/shared/components/ChainInfoCard.vue'
import { formatTime } from '@/shared/utils'
import type { CertDetailVO } from '@/shared/types'
const compact = useCompactViewport()

const router = useRouter()
const loading = ref(false)
const searched = ref(false)
const searchCertNo = ref('')
const detail = ref<CertDetailVO | null>(null)
const { pageError, runPageLoad } = usePageLoad()

async function handleSearch() {
  if (!searchCertNo.value.trim()) {
    ElMessage.warning('请输入证书编号')
    return
  }
  loading.value = true
  searched.value = true
  detail.value = null
  await runPageLoad(async (isCurrent) => {
    try {
      const res = await workApi.getCertDetail(searchCertNo.value.trim())
      if (!isCurrent()) return
      detail.value = res.data
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

async function handleDownload() {
  if (!detail.value) return
  try {
    await workApi.downloadCert(detail.value.basicInfo.certNo)
  } catch {
    ElMessage.error('下载失败')
  }
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
