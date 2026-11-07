<template>
  <!-- 分账模板列表页 -->
  <div data-page="SettleTemplateList" class="lc-page settle-template-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>分账模板</h1>
        <p class="page-header__desc">维护可复用的分账比例与参与角色。</p>
      </div>
      <el-button type="primary" @click="router.push('/admin/settle-templates/create')"
        >新建模板</el-button
      >
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="templateCode" label="模板编码" min-width="180">
        <template #default="{ row }">
          <span class="font-mono">{{ row.templateCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="templateName" label="模板名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{
            row.status
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { appApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import type { SettleTemplateVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<SettleTemplateVO[]>([])

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await appApi.getSettleTemplates()
      if (!isCurrent()) return
      list.value = res.data || []
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
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
</style>
