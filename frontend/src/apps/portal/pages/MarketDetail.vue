<template>
  <!-- 市场作品详情页 -->
  <div class="market-detail-page" v-loading="loading">
    <template v-if="detail">
      <el-row :gutter="24">
        <!-- 左侧：作品预览 -->
        <el-col :span="14">
          <div class="lc-card">
            <el-image
              :src="detail.basicInfo.coverUrl"
              fit="contain"
              style="width: 100%; max-height: 480px; border-radius: 8px;"
              :preview-src-list="detail.basicInfo.coverUrl ? [detail.basicInfo.coverUrl] : []"
            >
              <template #error>
                <div class="cover-placeholder">
                  <el-icon size="64"><Picture /></el-icon>
                  <p>暂无封面</p>
                </div>
              </template>
            </el-image>
          </div>
        </el-col>

        <!-- 右侧：信息与购买 -->
        <el-col :span="10">
          <div class="lc-card">
            <h2 class="detail-title">{{ detail.basicInfo.title }}</h2>
            <div class="detail-price" v-if="listing">{{ formatCurrency(listing.priceAmount) }}</div>

            <el-descriptions :column="1" size="small" style="margin-top: 16px;">
              <el-descriptions-item label="作品类型">{{ WorkTypeMap[detail.basicInfo.workType] || detail.basicInfo.workType }}</el-descriptions-item>
              <el-descriptions-item v-if="listing" label="授权类型">{{ LicenseTypeMap[listing.licenseType] || listing.licenseType }}</el-descriptions-item>
              <el-descriptions-item v-if="listing" label="授权范围">{{ listing.scopeDescription || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="listing" label="授权天数">{{ listing.durationDays ? `${listing.durationDays}天` : '永久' }}</el-descriptions-item>
              <el-descriptions-item v-if="listing" label="创作者"><span class="font-mono">{{ listing.creatorAccountNo || '-' }}</span></el-descriptions-item>
            </el-descriptions>

            <!-- 购买操作 -->
            <div class="buy-actions" style="margin-top: 24px;">
              <el-button type="primary" size="large" style="width: 100%;" :disabled="!listing" @click="handleBuy">
                立即购买
              </el-button>
            </div>
          </div>

          <!-- 作品描述 -->
          <div class="lc-card" style="margin-top: 16px;" v-if="detail.basicInfo.description">
            <div class="lc-card__title">作品描述</div>
            <p class="detail-description">{{ detail.basicInfo.description }}</p>
          </div>

          <!-- 验真信息 -->
          <div class="lc-card" style="margin-top: 16px;">
            <div class="lc-card__title">版权验真</div>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="证书编号">
                <span class="font-mono">{{ detail.relationInfo?.certNo || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="链上哈希">
                <span class="font-mono">{{ detail.chainInfo?.txHash || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="区块高度">
                {{ detail.chainInfo?.blockHeight || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { workApi, tradeApi } from '@/shared/api'
import { WorkTypeMap, LicenseTypeMap } from '@/shared/constants'
import { formatCurrency } from '@/shared/utils'
import type { WorkDetailVO, ListingDetailVO } from '@/shared/types'

const props = defineProps<{ workNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<WorkDetailVO | null>(null)
const listing = ref<ListingDetailVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getMarketWorkDetail(props.workNo)
    detail.value = res.data

    /* 如果有关联上架编号，加载上架详情获取价格和授权信息 */
    const listingNo = res.data?.relationInfo?.listingNo
    if (listingNo) {
      try {
        const listingRes = await tradeApi.getListingDetail(listingNo)
        listing.value = listingRes.data
      } catch { /* 上架信息加载失败降级 */ }
    }
  } finally {
    loading.value = false
  }
}

/** 立即购买——跳转到下单页 */
function handleBuy() {
  const listingNo = detail.value?.relationInfo?.listingNo
  if (!listingNo) return
  router.push({
    path: '/orders/create',
    query: { workNo: props.workNo, listingNo }
  })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 320px;
  background: #f5f7fa;
  color: #c0c4cc;
}
.detail-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}
.detail-price {
  font-size: 28px;
  font-weight: 700;
  color: #E6524B;
}
.detail-description {
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
