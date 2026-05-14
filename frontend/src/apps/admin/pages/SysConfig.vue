<template>
  <!-- 系统配置页（管理员） -->
  <div class="sys-config-page">
    <div class="page-header">
      <h2>系统配置</h2>
      <el-button type="primary" @click="openAdd">新增配置</el-button>
    </div>

    <el-table :data="configs" v-loading="loading" stripe>
      <el-table-column prop="configKey" label="配置键" min-width="200">
        <template #default="{ row }"><span class="font-mono">{{ row.configKey }}</span></template>
      </el-table-column>
      <el-table-column prop="configValue" label="配置值" min-width="240" show-overflow-tooltip />
      <el-table-column prop="configType" label="类型" width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.configKey)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑配置' : '新增配置'" width="520px">
      <el-form :model="form" label-width="80px">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { appApi } from '@/shared/api'
import type { SysConfigVO } from '@/shared/types'

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
  loading.value = true
  try {
    const res = await appApi.getConfigs()
    configs.value = res.data
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  form.configKey = ''
  form.configValue = ''
  form.configType = ''
  form.description = ''
  showDialog.value = true
}

function openEdit(row: SysConfigVO) {
  isEdit.value = true
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.configType = row.configType || ''
  form.description = row.description || ''
  showDialog.value = true
}

async function handleSave() {
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
  await ElMessageBox.confirm(`确定要删除配置 "${key}" 吗？`, '删除配置', { type: 'warning' })
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
