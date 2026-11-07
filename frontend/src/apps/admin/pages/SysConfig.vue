<template>
  <!-- 系统配置页（管理员） -->
  <div data-page="SysConfig" class="lc-page sys-config-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>系统配置</h1>
        <p class="page-header__desc">维护平台业务配置，保存后以服务端记录为准。</p>
      </div>
      <el-button type="primary" @click="openAdd">新增配置</el-button>
    </div>

    <el-table :data="configs" v-loading="loading" stripe>
      <el-table-column prop="configKey" label="配置键" min-width="200">
        <template #default="{ row }"
          ><span class="font-mono">{{ row.configKey }}</span></template
        >
      </el-table-column>
      <el-table-column prop="configValue" label="配置值" min-width="240" show-overflow-tooltip />
      <el-table-column prop="configType" label="类型" width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row.configKey)"
            >编辑</el-button
          >
          <el-button link type="danger" size="small" @click="handleDelete(row.configKey)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑配置' : '新增配置'" width="520px">
      <el-form :model="form" label-width="80px" label-position="top">
        <el-form-item label="配置键" required>
          <el-input v-model="form.configKey" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="form.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.configType" placeholder="如 STRING, JSON, NUMBER" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { appApi } from '@/shared/api'
import type { SysConfigVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const loading = ref(false)
const saving = ref(false)
const configs = ref<SysConfigVO[]>([])
const showDialog = ref(false)
const isEdit = ref(false)

const form = reactive({
  configKey: '',
  configValue: '',
  configType: '',
  description: ''
})

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await appApi.getConfigs()
      if (!isCurrent()) return
      configs.value = res.data
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

function openAdd() {
  isEdit.value = false
  form.configKey = ''
  form.configValue = ''
  form.configType = ''
  form.description = ''
  showDialog.value = true
}

function openEdit(configKey: string) {
  const row = configs.value.find((config) => config.configKey === configKey)
  if (!row) return
  isEdit.value = true
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.configType = row.configType || ''
  form.description = row.description || ''
  showDialog.value = true
}

async function handleSave() {
  if (saving.value) return
  if (!form.configKey.trim() || !form.configValue.trim()) {
    ElMessage.warning('请填写配置键与配置值')
    return
  }
  saving.value = true
  try {
    await appApi.upsertConfig({
      configKey: form.configKey,
      configValue: form.configValue,
      configType: form.configType || undefined,
      description: form.description || undefined
    })
    ElMessage.success('保存成功')
    showDialog.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(key: string) {
  if (
    !(await ElMessageBox.confirm(`确定要删除配置 "${key}" 吗？`, '删除配置', { type: 'warning' })
      .then(() => true)
      .catch(() => false))
  )
    return
  await appApi.deleteConfig(key)
  ElMessage.success('已删除')
  loadData()
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
