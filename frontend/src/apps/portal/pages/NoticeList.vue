<template>
  <!-- 通知列表页 -->
  <div data-page="NoticeList" class="lc-page notice-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>通知消息</h1>
        <p class="page-header__desc">查看业务进度通知与未读消息。</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        aria-label="消息类型"
        v-model="query.noticeType"
        placeholder="消息类型"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option label="系统通知" value="SYSTEM" />
        <el-option label="业务通知" value="BUSINESS" />
        <el-option label="审核通知" value="REVIEW" />
      </el-select>
      <el-select
        aria-label="阅读状态"
        v-model="readFilter"
        placeholder="阅读状态"
        clearable
        style="width: 150px"
        @change="loadData"
      >
        <el-option label="未读" :value="false" />
        <el-option label="已读" :value="true" />
      </el-select>
    </div>

    <!-- 列表 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row.noticeNo)">
            <el-badge v-if="!row.readFlag" is-dot>{{ row.title }}</el-badge>
            <span v-else>{{ row.title }}</span>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="noticeType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.noticeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="readFlag" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.readFlag ? 'info' : 'danger'" size="small">{{
            row.readFlag ? '已读' : '未读'
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180">
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
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { appApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import type { MessageNoticeVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<MessageNoticeVO[]>([])
const total = ref(0)
const readFilter = ref<boolean | undefined>(undefined)

const query = reactive({
  noticeType: '' as string,
  pageNo: 1,
  pageSize: 10
})

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await appApi.getNotices({
        ...query,
        noticeType: query.noticeType || undefined,
        readFlag: readFilter.value
      })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

function goDetail(noticeNo: string) {
  router.push(`/notices/${encodeURIComponent(noticeNo)}`)
}

onMounted(loadData)
/** 改变筛选条件后从第一页重新读取。 */
function searchFromFirstPage() {
  query.pageNo = 1
  void loadData()
}
</script>

<style scoped lang="scss">
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
