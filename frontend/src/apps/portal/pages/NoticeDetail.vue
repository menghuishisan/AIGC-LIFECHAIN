<template>
  <!-- 通知详情页 -->
  <div class="notice-detail-page" v-loading="loading">
    <template v-if="notice">
      <div class="page-header">
        <el-button link @click="router.back()">&lt; 返回</el-button>
      </div>

      <div class="lc-card notice-content-card">
        <h2 class="notice-title">{{ notice.title }}</h2>
        <div class="notice-meta">
          <el-tag size="small">{{ notice.noticeType }}</el-tag>
          <span class="notice-time">{{ formatTime(notice.createdAt) }}</span>
        </div>
        <el-divider />
        <div class="notice-body" v-html="sanitizedContent" />

        <!-- 关联业务跳转 -->
        <div class="notice-biz" v-if="notice.bizType && notice.bizNo">
          <el-divider />
          <p>
            关联业务：
            <el-button link type="primary" @click="goBiz">{{ BizTypeMap[notice.bizType] || notice.bizType }} - {{ notice.bizNo }}</el-button>
          </p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { appApi } from '@/shared/api'
import { BizTypeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { MessageNoticeVO } from '@/shared/types'

const props = defineProps<{ noticeNo: string }>()
const router = useRouter()
const loading = ref(false)
const notice = ref<MessageNoticeVO | null>(null)

/** 防XSS：简单文本转义 */
const sanitizedContent = computed(() => {
  if (!notice.value?.content) return ''
  return notice.value.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br />')
})

async function loadData() {
  loading.value = true
  try {
    const res = await appApi.getNoticeDetail(props.noticeNo)
    notice.value = res.data
    /* 标记已读 */
    if (!res.data.readFlag) {
      await appApi.markNoticeRead(props.noticeNo, generateRequestId())
      notice.value!.readFlag = true
    }
  } finally {
    loading.value = false
  }
}

/** 关联业务跳转 */
function goBiz() {
  if (!notice.value) return
  const { bizType, bizNo } = notice.value
  const routeMap: Record<string, string> = {
    WORK: `/creator/works/${bizNo}`,
    CLAIM: `/creator/claims/${bizNo}`,
    CERTIFICATE: `/creator/certificates/${bizNo}`,
    ORDER: `/orders/${bizNo}`,
    LICENSE: `/buyer/licenses/${bizNo}`
  }
  const path = routeMap[bizType || '']
  if (path) router.push(path)
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.notice-content-card {
  max-width: 800px;
}
.notice-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 12px;
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.notice-time {
  font-size: 13px;
  color: #909399;
}
.notice-body {
  line-height: 1.8;
  color: #606266;
}
</style>
