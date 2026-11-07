<template>
  <!-- 监管综合搜索页 -->
  <div data-page="RegulatorSearch" class="lc-page regulator-search-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>综合搜索</h1>
        <p class="page-header__desc">通过关键词和对象类型检索关联业务记录。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="输入编号、名称等关键词"
        clearable
        style="width: 320px"
        @keyup.enter="searchFromFirstPage"
      />
      <el-select
        aria-label="目标类型"
        v-model="query.targetType"
        placeholder="目标类型"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option
          v-for="(label, value) in TargetTypeMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <el-button type="primary" @click="searchFromFirstPage">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="objectType" label="类型" width="100">
        <template #default="{ row }">{{
          TargetTypeMap[row.objectType] || row.objectType
        }}</template>
      </el-table-column>
      <el-table-column prop="objectNo" label="编号" min-width="200">
        <template #default="{ row }"
          ><span class="font-mono">{{ row.objectNo }}</span></template
        >
      </el-table-column>
      <el-table-column prop="title" label="名称/摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="120" />
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
import type { RegulatorSearchResultVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { appApi } from '@/shared/api'
import { TargetTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const loading = ref(false)
const list = ref<RegulatorSearchResultVO[]>([])
const total = ref(0)
const query = reactive({ keyword: '', targetType: '', pageNo: 1, pageSize: 10 })
const route = useRoute()

watch(
  () => route.query.keyword,
  (value) => {
    query.keyword = typeof value === 'string' ? value : ''
    query.pageNo = 1
    if (query.keyword) void loadData()
  },
  { immediate: true }
)

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await appApi.regulatorSearch({
        ...query,
        keyword: query.keyword || undefined,
        targetType: query.targetType || undefined
      })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}
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
