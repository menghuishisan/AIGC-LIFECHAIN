<template>
  <!-- 我的授权列表页 -->
  <div class="license-list-page">
    <div class="page-header">
      <h2>我的授权</h2>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="licenseNo" label="授权编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/buyer/licenses/${row.licenseNo}`)">{{ row.licenseNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="licenseType" label="授权类型" width="120">
        <template #default="{ row }">{{ LicenseTypeMap[row.licenseType] || row.licenseType }}</template>
      </el-table-column>
      <el-table-column prop="licenseStatus" label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.licenseStatus" type="license" />
        </template>
      </el-table-column>
      <el-table-column prop="effectiveTime" label="生效时间" width="180">
        <template #default="{ row }">{{ formatTime(row.effectiveTime) }}</template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" width="180">
        <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getMyLicenses(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
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
