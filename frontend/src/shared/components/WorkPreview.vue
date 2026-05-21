<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
const audioRef = ref<HTMLAudioElement | null>(null)
const videoRef = ref<HTMLVideoElement | null>(null)
const timeLimitReached = ref(false)

const primaryFile = computed(() =>
  props.files?.find(f => f.purpose === 'ORIGINAL') ?? props.files?.[0]
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
  const file = primaryFile.value
  if (!file) return
  loading.value = true
  try {
    const api = props.isMarket ? getMarketPreviewUrl : getPreviewUrl
    const res = await api(props.workNo, file.fileId)
    preview.value = res.data
  } catch {
    preview.value = null
  } finally {
    loading.value = false
  }
}

function onTimeUpdate(el: HTMLAudioElement | HTMLVideoElement) {
  if (preview.value?.previewDurationSeconds && el.currentTime >= preview.value.previewDurationSeconds) {
    el.pause()
    timeLimitReached.value = true
  }
}

function openDownload() {
  if (preview.value?.previewUrl) {
    window.open(preview.value.previewUrl, '_blank')
  }
}

onMounted(loadPreview)
watch(() => props.workNo, loadPreview)
</script>

<template>
  <div class="work-preview">
    <div v-if="loading" class="preview-loading">
      <el-skeleton :rows="4" animated />
    </div>

    <!-- IMAGE -->
    <template v-else-if="mediaCategory === 'image'">
      <div class="preview-image-wrapper">
        <el-image
          :src="preview?.previewUrl || coverUrl"
          :preview-src-list="preview?.accessLevel === 'FULL' && preview?.previewUrl ? [preview.previewUrl] : undefined"
          fit="contain"
          class="preview-image"
        />
        <div v-if="preview?.accessLevel === 'LIMITED'" class="watermark-overlay">
          <span v-for="i in 6" :key="i">PREVIEW</span>
        </div>
      </div>
    </template>

    <!-- AUDIO -->
    <template v-else-if="mediaCategory === 'audio'">
      <div class="preview-audio">
        <div class="audio-cover">
          <el-image v-if="coverUrl" :src="coverUrl" fit="cover" class="audio-cover-img" />
          <el-icon v-else :size="64" class="audio-icon"><Headset /></el-icon>
        </div>
        <div v-if="preview?.previewUrl" class="audio-player">
          <audio
            ref="audioRef"
            :src="preview.previewUrl"
            controls
            @timeupdate="onTimeUpdate(audioRef!)"
          />
          <div v-if="timeLimitReached" class="time-limit-notice">
            <el-tag type="warning">试听结束（{{ preview.previewDurationSeconds }}秒），购买后可收听完整版</el-tag>
          </div>
        </div>
        <div v-else class="no-preview">
          <el-tag>购买后可收听完整音频</el-tag>
        </div>
      </div>
    </template>

    <!-- VIDEO -->
    <template v-else-if="mediaCategory === 'video'">
      <div class="preview-video">
        <div v-if="preview?.previewUrl" class="video-player">
          <video
            ref="videoRef"
            :src="preview.previewUrl"
            controls
            :poster="coverUrl"
            @timeupdate="onTimeUpdate(videoRef!)"
          />
          <div v-if="timeLimitReached" class="time-limit-notice">
            <el-tag type="warning">预览结束（{{ preview.previewDurationSeconds }}秒），购买后可观看完整版</el-tag>
          </div>
        </div>
        <div v-else class="no-preview">
          <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="preview-image" />
          <el-tag>购买后可观看完整视频</el-tag>
        </div>
      </div>
    </template>

    <!-- TEXT / MODEL / OTHER -->
    <template v-else>
      <div class="preview-file-info">
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="preview-image" />
        <div v-if="primaryFile" class="file-meta">
          <p><strong>{{ primaryFile.fileName }}</strong></p>
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

.watermark-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 40px;
  pointer-events: none;
  span {
    font-size: 24px;
    font-weight: bold;
    color: rgba(0, 0, 0, 0.08);
    transform: rotate(-30deg);
    user-select: none;
  }
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
