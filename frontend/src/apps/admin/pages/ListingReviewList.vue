<template>
  <!-- 上架审核列表页（管理员） -->
  <div class="listing-review-list-page">
    <div class="page-header"><h2>上架审核</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.reviewStatus" placeholder="审核状态" clearable style="width: 150px" @change="loadData">
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="listingNo" label="上架编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/listings/${row.listingNo}`)">{{ row.listingNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="workTitle" label="作品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="licenseType" label="授权类型" width="120">
        <template #default="{ row }">{{ LicenseTypeMap[row.licenseType] || row.licenseType }}</template>
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
      <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total"
        layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" @size-change="loadData" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatCurrency } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ reviewStatus: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getAdminListings({ pageNo: query.pageNo, pageSize: query.pageSize, reviewStatus: query.reviewStatus || undefined })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
