<template>
  <!-- 新建分账模板 -->
  <div class="settle-template-create-page">
    <div class="page-header">
      <el-button link @click="router.back()">&lt; 返回</el-button>
      <h2>新建分账模板</h2>
    </div>

    <div class="lc-card" style="max-width: 680px;">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="模板说明（可选）" />
        </el-form-item>

        <el-form-item label="分账项">
          <div v-for="(item, idx) in form.items" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px; width: 100%;">
            <el-select v-model="item.roleType" placeholder="角色" style="width: 140px;">
              <el-option label="创作者" value="CREATOR" />
              <el-option label="平台" value="PLATFORM" />
            </el-select>
            <el-input-number v-model="item.ratio" :min="0" :max="1" :step="0.01" :precision="2" placeholder="比例" style="width: 140px;" />
            <el-input v-model="item.description" placeholder="说明（可选）" style="flex: 1;" />
            <el-button type="danger" link @click="form.items.splice(idx, 1)">删除</el-button>
          </div>
          <el-button @click="form.items.push({ roleType: 'CREATOR', ratio: 0, description: '' })">添加分账项</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">创建模板</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appApi } from '@/shared/api'
import { generateRequestId } from '@/shared/utils'

const router = useRouter()
const submitting = ref(false)

const form = reactive({
  templateName: '',
  description: '',
  items: [{ roleType: 'CREATOR', ratio: 0.7, description: '' }, { roleType: 'PLATFORM', ratio: 0.3, description: '' }]
})

async function handleSubmit() {
  if (!form.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('至少需要一个分账项')
    return
  }
  submitting.value = true
  try {
    await appApi.createSettleTemplate({
      templateName: form.templateName,
      description: form.description || undefined,
      items: form.items.map(i => ({ roleType: i.roleType, ratio: i.ratio, description: i.description || undefined })),
      requestId: generateRequestId()
    })
    ElMessage.success('模板创建成功')
    router.push('/admin/settle-templates')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
