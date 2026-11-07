<template>
  <!-- 争议管理列表页（管理员） -->
  <div data-page="DisputeList" class="lc-page dispute-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>争议管理</h1>
        <p class="page-header__desc">按案件状态定位待受理、待补证和处理中的争议。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-select
        aria-label="争议状态"
        v-model="query.status"
        placeholder="争议状态"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option
          v-for="(label, value) in DisputeStatusMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="caseNo" label="案件编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/disputes/${row.caseNo}`)">{{
            row.caseNo
          }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="disputeType" label="类型" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusTag :status="row.status" type="dispute" /></template>
      </el-table-column>
      <el-table-column
        prop="applicantAccountNo"
        label="申请人"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column
        prop="respondentAccountNo"
        label="被申请人"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
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
import type { RegulatorDisputeListVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { DisputeStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<RegulatorDisputeListVO[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await regulatorApi.getRegulatorDisputes({
        ...query,
        status: query.status || undefined
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
