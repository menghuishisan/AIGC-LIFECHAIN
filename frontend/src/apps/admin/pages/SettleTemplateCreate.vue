<template>
  <!-- 新建分账模板 -->
  <div data-page="SettleTemplateCreate" class="lc-page settle-template-create-page">
    <div class="page-header">
      <el-button link @click="router.back()">&lt; 返回</el-button>
      <div>
        <h1>新建分账模板</h1>
        <p class="page-header__desc">定义分配比例，所有分账项合计须为100%。</p>
      </div>
    </div>

    <div class="lc-card" style="max-width: 680px">
      <el-form :model="form" label-width="100px" label-position="top">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="模板说明（可选）"
          />
        </el-form-item>

        <el-form-item label="分账项">
          <div v-for="(item, idx) in form.items" :key="idx" class="ratio-row">
            <el-select
              v-model="item.roleType"
              :aria-label="`第${idx + 1}项分账角色`"
              placeholder="角色"
            >
              <el-option label="创作者" value="CREATOR" />
              <el-option label="平台" value="PLATFORM" />
            </el-select>
            <el-input-number
              v-model="item.ratio"
              :aria-label="`第${idx + 1}项分账比例`"
              :min="0"
              :max="1"
              :step="0.01"
              :precision="4"
              placeholder="比例"
            />
            <el-input
              v-model="item.description"
              :aria-label="`第${idx + 1}项说明`"
              placeholder="说明（可选）"
            />
            <el-button
              type="danger"
              link
              :aria-label="`删除第${idx + 1}项`"
              @click="form.items.splice(idx, 1)"
              >删除</el-button
            >
          </div>
          <el-button @click="form.items.push({ roleType: 'CREATOR', ratio: 0, description: '' })"
            >添加分账项</el-button
          >
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
  items: [
    { roleType: 'CREATOR', ratio: 0.7, description: '' },
    { roleType: 'PLATFORM', ratio: 0.3, description: '' }
  ]
})

async function handleSubmit() {
  if (submitting.value) return
  if (!form.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('至少需要一个分账项')
    return
  }
  const ratios = form.items.map((item) => Math.round(Number(item.ratio) * 10000))
  if (
    ratios.some((value) => !Number.isInteger(value) || value < 0 || value > 10000) ||
    ratios.reduce((total, ratio) => total + ratio, 0) !== 10000
  ) {
    ElMessage.warning('分账比例须在0至1之间且合计为1')
    return
  }
  if (new Set(form.items.map((item) => item.roleType)).size !== form.items.length) {
    ElMessage.warning('同一角色只保留一个分账项')
    return
  }
  submitting.value = true
  try {
    await appApi.createSettleTemplate({
      templateName: form.templateName,
      description: form.description || undefined,
      items: form.items.map((i) => ({
        roleType: i.roleType,
        ratio: i.ratio,
        description: i.description || undefined
      })),
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
.ratio-row {
  display: grid;
  grid-template-columns: 1fr 140px 1fr auto;
  gap: 8px;
  width: 100%;
  margin-bottom: 12px;
  > * {
    min-width: 0;
  }
  .el-input-number {
    width: 100%;
  }
}
@media (max-width: 760px) {
  .ratio-row {
    grid-template-columns: 1fr 1fr;
  }
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
