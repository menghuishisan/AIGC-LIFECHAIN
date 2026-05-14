<template>
  <!-- 状态标签组件：根据状态值自动映射中文文案和颜色 -->
  <span :class="['status-tag', `status-tag--${colorType}`]">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  AccountStatusMap, DidStatusMap, WorkStatusMap, ClaimStatusMap,
  CertStatusMap, OrderStatusMap, LicenseStatusMap, SettlementStatusMap,
  DisputeStatusMap, FreezeStatusMap, RiskStatusMap, ChainStatusMap,
  PayStatusMap, ListingStatusMap
} from '@/shared/constants'

const props = defineProps<{
  /** 状态值 */
  status: string
  /** 状态类型，用于选择对应的映射表 */
  type?: 'account' | 'did' | 'work' | 'claim' | 'cert' | 'order' | 'license' | 'settlement' | 'dispute' | 'freeze' | 'risk' | 'chain' | 'pay' | 'auth' | 'listing'
}>()

/** 状态映射表集合 */
const statusMaps: Record<string, Record<string, string>> = {
  account: AccountStatusMap,
  auth: AccountStatusMap,
  did: DidStatusMap,
  work: WorkStatusMap,
  claim: ClaimStatusMap,
  cert: CertStatusMap,
  order: OrderStatusMap,
  license: LicenseStatusMap,
  settlement: SettlementStatusMap,
  dispute: DisputeStatusMap,
  freeze: FreezeStatusMap,
  risk: RiskStatusMap,
  chain: ChainStatusMap,
  pay: PayStatusMap,
  listing: ListingStatusMap
}

/** 状态中文文案 */
const label = computed(() => {
  if (props.type && statusMaps[props.type]) {
    return statusMaps[props.type][props.status] || props.status
  }
  /* 如果没有指定类型，遍历所有映射表查找 */
  for (const map of Object.values(statusMaps)) {
    if (map[props.status]) return map[props.status]
  }
  return props.status
})

/** 颜色类型映射 */
const colorType = computed(() => {
  const s = props.status
  /* 成功类状态 */
  if (['AUTH_APPROVED', 'DID_ACTIVE', 'OWNERSHIP_CONFIRMED', 'CLAIM_SUCCESS',
    'CERT_ACTIVE', 'PAY_CONFIRMED', 'AUTH_GRANTED', 'ORDER_COMPLETED',
    'LICENSE_ACTIVE', 'SETTLE_SUCCESS', 'REVERSE_SUCCESS', 'DISPUTE_RESOLVED',
    'FREEZE_APPROVED', 'UNFREEZE_APPROVED', 'RISK_RELEASED', 'CHAIN_SUCCESS',
    'PAY_SUCCESS', 'LISTED'].includes(s)) {
    return 'success'
  }
  /* 警告类状态 */
  if (['AUTH_PENDING', 'DID_PENDING', 'DID_APPROVED_PENDING_CHAIN', 'FEATURE_PENDING',
    'CLAIM_REVIEWING', 'CLAIM_CHAIN_PENDING', 'CERT_GENERATING', 'CERT_REGENERATING',
    'PAY_PENDING_CONFIRM', 'AUTH_GRANTING', 'SETTLEMENT_PENDING', 'REFUND_PENDING',
    'LICENSE_PENDING', 'SETTLE_READY', 'SETTLE_PROCESSING', 'REVERSE_PENDING',
    'DISPUTE_SUBMITTED', 'DISPUTE_ACCEPTED', 'DISPUTE_EVIDENCE_PENDING', 'DISPUTE_REVIEWING',
    'FREEZE_APPLIED', 'UNFREEZE_APPLIED', 'RISK_MARKED', 'RISK_REVIEWING',
    'CHAIN_PENDING', 'CHAIN_SUBMITTED', 'PAY_PENDING', 'CLAIM_SUBMITTED',
    'CLAIM_APPROVED_PENDING_CHAIN', 'PENDING_REVIEW'].includes(s)) {
    return 'pending'
  }
  /* 危险类状态 */
  if (['AUTH_REJECTED', 'DID_CHAIN_FAILED', 'CLAIM_REJECTED', 'CLAIM_FAILED',
    'CLAIM_CHAIN_FAILED', 'CERT_INVALID', 'CERT_REVOKED', 'ORDER_EXCEPTION',
    'SETTLE_FAILED', 'REVERSE_FAILED', 'DISPUTE_REJECTED', 'FREEZE_REJECTED',
    'RISK_CONFIRMED', 'CHAIN_FAILED', 'PAY_FAILED', 'SIMILARITY_HIGH_RISK',
    'SETTLE_PARTIAL_SUCCESS', 'REJECTED'].includes(s)) {
    return 'danger'
  }
  /* 冻结类状态 */
  if (['ACCOUNT_FROZEN', 'ACCOUNT_DISABLED', 'DID_SUSPENDED', 'DID_REVOKED',
    'RISK_FROZEN', 'ORDER_FROZEN', 'LICENSE_FROZEN', 'LICENSE_REVOKED',
    'LICENSE_TERMINATED', 'SETTLE_FROZEN'].includes(s)) {
    return 'frozen'
  }
  /* 链上信息类 */
  if (['CHAIN_SUCCESS', 'CHAIN_SUBMITTED'].includes(s)) {
    return 'chain'
  }
  /* 警告类 */
  if (['ORDER_EXPIRED', 'LICENSE_EXPIRED', 'PAY_CLOSED', 'REFUNDED',
    'ORDER_CANCELLED', 'UNLISTED'].includes(s)) {
    return 'warning'
  }
  /* 默认草稿 */
  return 'draft'
})
</script>
