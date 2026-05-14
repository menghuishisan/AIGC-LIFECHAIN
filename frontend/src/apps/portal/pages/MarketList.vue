<template>
  <!-- 内容市场列表页 -->
  <div class="market-list-page">
    <div class="page-header">
      <h2>内容市场</h2>
      <p class="page-subtitle">浏览已确权并可授权交易的作品</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索作品名称" clearable style="width: 220px" @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.workType" placeholder="作品类型" clearable style="width: 150px" @change="loadData">
        <el-option v-for="(label, value) in WorkTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
    </div>

    <!-- 作品卡片网格 -->
    <div class="market-grid" v-loading="loading">
      <div v-for="item in list" :key="item.workNo" class="market-card" @click="router.push(`/market/works/${item.workNo}`)">
        <div class="market-card__cover">
          <el-image :src="item.coverUrl" fit="cover" style="width: 100%; height: 200px; border-radius: 8px 8px 0 0;">
            <template #error>
              <div class="market-card__cover-placeholder">
                <el-icon size="48"><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </div>
        <div class="market-card__body">
          <div class="market-card__title">{{ item.title }}</div>
          <div class="market-card__meta">
            <el-tag size="small">{{ WorkTypeMap[item.workType] || item.workType }}</el-tag>
          </div>
          <div class="market-card__trust">
            <el-tag type="success" size="small" effect="plain">已确权</el-tag>
            <el-tag type="warning" size="small" effect="plain">链上锚定</el-tag>
          </div>
          <div class="market-card__hint">点击查看授权详情与价格</div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无上架作品" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[12, 24, 48]"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Picture, Search } from '@element-plus/icons-vue'
import { workApi } from '@/shared/api'
import { WorkTypeMap } from '@/shared/constants'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  workType: '',
  keyword: '',
  pageNo: 1,
  pageSize: 12
})

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getMarketWorks({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      workType: query.workType || undefined,
      keyword: query.keyword || undefined
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/* 从路由 query 中读取搜索关键词 */
watch(() => route.query.keyword, (val) => {
  query.keyword = (val as string) || ''
  query.pageNo = 1
  loadData()
})

onMounted(() => {
  if (route.query.keyword) {
    query.keyword = route.query.keyword as string
  }
  loadData()
})
</script>

<style scoped lang="scss">
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}
.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}
.market-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  overflow: hidden;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
  &__cover-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 200px;
    background: #f5f7fa;
    color: #c0c4cc;
  }
  &__body {
    padding: 12px 16px 16px;
  }
  &__title {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }
  &__trust {
    display: flex;
    gap: 6px;
    margin-bottom: 6px;
  }
  &__hint {
    font-size: 12px;
    color: #c0c4cc;
  }
  &__price {
    font-size: 16px;
    font-weight: 600;
    color: #E6524B;
  }
  &__license {
    font-size: 12px;
    color: #909399;
  }
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
