<template>
  <!-- 验真未找到/未通过页 -->
  <div class="verify-not-found-page">
    <el-result icon="error" title="未找到匹配记录">
      <template #sub-title>
        <p>未找到与 <strong>{{ queryValue }}</strong> 匹配的版权登记记录</p>
        <p class="sub-hint">请检查输入是否正确，或确认查询方式是否匹配</p>
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.push('/verify')">重新查询</el-button>
        <el-button @click="$router.push('/verify/help')">查看帮助</el-button>
      </template>
    </el-result>

    <div class="tips-card">
      <h4>可能的原因</h4>
      <ul>
        <li>输入的编号或哈希值有误，请仔细核对</li>
        <li>查询方式与输入值不匹配（如选择了"证书编号"但输入的是作品编号）</li>
        <li>该作品尚未完成确权上链流程</li>
        <li>证书可能已被撤销或作废</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const queryValue = computed(() => (route.query.queryValue as string) || '(未知)')
</script>

<style lang="scss" scoped>
.verify-not-found-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 60px 20px;
}

.sub-hint {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}

.tips-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-top: 32px;

  h4 {
    font-size: 15px;
    margin-bottom: 12px;
    color: #333;
  }

  ul {
    padding-left: 20px;
    margin: 0;

    li {
      line-height: 2;
      color: #666;
      font-size: 14px;
    }
  }
}
</style>
