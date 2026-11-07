<template>
  <!-- 监管争议列表页 -->
  <div
    data-page="RegulatorDisputeList"
    class="lc-page regulator-dispute-list-page"
    v-loading="loading"
  >
    <PageLoadFeedback :message="pageError" @retry="loadList" />
    <div class="page-header">
      <div>
        <h1>争议管理</h1>
        <p class="page-header__desc">检索争议案件并跟踪调查进展。</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        aria-label="争议状态"
        v-model="query.status"
        placeholder="争议状态"
        clearable
        @change="searchFromFirstPage"
        style="width: 140px"
      >
        <el-option
          v-for="(label, key) in DisputeStatusMap"
          :key="key"
          :label="label"
          :value="key"
        />
      </el-select>
      <el-input
        v-model="query.disputeType"
        placeholder="争议类型"
        clearable
        @change="loadList"
        style="width: 160px"
      />
    </div>

    <!-- 列表 -->
    <el-table :data="list" stripe>
      <el-table-column prop="caseNo" label="案件编号" width="200">
        <template #default="{ row }">
          <router-link :to="`/regulator/disputes/${row.caseNo}`" class="link">{{
            row.caseNo
          }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="disputeType" label="争议类型" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" type="dispute" /></template>
      </el-table-column>
      <el-table-column prop="applicantAccountNo" label="申请方" width="200" show-overflow-tooltip />
      <el-table-column
        prop="respondentAccountNo"
        label="被申请方"
        width="200"
        show-overflow-tooltip
      />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <PagePagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @change="
        (p) => {
          query.pageNo = p
          loadList()
        }
      "
      style="margin-top: 16px; justify-content: flex-end"
    />
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { regulatorApi } from '@/shared/api'
import { DisputeStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import type { RegulatorDisputeListVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const loading = ref(false)
const list = ref<RegulatorDisputeListVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20, status: '', disputeType: '' })

async function loadList() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await regulatorApi.getRegulatorDisputes({
        pageNo: query.pageNo,
        pageSize: query.pageSize,
        status: query.status || undefined,
        disputeType: query.disputeType || undefined
      })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

onMounted(loadList)
/** 改变筛选条件后从第一页重新读取。 */
function searchFromFirstPage() {
  query.pageNo = 1
  void loadList()
}
</script>
