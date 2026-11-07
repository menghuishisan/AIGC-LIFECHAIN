<template>
  <!-- 验真帮助页 -->
  <div data-page="VerifyHelp" class="lc-page verify-help-page">
    <h1>验真帮助中心</h1>

    <el-collapse v-model="activeNames" accordion>
      <el-collapse-item title="什么是 AIGC 作品版权验真？" name="1">
        <p>
          AIGC 作品版权验真是 LifeChain
          平台提供的公共服务，任何人无需登录即可通过证书编号、作品编号、
          作品哈希或交易哈希等方式，查验 AIGC
          作品的版权登记信息。系统会与区块链上的存证记录进行比对， 确保版权信息真实有效、不可篡改。
        </p>
      </el-collapse-item>

      <el-collapse-item title="支持哪些查询方式？" name="2">
        <ul>
          <li><strong>证书编号</strong>：输入证书上显示的完整编号</li>
          <li><strong>作品编号</strong>：输入作品详情中显示的完整编号</li>
          <li><strong>文件哈希</strong>：输入原始作品文件的 SHA-256 摘要</li>
          <li><strong>交易哈希</strong>：通过区块链上链交易的哈希值进行查询</li>
        </ul>
      </el-collapse-item>

      <el-collapse-item title="验真通过说明什么？" name="3">
        <p>
          验真通过表示：该作品的版权登记信息已成功上链，链上记录与平台数据一致。
          验真结果会展示证书编号、作品编号、创作者 DID、确权时间、链上交易哈希等详细信息。
        </p>
      </el-collapse-item>

      <el-collapse-item title="验真未通过/未找到记录是什么原因？" name="4">
        <p>可能的原因包括：</p>
        <ul>
          <li>输入的编号或哈希有误</li>
          <li>查询方式选择不正确</li>
          <li>作品尚未完成确权上链</li>
          <li>作品尚未生成证书登记</li>
        </ul>
      </el-collapse-item>

      <el-collapse-item title="验真级别有哪些？" name="5">
        <p>根据查询来源不同，验真分为以下级别：</p>
        <ul>
          <li><strong>PUBLIC</strong>：公共验真 — 任何人可访问，基础验证</li>
          <li><strong>LOGIN</strong>：用户验真 — 登录用户发起，可关联身份</li>
          <li><strong>REGULATOR</strong>：监管验真 — 监管机构发起，最高权限</li>
        </ul>
      </el-collapse-item>

      <el-collapse-item title="如何联系我们？" name="6">
        <p>
          如有任何疑问或需要人工协助，请通过平台客服通道联系我们。
          已登录用户可从订单详情发起与该笔交易相关的争议。
        </p>
      </el-collapse-item>
    </el-collapse>

    <div class="help-actions">
      <el-button type="primary" @click="$router.push('/verify')">去验真</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const activeNames = ref('1')
</script>

<style lang="scss" scoped>
.verify-help-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 40px 20px;

  h1 {
    font-size: 28px;
    margin-bottom: 32px;
    color: var(--lc-ink);
    text-align: center;
  }

  p,
  li {
    line-height: 1.8;
    color: var(--lc-muted);
    font-size: 14px;
  }

  ul {
    padding-left: 20px;
    margin: 8px 0;
  }
}

.help-actions {
  text-align: center;
  margin-top: 40px;
}
</style>
