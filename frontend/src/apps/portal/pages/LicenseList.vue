<template>
  <!-- 我的授权列表页 -->
  <div data-page="LicenseList" class="lc-page license-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>我的授权</h1>
        <p class="page-header__desc">查看已购买作品的授权状态与有效期限。</p>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="basicInfo.licenseNo" label="授权编号" min-width="180">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="router.push(`/buyer/licenses/${row.basicInfo.licenseNo}`)"
            >{{ row.basicInfo.licenseNo }}</el-button
          >
        </template>
      </el-table-column>
      <el-table-column prop="basicInfo.licenseType" label="授权类型" width="120">
        <template #default="{ row }">{{
          LicenseTypeMap[row.basicInfo.licenseType] || row.basicInfo.licenseType
        }}</template>
      </el-table-column>
      <el-table-column prop="statusInfo.licenseStatus" label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.statusInfo.licenseStatus" type="license" />
        </template>
      </el-table-column>
      <el-table-column prop="timeInfo.effectiveTime" label="生效时间" width="180">
        <template #default="{ row }">{{ formatTime(row.timeInfo.effectiveTime) }}</template>
      </el-table-column>
      <el-table-column prop="timeInfo.expireTime" label="过期时间" width="180">
        <template #default="{ row }">{{ formatTime(row.timeInfo.expireTime) }}</template>
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
import type { LicenseDetailVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<LicenseDetailVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await tradeApi.getMyLicenses(query)
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
