<template>
  <div data-page="MarketList" class="lc-page market-list-page">
    <header class="market-intro">
      <div>
        <p class="market-kicker">LIFECHAIN / CREATOR ECONOMY</p>
        <h1>内容市场</h1>
        <p class="market-subtitle">发现可授权的原创作品，查看作品信息与授权条款。</p>
      </div>
      <div class="market-count" aria-live="polite">
        <strong>{{ total }}</strong>
        <span>件在售作品</span>
      </div>
    </header>

    <el-form class="market-filters" :model="query" @submit.prevent="search" label-position="top">
      <el-input
        v-model="query.keyword"
        class="market-search"
        placeholder="搜索作品名称"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix
          ><el-icon><Search /></el-icon
        ></template>
      </el-input>
      <el-select
        aria-label="全部类型"
        v-model="query.workType"
        placeholder="全部类型"
        clearable
        @change="changeWorkType"
      >
        <el-option
          v-for="(label, value) in WorkTypeMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <el-button type="primary" native-type="submit" :loading="loading">搜索</el-button>
      <span class="market-filter-count">{{ total }} 个结果</span>
    </el-form>

    <el-alert
      v-if="errorMessage"
      class="market-error"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    >
      <template #default
        ><el-button text type="primary" @click="loadData">重试</el-button></template
      >
    </el-alert>

    <div v-if="loading && !list.length" class="market-grid" aria-busy="true">
      <el-skeleton v-for="index in 6" :key="index" animated class="market-skeleton">
        <template #template>
          <el-skeleton-item variant="image" class="market-skeleton__image" />
          <div class="market-skeleton__body">
            <el-skeleton-item variant="h3" />
            <el-skeleton-item variant="text" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="list.length" class="market-grid" :aria-busy="loading">
      <RouterLink
        v-for="item in list"
        :key="item.workNo"
        :to="`/market/works/${item.workNo}`"
        class="market-card"
      >
        <div class="market-card__cover">
          <el-image v-if="item.coverUrl" :src="item.coverUrl" fit="cover" lazy>
            <template #error>
              <div class="market-card__cover-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div v-else class="market-card__cover-placeholder">
            <el-icon><Picture /></el-icon>
          </div>
          <span class="market-card__status">已上架</span>
        </div>
        <div class="market-card__body">
          <div class="market-card__meta">{{ WorkTypeMap[item.workType] || item.workType }}</div>
          <h2 class="market-card__title">{{ item.title }}</h2>
          <div class="market-card__footer">
            <span class="font-mono">{{ item.workNo }}</span>
            <span
              >查看授权 <el-icon><ArrowRight /></el-icon
            ></span>
          </div>
        </div>
      </RouterLink>
    </div>
    <el-empty v-else-if="!loading" description="没有符合条件的在售作品" />

    <div v-if="total" class="pagination-wrap">
      <PagePagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[12, 24, 48]"
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import { ref, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Picture, Search } from '@element-plus/icons-vue'
import { workApi } from '@/shared/api'
import { WorkTypeMap } from '@/shared/constants'
import type { WorkListVO } from '@/shared/types'
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const list = ref<WorkListVO[]>([])
const total = ref(0)
const errorMessage = ref('')
let activeRequest = 0
const query = reactive({
  workType: '',
  keyword: '',
  pageNo: 1,
  pageSize: 12
})

async function loadData() {
  const requestId = ++activeRequest
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await workApi.getMarketWorks({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      workType: query.workType || undefined,
      keyword: query.keyword || undefined
    })
    if (requestId !== activeRequest) return
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    if (requestId !== activeRequest) return
    list.value = []
    total.value = 0
    errorMessage.value = '暂时无法加载市场作品，请检查网络后重试。'
  } finally {
    if (requestId === activeRequest) loading.value = false
  }
}

function search() {
  query.pageNo = 1
  const keyword = query.keyword.trim()
  const nextQuery = { ...route.query }
  if (keyword) nextQuery.keyword = keyword
  else delete nextQuery.keyword
  if (((route.query.keyword as string) || '') === keyword) {
    loadData()
  } else {
    router.replace({ path: route.path, query: nextQuery })
  }
}

function changeWorkType() {
  query.pageNo = 1
  loadData()
}

watch(
  () => route.query.keyword,
  (val) => {
    query.keyword = (val as string) || ''
    query.pageNo = 1
    loadData()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.market-list-page {
  width: 100%;
}

.market-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 0 28px;
  border-bottom: 1px solid var(--lc-line);
}

.market-intro h1 {
  margin: 4px 0 6px;
  color: var(--lc-ink);
  font-size: 28px;
  font-weight: 650;
}

.market-kicker {
  color: var(--lc-accent-strong);
  font: 600 11px/1.4 var(--lc-mono);
}

.market-subtitle {
  color: var(--lc-muted);
  font-size: 14px;
}

.market-count {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: var(--lc-muted);
  font-size: 12px;
  white-space: nowrap;
}

.market-count strong {
  color: var(--lc-ink);
  font: 600 24px/1.1 var(--lc-mono);
}

.market-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 0;
  border-bottom: 1px solid var(--lc-line);
}

.market-search {
  max-width: 360px;
}

.market-filters :deep(.el-select) {
  width: 180px;
}

.market-filter-count {
  margin-left: auto;
  color: var(--lc-muted);
  font-size: 12px;
}

.market-error {
  margin-top: 16px;
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 240px), 1fr));
  gap: 16px;
  padding-top: 20px;
}

.market-skeleton {
  border: 1px solid var(--lc-line);
  background: var(--lc-surface);
}

.market-skeleton__image {
  width: 100%;
  height: 190px;
}

.market-skeleton__body {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.market-card {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  text-decoration: none;
  background: #fff;
  border: 1px solid var(--lc-line);
  transition:
    border-color 160ms ease,
    transform 160ms ease;

  &:hover {
    border-color: var(--lc-accent);
    transform: translateY(-2px);
  }

  &:focus-visible {
    outline: 3px solid var(--lc-accent);
    outline-offset: 3px;
  }

  &__cover {
    position: relative;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    background: #e6ebe5;

    .el-image,
    :deep(img) {
      display: block;
      width: 100%;
      height: 100%;
    }
  }

  &__cover-placeholder {
    display: grid;
    width: 100%;
    height: 100%;
    place-items: center;
    color: #6f8278;
    font-size: 34px;
  }

  &__status {
    position: absolute;
    top: 12px;
    left: 12px;
    padding: 4px 8px;
    background: var(--lc-surface);
    color: var(--lc-accent-strong);
    font-size: 11px;
    font-weight: 600;
  }

  &__body {
    padding: 14px 16px 12px;
  }

  &__meta {
    color: var(--lc-accent-strong);
    font-size: 12px;
  }

  &__title {
    margin: 6px 0 16px;
    overflow: hidden;
    color: var(--lc-ink);
    font-size: 16px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    padding-top: 10px;
    border-top: 1px solid var(--lc-line);
    color: var(--lc-muted);
    font-size: 11px;

    span:last-child {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--lc-accent-strong);
      font-weight: 600;
    }
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .market-intro {
    align-items: flex-start;
    padding-top: 8px;
  }

  .market-intro h1 {
    font-size: 24px;
  }

  .market-filters {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(120px, 0.7fr);
  }

  .market-search {
    max-width: none;
    grid-column: 1 / -1;
  }

  .market-filters :deep(.el-select) {
    width: 100%;
  }

  .market-filter-count {
    display: none;
  }
}
</style>
