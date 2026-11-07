<template>
  <!-- 我的上架列表页 -->
  <div data-page="ListingList" class="lc-page listing-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>我的上架</h1>
        <p class="page-header__desc">跟踪上架审核与在售状态，管理作品授权。</p>
      </div>
      <el-button type="primary" @click="router.push('/creator/listings/create')"
        >申请上架</el-button
      >
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        aria-label="上架状态"
        v-model="query.status"
        placeholder="上架状态"
        clearable
        style="width: 180px"
        @change="searchFromFirstPage"
      >
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已上架" value="LISTED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已下架" value="UNLISTED" />
      </el-select>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="listingNo" label="上架编号" min-width="180">
        <template #default="{ row }">
          <span class="font-mono">{{ row.listingNo }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="workTitle" label="作品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="licenseType" label="授权类型" width="120">
        <template #default="{ row }">{{
          LicenseTypeMap[row.licenseType] || row.licenseType
        }}</template>
      </el-table-column>
      <el-table-column prop="priceAmount" label="价格" width="120">
        <template #default="{ row }">{{ formatCurrency(row.priceAmount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.status" type="listing" />
        </template>
      </el-table-column>
      <el-table-column prop="listTime" label="上架时间" width="180">
        <template #default="{ row }">{{ formatTime(row.listTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'LISTED'"
            link
            type="danger"
            size="small"
            @click="handleRemove(row.listingNo)"
            >下架</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <PagePagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import type { ListingDetailVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime, formatCurrency, generateRequestId } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<ListingDetailVO[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await tradeApi.getMyListings({ ...query, status: query.status || undefined })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

/** 下架 */
async function handleRemove(listingNo: string) {
  if (
    !(await ElMessageBox.confirm('确定要下架该作品吗？', '下架确认', { type: 'warning' })
      .then(() => true)
      .catch(() => false))
  )
    return
  await tradeApi.removeListing(listingNo, generateRequestId())
  ElMessage.success('下架成功')
  loadData()
}

onMounted(loadData)
/** 改变筛选条件后从第一页重新读取。 */
function searchFromFirstPage() {
  query.pageNo = 1
  void loadData()
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
