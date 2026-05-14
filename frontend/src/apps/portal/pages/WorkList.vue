<template>
  <!-- 我的作品列表页 -->
  <div class="work-list-page">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">我的作品</h2>
        <p class="page-header__desc">管理上传、特征提取、确权、证书和上架状态</p>
      </div>
      <el-button type="primary" @click="router.push('/creator/works/create')">上传作品</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 160px">
        <el-option v-for="(label, key) in WorkStatusMap" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="loadData">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 作品表格 -->
    <div class="lc-card">
      <el-table :data="records" stripe v-loading="loading" row-key="workNo">
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" alt="封面" />
            <div v-else style="width:48px;height:48px;border-radius:6px;background:#F0F3F5;display:flex;align-items:center;justify-content:center;font-size:11px;color:#8A9AA1">无</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="workNo" label="作品编号" width="180">
          <template #default="{ row }"><span class="font-mono">{{ row.workNo }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }"><StatusTag :status="row.status" type="work" /></template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ WorkTypeMap[row.workType] || row.workType }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="120">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/creator/works/${row.workNo}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @change="loadData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { WorkStatusMap, WorkTypeMap } from '@/shared/constants'
import { formatDate } from '@/shared/utils'
import type { WorkListVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const records = ref<WorkListVO[]>([])
const total = ref(0)

const query = reactive({ status: '', pageNo: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getMyWorks({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      status: query.status || undefined
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = ''
  query.pageNo = 1
  loadData()
}

onMounted(loadData)
</script>
