<template>
  <!-- 账户管理列表页 -->
  <div data-page="AccountList" class="lc-page account-list-page">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <div class="page-header">
      <div>
        <h1>账户管理</h1>
        <p class="page-header__desc">按账户状态、类型和关键词定位账户，进入详情完成审核与管理。</p>
      </div>
      <div>
        <el-button type="primary" @click="showCreateDialog('PLATFORM')">创建平台账户</el-button>
        <el-button @click="showCreateDialog('REGULATOR')">创建监管账户</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        aria-label="账户状态"
        v-model="query.status"
        placeholder="账户状态"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option
          v-for="(label, value) in AccountStatusMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <el-select
        aria-label="账户类型"
        v-model="query.accountType"
        placeholder="账户类型"
        clearable
        style="width: 150px"
        @change="searchFromFirstPage"
      >
        <el-option
          v-for="(label, value) in AccountTypeMap"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索手机号/昵称"
        clearable
        style="width: 200px"
        @keyup.enter="searchFromFirstPage"
      />
      <el-button type="primary" @click="searchFromFirstPage">查询</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="accountNo" label="账户编号" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/accounts/${row.accountNo}`)">{{
            row.accountNo
          }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="accountType" label="类型" width="100">
        <template #default="{ row }">{{
          AccountTypeMap[row.accountType] || row.accountType
        }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.status" type="account" />
        </template>
      </el-table-column>
      <el-table-column prop="authStatus" label="认证状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.authStatus" type="auth" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 'ACCOUNT_FROZEN'"
            link
            type="danger"
            size="small"
            @click="handleFreeze(row.accountNo)"
            >冻结</el-button
          >
          <el-button v-else link type="success" size="small" @click="handleUnfreeze(row.accountNo)"
            >解冻</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <PagePagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadData"
      />
    </div>

    <!-- 创建账户对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="createType === 'PLATFORM' ? '创建平台账户' : '创建监管账户'"
      width="480px"
    >
      <el-form :model="createForm" label-width="80px" label-position="top">
        <el-form-item label="手机号" required>
          <el-input v-model="createForm.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="createForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import PagePagination from '@/shared/components/PagePagination.vue'
import type { AccountProfileVO } from '@/shared/types'
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { AccountStatusMap, AccountTypeMap } from '@/shared/constants'
import { generateRequestId } from '@/shared/utils'
const { pageError, runPageLoad } = usePageLoad()

const router = useRouter()
const loading = ref(false)
const list = ref<AccountProfileVO[]>([])
const total = ref(0)
const query = reactive({ status: '', accountType: '', keyword: '', pageNo: 1, pageSize: 10 })

/* 创建对话框 */
const createDialogVisible = ref(false)
const createType = ref<'PLATFORM' | 'REGULATOR'>('PLATFORM')
const creating = ref(false)
const createForm = reactive({ mobile: '', password: '', nickname: '' })

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await authApi.getAccountList({
        ...query,
        status: query.status || undefined,
        accountType: query.accountType || undefined,
        keyword: query.keyword || undefined
      })
      if (!isCurrent()) return
      list.value = res.data.records
      total.value = res.data.total
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

function showCreateDialog(type: 'PLATFORM' | 'REGULATOR') {
  createType.value = type
  createForm.mobile = ''
  createForm.password = ''
  createForm.nickname = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (creating.value) return
  if (
    !/^1[3-9]\d{9}$/.test(createForm.mobile) ||
    !createForm.nickname.trim() ||
    createForm.password.length < 8 ||
    createForm.password.length > 32
  ) {
    ElMessage.warning('请填写正确手机号、昵称及8至32位密码')
    return
  }
  creating.value = true
  try {
    const fn =
      createType.value === 'PLATFORM'
        ? authApi.createPlatformAccount
        : authApi.createRegulatorAccount
    await fn({
      mobile: createForm.mobile,
      password: createForm.password,
      nickname: createForm.nickname,
      requestId: generateRequestId()
    })
    ElMessage.success('账户创建成功')
    createDialogVisible.value = false
    loadData()
  } finally {
    creating.value = false
  }
}

async function handleFreeze(accountNo: string) {
  const reason = await ElMessageBox.prompt('请输入冻结原因', '冻结账户', { inputType: 'textarea' })
  await authApi.freezeAccount({ accountNo, reason: reason.value, requestId: generateRequestId() })
  ElMessage.success('账户已冻结')
  loadData()
}

async function handleUnfreeze(accountNo: string) {
  if (
    !(await ElMessageBox.confirm('确定要解冻该账户吗？', '解冻账户')
      .then(() => true)
      .catch(() => false))
  )
    return
  await authApi.unfreezeAccount({ accountNo, reason: '管理员解冻', requestId: generateRequestId() })
  ElMessage.success('账户已解冻')
  loadData()
}

onMounted(loadData)
/** 改变筛选条件后从第一页重新读取。 */
function searchFromFirstPage() {
  query.pageNo = 1
  void loadData()
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
