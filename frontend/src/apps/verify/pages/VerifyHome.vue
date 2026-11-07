<template>
  <div class="verify-home-page">
    <section class="verify-hero">
      <div class="verify-copy">
        <p class="verify-eyebrow"><span class="verify-eyebrow__mark" />PUBLIC RECORD / LIFECHAIN</p>
        <h1>作品权属，<br /><span>有据可查。</span></h1>
        <p class="verify-sub">按证书编号、作品编号或文件哈希，查询平台登记摘要。</p>

        <el-form class="verify-form" :model="form" @submit.prevent="handleVerify">
          <label for="verify-query-type">查询方式</label>
          <el-select
            id="verify-query-type"
            v-model="form.queryType"
            size="large"
            aria-label="查询方式"
          >
            <el-option label="证书编号" value="CERT_NO" />
            <el-option label="作品编号" value="WORK_NO" />
            <el-option label="文件哈希" value="FILE_HASH" />
          </el-select>
          <label for="verify-query-value">编号或哈希</label>
          <div class="verify-form__entry">
            <el-input
              id="verify-query-value"
              v-model="form.queryValue"
              placeholder="粘贴证书编号或哈希"
              size="large"
              clearable
              autocomplete="off"
            />
            <el-button type="primary" size="large" @click="handleVerify">
              <el-icon><Search /></el-icon>
              <span>核验记录</span>
            </el-button>
          </div>
        </el-form>
        <p class="verify-query-note">公开查询仅返回登记摘要，不展示原始作品或个人身份信息</p>
      </div>

      <aside class="verify-artifact" aria-label="公开登记记录示意">
        <div class="verify-artifact__top"><span>RIGHTS REGISTER</span><span>LC / PUBLIC</span></div>
        <div class="verify-artifact__seal">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <div class="verify-artifact__heading">
          <span>RECORD STATUS</span>
          <strong>登记摘要</strong>
        </div>
        <div class="verify-artifact__rows">
          <div>
            <span>登记索引</span><strong><i />可查询</strong>
          </div>
          <div><span>公开信息</span><strong>脱敏展示</strong></div>
          <div><span>证据索引</span><strong class="font-mono">SHA-256</strong></div>
        </div>
        <div class="verify-artifact__foot">
          <span>CONTENT AUTHENTICITY</span><span>01 — 04</span>
        </div>
      </aside>
    </section>

    <section class="verify-assurances" aria-label="核验信息">
      <div><strong>公开摘要</strong><span>仅展示核验所需的脱敏字段</span></div>
      <div><strong>证据比对</strong><span>登记信息与对应链上记录关联</span></div>
      <div><strong>结果可追溯</strong><span>记录编号与交易摘要可供复核</span></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled, Search } from '@element-plus/icons-vue'
const router = useRouter()
const form = reactive({ queryType: 'CERT_NO', queryValue: '' })

/** 提交验真查询 */
async function handleVerify() {
  if (!form.queryValue.trim()) {
    ElMessage.warning('请输入查询值')
    return
  }
  await router.push({
    path: '/verify/result',
    query: { queryType: form.queryType, queryValue: form.queryValue.trim() }
  })
}
</script>

<style lang="scss" scoped>
.verify-home-page {
  width: min(1120px, 100%);
  margin: 0 auto;
  padding: 28px 0 0;
}

.verify-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  align-items: center;
  gap: clamp(32px, 7vw, 88px);
  padding: 24px 0 44px;
  border-bottom: 1px solid var(--lc-line);
}

.verify-eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--lc-accent-strong);
  font: 600 11px/1.4 var(--lc-mono);
}

.verify-eyebrow__mark {
  width: 9px;
  height: 9px;
  border: 2px solid var(--lc-accent);
  border-radius: 50%;
}

.verify-copy h1 {
  margin: 20px 0 10px;
  color: var(--lc-ink);
  font-size: 40px;
  font-weight: 620;
  line-height: 1.18;
}

.verify-copy h1 span {
  color: var(--lc-accent-strong);
}

.verify-sub {
  max-width: 480px;
  color: var(--lc-muted);
  font-size: 15px;
  line-height: 1.7;
}

.verify-form {
  display: grid;
  gap: 8px;
  margin-top: 28px;
}

.verify-form > label {
  margin-top: 8px;
  color: var(--lc-ink);
  font-size: 12px;
  font-weight: 600;
}

.verify-form > .el-select {
  width: 100%;
}

.verify-form__entry {
  display: flex;
  gap: 8px;
}

.verify-form__entry .el-input {
  min-width: 0;
}

.verify-form__entry .el-button {
  flex: 0 0 auto;
}

.verify-query-note {
  margin-top: 10px;
  color: var(--lc-muted);
  font-size: 11px;
}

.verify-query-error {
  margin-top: 12px;
}

.verify-artifact {
  position: relative;
  display: flex;
  min-height: 352px;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px;
  overflow: hidden;
  background: #e5eee6;
  border: 1px solid #cbd9cf;
  color: var(--lc-ink);
}

.verify-artifact::before {
  position: absolute;
  top: 46px;
  right: -44px;
  width: 220px;
  height: 220px;
  border: 1px solid rgba(31, 81, 70, 0.18);
  border-radius: 50%;
  content: '';
}

.verify-artifact::after {
  position: absolute;
  top: 64px;
  right: -26px;
  width: 184px;
  height: 184px;
  border: 1px solid rgba(31, 81, 70, 0.14);
  border-radius: 50%;
  content: '';
}

.verify-artifact__top,
.verify-artifact__foot {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #485f53;
  font: 600 10px/1.4 var(--lc-mono);
}

.verify-artifact__seal {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border: 1px solid #6b9480;
  border-radius: 50%;
  color: var(--lc-accent-strong);
  font-size: 36px;
}

.verify-artifact__heading {
  display: grid;
  gap: 4px;
}

.verify-artifact__heading span {
  color: #485f53;
  font: 600 10px/1.4 var(--lc-mono);
}

.verify-artifact__heading strong {
  font-size: 24px;
  font-weight: 600;
}

.verify-artifact__rows {
  display: grid;
  border-top: 1px solid #c6d7cb;
}

.verify-artifact__rows > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #c6d7cb;
  font-size: 12px;
}

.verify-artifact__rows span {
  color: #485f53;
}

.verify-artifact__rows strong {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.verify-artifact__rows i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #437e5c;
}

.verify-assurances {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  padding: 20px 0;
}

.verify-assurances > div {
  display: grid;
  gap: 4px;
}

.verify-assurances strong {
  color: var(--lc-ink);
  font-size: 12px;
}

.verify-assurances span {
  color: var(--lc-muted);
  font-size: 11px;
}

@media (max-width: 760px) {
  .verify-hero {
    grid-template-columns: 1fr;
    gap: 28px;
    padding-top: 8px;
  }

  .verify-copy h1 {
    font-size: 34px;
  }

  .verify-artifact {
    min-height: 300px;
  }

  .verify-assurances {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

@media (max-width: 420px) {
  .verify-form__entry {
    align-items: stretch;
    flex-direction: column;
  }

  .verify-form__entry .el-button {
    width: 100%;
  }

  .verify-copy h1 {
    font-size: 30px;
  }
}
</style>
