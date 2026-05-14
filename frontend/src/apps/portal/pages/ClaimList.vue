<template>
  <!-- 确权列表页 -->
  <div class="claim-list-page">
    <div class="page-header">
      <h2>我的确权</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="确权状态" clearable style="width: 180px" @change="loadData">
        <el-option v-for="(label, value) in ClaimStatusMap" :key="value" :label="label" :value="value" />
      </el-select>
    </div>

    <!-- 列表表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="claimNo" label="确权编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/creator/claims/${row.claimNo}`)">{{ row.claimNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="workNo" label="关联作品" min-width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/creator/works/${row.workNo}`)">{{ row.workNo }}</el-button>
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
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { ClaimStatusMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getMyClaims({ ...query, status: query.status || undefined })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
