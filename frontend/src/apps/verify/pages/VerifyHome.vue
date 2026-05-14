<template>
  <!-- 公共验真首页 -->
  <div class="verify-home-page">
    <div class="verify-hero">
      <h1>AIGC 作品版权验真</h1>
      <p class="verify-sub">输入证书编号、作品编号或作品哈希，即可查询版权登记信息</p>
    </div>

    <div class="verify-form-card">
      <el-form :model="form" @submit.prevent="handleVerify">
        <el-form-item>
          <el-select v-model="form.queryType" style="width: 160px; margin-right: 12px;">
            <el-option label="证书编号" value="CERT_NO" />
            <el-option label="作品编号" value="WORK_NO" />
            <el-option label="作品哈希" value="WORK_HASH" />
            <el-option label="交易哈希" value="TX_HASH" />
          </el-select>
          <el-input
            v-model="form.queryValue"
            placeholder="请输入查询值"
            style="flex: 1; margin-right: 12px;"
            @keyup.enter="handleVerify"
            clearable
          />
          <el-button type="primary" :loading="loading" @click="handleVerify">
            <el-icon style="margin-right: 4px;"><Search /></el-icon>
            验真查询
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 使用说明 -->
    <div class="verify-tips">
      <h3>如何验真？</h3>
      <div class="tips-grid">
        <div class="tip-item">
          <div class="tip-number">1</div>
          <div class="tip-content">
            <strong>选择查询方式</strong>
            <p>支持通过证书编号、作品编号、作品哈希或交易哈希进行查询</p>
          </div>
        </div>
        <div class="tip-item">
          <div class="tip-number">2</div>
          <div class="tip-content">
            <strong>输入查询值</strong>
            <p>在输入框中粘贴或输入对应的编号/哈希值</p>
          </div>
        </div>
        <div class="tip-item">
          <div class="tip-number">3</div>
          <div class="tip-content">
            <strong>查看验真结果</strong>
            <p>系统将与区块链记录比对，返回版权登记详情</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { workApi } from '@/shared/api'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const form = reactive({ queryType: 'CERT_NO', queryValue: '' })

/** 提交验真查询 */
async function handleVerify() {
  if (!form.queryValue.trim()) {
    ElMessage.warning('请输入查询值')
    return
  }
  loading.value = true
  try {
    const res = await workApi.publicVerify({
      queryType: form.queryType,
      queryValue: form.queryValue.trim()
    })
    // 验真成功，跳转结果页
    if (res.data?.verified) {
      router.push({ path: '/verify/result', query: { queryType: form.queryType, queryValue: form.queryValue } })
    } else {
      router.push({ path: '/verify/not-found', query: { queryType: form.queryType, queryValue: form.queryValue } })
    }
  } catch {
    // 接口报错也跳转未找到页
    router.push({ path: '/verify/not-found', query: { queryType: form.queryType, queryValue: form.queryValue } })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.verify-home-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

.verify-hero {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-size: 32px;
    color: #132126;
    margin-bottom: 12px;
  }

  .verify-sub {
    font-size: 16px;
    color: #6B7D84;
  }
}

.verify-form-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  margin-bottom: 40px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__content) {
    display: flex;
    align-items: center;
  }
}

.verify-tips {
  h3 {
    font-size: 18px;
    margin-bottom: 20px;
    color: #333;
  }
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.tip-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.tip-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $brand-primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.tip-content {
  strong {
    display: block;
    margin-bottom: 4px;
    color: #132126;
  }

  p {
    font-size: 13px;
    color: #6B7D84;
    margin: 0;
    line-height: 1.5;
  }
}
</style>
