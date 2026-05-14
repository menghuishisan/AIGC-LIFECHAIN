<template>
  <!-- 授权模板列表页 -->
  <div class="license-template-list-page">
    <div class="page-header">
      <h2>授权模板</h2>
      <el-button type="primary" @click="router.push('/creator/license-templates/create')">新建模板</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="templateCode" label="模板编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/creator/license-templates/${row.templateCode}`)">{{ row.templateCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="licenseType" label="授权类型" width="120">
        <template #default="{ row }">{{ LicenseTypeMap[row.licenseType] || row.licenseType }}</template>
      </el-table-column>
      <el-table-column prop="priceAmount" label="建议价格" width="120">
        <template #default="{ row }">{{ row.priceAmount ? formatCurrency(row.priceAmount) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="durationDays" label="授权天数" width="100">
        <template #default="{ row }">{{ row.durationDays ? `${row.durationDays}天` : '永久' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
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
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime, formatCurrency } from '@/shared/utils'
import type { LicenseTemplateVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const list = ref<LicenseTemplateVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getLicenseTemplates(query)
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
