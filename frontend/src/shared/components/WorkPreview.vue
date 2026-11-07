<script setup lang="ts">
import { ref, computed, onBeforeUnmount, watch } from 'vue'
import { Headset } from '@element-plus/icons-vue'
import { getPreviewUrl, getMarketPreviewUrl } from '@/shared/api/work'
import type { WorkFileVO, PreviewUrlVO } from '@/shared/types'
const props = defineProps<{
  workNo: string
  workType: string
  files?: WorkFileVO[]
  coverUrl?: string
  accessLevel: 'FULL' | 'LIMITED'
  isMarket?: boolean
}>()

const preview = ref<PreviewUrlVO | null>(null)
const loading = ref(false)
const timeLimitReached = ref(false)
const previewError = ref('')
let loadVersion = 0

const primaryFile = computed(
  () => props.files?.find((f) => f.purpose === 'ORIGINAL') ?? props.files?.[0]
)

const mediaCategory = computed(() => {
  const t = props.workType?.toUpperCase()
  if (t === 'IMAGE') return 'image'
  if (t === 'AUDIO') return 'audio'
  if (t === 'VIDEO') return 'video'
  if (t === 'TEXT') return 'text'
  if (t === 'MODEL') return 'model'
  return 'other'
})

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function loadPreview() {
  const version = ++loadVersion
  // 切换作品或文件时立即丢弃旧预览，迟到的响应不能覆盖新作品。
  preview.value = null
  previewError.value = ''
  timeLimitReached.value = false
  const file = primaryFile.value
  loading.value = false
  if (!file) return
  loading.value = true
  try {
    const api = props.isMarket ? getMarketPreviewUrl : getPreviewUrl
    const res = await api(props.workNo, file.fileId)
    if (version === loadVersion) preview.value = res.data
  } catch (error) {
    if (version === loadVersion)
      previewError.value = error instanceof Error ? error.message : '预览加载失败'
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function openDownload() {
  if (preview.value?.previewUrl) {
    window.open(preview.value.previewUrl, '_blank', 'noopener,noreferrer')
  }
}

watch(
  () => [props.workNo, primaryFile.value?.fileId, props.isMarket, props.accessLevel],
  loadPreview,
  { immediate: true }
)
onBeforeUnmount(() => {
  loadVersion += 1
})
</script>

<template>
  <div class="work-preview">
    <div v-if="loading" class="preview-loading">
      <el-skeleton :rows="4" animated />
    </div>

    <div v-else-if="previewError" class="preview-error" role="alert">
      <p>{{ previewError }}</p>
      <el-button @click="loadPreview">重新加载预览</el-button>
    </div>

    <el-empty
      v-else-if="mediaCategory === 'image' && !preview?.previewUrl && !coverUrl"
      description="暂无预览图片"
      :image-size="48"
    />

    <!-- 图片水印由服务端写入像素，前端仅展示生成后的副本。 -->
    <template v-else-if="mediaCategory === 'image'">
      <div class="preview-image-wrapper">
        <el-image
          alt="作品预览"
          :src="preview?.previewUrl || coverUrl"
          :preview-src-list="
            preview?.accessLevel === 'FULL' && preview?.previewUrl
              ? [preview.previewUrl]
              : undefined
          "
          fit="contain"
          class="preview-image"
        />
      </div>
    </template>

    <!-- 音频预览 -->
    <template v-else-if="mediaCategory === 'audio'">
      <div class="preview-audio">
        <div class="audio-cover">
          <el-image
            v-if="coverUrl"
            :src="coverUrl"
            alt="音频封面"
            fit="cover"
            class="audio-cover-img"
          />
          <el-icon v-else :size="64" class="audio-icon"><Headset /></el-icon>
        </div>
        <div v-if="preview?.previewUrl" class="audio-player">
          <audio
            :src="preview.previewUrl"
            controls
            aria-label="作品音频预览"
            @ended="timeLimitReached = preview?.accessLevel === 'LIMITED'"
          />
          <div v-if="timeLimitReached" class="time-limit-notice">
            <el-tag type="warning">试听片段已结束，购买后可收听完整版</el-tag>
          </div>
        </div>
        <div v-else class="no-preview">
          <el-tag>购买后可收听完整音频</el-tag>
        </div>
      </div>
    </template>

    <!-- 视频预览 -->
    <template v-else-if="mediaCategory === 'video'">
      <div class="preview-video">
        <div v-if="preview?.previewUrl" class="video-player">
          <video
            :src="preview.previewUrl"
            controls
            aria-label="作品视频预览"
            :poster="coverUrl"
            @ended="timeLimitReached = preview?.accessLevel === 'LIMITED'"
          />
          <div v-if="timeLimitReached" class="time-limit-notice">
            <el-tag type="warning">预览片段已结束，购买后可观看完整版</el-tag>
          </div>
        </div>
        <div v-else class="no-preview">
          <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="preview-image" />
          <el-tag>购买后可观看完整视频</el-tag>
        </div>
      </div>
    </template>

    <!-- 文本、模型及其他文件 -->
    <template v-else>
      <div class="preview-file-info">
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="preview-image" />
        <div v-if="primaryFile" class="file-meta">
          <p>
            <strong>{{ primaryFile.fileName }}</strong>
          </p>
          <p>类型：{{ primaryFile.fileType }} | 大小：{{ formatFileSize(primaryFile.fileSize) }}</p>
          <el-button
            v-if="preview?.accessLevel === 'FULL' && preview?.previewUrl"
            type="primary"
            @click="openDownload"
          >
            下载文件
          </el-button>
          <el-tag v-else type="info">购买后可下载完整文件</el-tag>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.work-preview {
  width: 100%;
}

.preview-loading {
  padding: 20px;
}

.preview-image-wrapper {
  position: relative;
  width: 100%;
  min-height: 200px;
}

.preview-image {
  width: 100%;
  max-height: 500px;
}

.preview-audio {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.audio-cover {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.audio-cover-img {
  width: 100%;
  height: 100%;
}

.audio-player {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  audio {
    width: 100%;
    max-width: 400px;
  }
}

.preview-video {
  width: 100%;
  .video-player {
    position: relative;
    video {
      width: 100%;
      max-height: 500px;
      background: #000;
    }
  }
}

.time-limit-notice {
  text-align: center;
  margin-top: 8px;
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.preview-file-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
  .file-meta {
    text-align: center;
    p {
      margin: 4px 0;
      color: #606266;
    }
  }
}
</style>
