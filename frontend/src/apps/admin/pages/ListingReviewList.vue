<template>
  <!-- 上架审核列表页（管理员） -->
  <div data-page="ListingReviewList" class="lc-page listing-review-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>上架审核</h1>
        <p class="page-header__desc">审核作品授权条款与上架申请。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-select
        aria-label="审核状态"
        v-model="query.reviewStatus"
        placeholder="审核状态"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="listingNo" label="上架编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/listings/${row.listingNo}`)">{{
            row.listingNo
          }}</el-button>
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
        <template #default="{ row }"><StatusTag :status="row.status" type="listing" /></template>
      </el-table-column>
      <el-table-column prop="creatorAccountNo" label="创作者" width="160" show-overflow-tooltip />
    </el-table>

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
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatCurrency } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<ListingDetailVO[]>([])
const total = ref(0)
const query = reactive({ reviewStatus: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await tradeApi.getAdminListings({
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        reviewStatus: query.reviewStatus || undefined
      })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
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
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
