<template>
  <!-- 确权列表页 -->
  <div data-page="ClaimList" class="lc-page claim-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>我的确权</h1>
        <p class="page-header__desc">跟踪确权申请、审核结果与上链状态。</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        aria-label="确权状态"
        v-model="query.status"
        placeholder="确权状态"
        clearable
        style="width: 180px"
        @change="searchFromFirstPage"
      >
        <el-option
          v-for="(label, value) in ClaimStatusMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
    </div>

    <!-- 列表表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="claimNo" label="确权编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/creator/claims/${row.claimNo}`)">{{
            row.claimNo
          }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="workNo" label="关联作品" min-width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/creator/works/${row.workNo}`)">{{
            row.workNo
          }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="140">
        <template #default="{ row }">
          <StatusTag :status="row.status" type="claim" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
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
import type { ClaimListVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { ClaimStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<ClaimListVO[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await workApi.getMyClaims({ ...query, status: query.status || undefined })
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
