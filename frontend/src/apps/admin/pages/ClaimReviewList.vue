<template>
  <!-- 确权审核列表页（管理员） -->
  <div class="claim-review-list-page">
    <div class="page-header"><h2>确权审核</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="确权状态" clearable style="width: 180px" @change="loadData">
        <el-option v-for="(label, value) in ClaimStatusMap" :key="value" :label="label" :value="value" />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="确权编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/claims/${row.claimNo}`)">{{ row.claimNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="作品名称" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.workTitle || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联作品" min-width="160">
        <template #default="{ row }"><span class="font-mono">{{ row.workNo }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }"><StatusTag :status="row.status" type="claim" /></template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatTime(row.submitTime || row.createdAt) }}</template>
      </el-table-column>
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
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { ClaimStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import type { ClaimListVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const list = ref<ClaimListVO[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getAdminClaims({ ...query, status: query.status || undefined })
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
