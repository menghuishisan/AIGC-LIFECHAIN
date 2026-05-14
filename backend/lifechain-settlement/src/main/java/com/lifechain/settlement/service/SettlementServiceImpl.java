package com.lifechain.settlement.service;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.chain.adapter.SettlementChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.SettlementStatusEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.settlement.dto.ReverseSettlementVO;
import com.lifechain.settlement.dto.SettlementItemVO;
import com.lifechain.settlement.dto.SettlementRecordVO;
import com.lifechain.settlement.entity.ReverseSettlementRecordEntity;
import com.lifechain.settlement.entity.SettlementItemEntity;
import com.lifechain.settlement.entity.SettlementRecordEntity;
import com.lifechain.settlement.entity.WorkSettleRuleEntity;
import com.lifechain.settlement.mapper.ReverseSettlementRecordMapper;
import com.lifechain.settlement.mapper.SettlementItemMapper;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import com.lifechain.settlement.mapper.WorkSettleRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 结算服务实现
 * <p>
 * 核心分账结算逻辑实现。负责订单结算的完整生命周期管理：
 * <ol>
 *   <li>基于作品结算规则（或默认80/20规则）计算各角色分账金额</li>
 *   <li>创建结算记录和明细，提交链上存证</li>
 *   <li>根据链上回执更新结算状态</li>
 *   <li>支持结算失败重试和逆分账操作</li>
 * </ol>
 * 所有状态变更均记录审计日志和状态变更历史，确保全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRecordMapper settlementRecordMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final WorkSettleRuleMapper workSettleRuleMapper;
    private final ReverseSettlementRecordMapper reverseSettlementRecordMapper;
    private final AccountMapper accountMapper;
    private final SettlementChainAdapter settlementChainAdapter;
    private final AuditService auditService;
    private final TraceEventService traceEventService;

    /** 默认创作者分成比例（80%） */
    private static final BigDecimal DEFAULT_CREATOR_RATIO = new BigDecimal("0.8000");
    /** 默认平台分成比例（20%） */
    private static final BigDecimal DEFAULT_PLATFORM_RATIO = new BigDecimal("0.2000");
    /** 平台系统账户ID（用于平台分成的收款方） */
    private static final Long PLATFORM_ACCOUNT_ID = 0L;

    /**
     * {@inheritDoc}
     * <p>
     * 完整结算流程：
     * <ol>
     *   <li>幂等校验：检查订单是否已结算</li>
     *   <li>查询生效规则，无规则则使用默认80/20比例</li>
     *   <li>计算各角色分账金额</li>
     *   <li>创建结算记录（状态=SETTLE_PROCESSING, 链上状态=CHAIN_PENDING）</li>
     *   <li>创建结算明细</li>
     *   <li>提交链上存证</li>
     *   <li>根据回执更新状态和链上信息</li>
     *   <li>写入状态变更历史和审计日志</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRecordVO settleOrder(String orderNo, Long orderId, Long workId,
                                           String workNo, Long totalAmount, Long creatorAccountId) {
        log.info("开始结算，orderNo={}, workId={}, totalAmount={}", orderNo, workId, totalAmount);

        // 1. 幂等校验：检查订单是否已结算
        SettlementRecordEntity existing = settlementRecordMapper.selectByOrderId(orderId);
        if (existing != null) {
            log.warn("订单已存在结算记录，orderNo={}, settleNo={}, status={}",
                    orderNo, existing.getSettleNo(), existing.getStatus());
            throw new BizException(ErrorCodeEnum.SETTLEMENT_FAILED, "该订单已存在结算记录");
        }

        // 2. 查询作品生效的结算规则
        WorkSettleRuleEntity rule = workSettleRuleMapper.selectEffectiveRule(workId);
        BigDecimal creatorRatio;
        BigDecimal platformRatio;
        if (rule != null) {
            creatorRatio = rule.getCreatorRatio();
            platformRatio = rule.getPlatformRatio();
            log.info("使用作品结算规则，workId={}, creatorRatio={}, platformRatio={}",
                    workId, creatorRatio, platformRatio);
        } else {
            creatorRatio = DEFAULT_CREATOR_RATIO;
            platformRatio = DEFAULT_PLATFORM_RATIO;
            log.info("作品无绑定规则，使用默认80/20比例，workId={}", workId);
        }

        // 3. 计算分账金额
        long creatorAmount = calculateAmount(totalAmount, creatorRatio);
        long platformAmount = totalAmount - creatorAmount;

        LocalDateTime now = DateTimeUtil.nowUtc();
        String settleNo = BizNoUtil.settleNo();

        // 4. 创建结算记录
        SettlementRecordEntity record = new SettlementRecordEntity();
        record.setSettleNo(settleNo);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setWorkId(workId);
        record.setWorkNo(workNo);
        record.setTotalAmount(totalAmount);
        record.setStatus(SettlementStatusEnum.SETTLE_PROCESSING.getCode());
        record.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        record.setSettleTime(now);
        record.setRetryCount(0);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        settlementRecordMapper.insert(record);

        // 5. 创建结算明细
        List<SettlementItemEntity> items = new ArrayList<>();
        items.add(buildSettlementItem(record.getId(), settleNo, creatorAccountId,
                "CREATOR", creatorRatio, creatorAmount, now));
        items.add(buildSettlementItem(record.getId(), settleNo, PLATFORM_ACCOUNT_ID,
                "PLATFORM", platformRatio, platformAmount, now));
        for (SettlementItemEntity item : items) {
            settlementItemMapper.insert(item);
        }

        // 6. 构建明细摘要哈希用于上链
        String summaryHash = buildSummaryHash(settleNo, items);

        // 7. 提交链上存证
        try {
            ChainSubmitResult chainResult = settlementChainAdapter.registerSettlement(
                    record.getId(), settleNo, orderNo, totalAmount, summaryHash, now);

            if (chainResult.isSuccess()) {
                record.setStatus(SettlementStatusEnum.SETTLE_PROCESSING.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                record.setTxHash(chainResult.getTxHash());
                record.setBlockHeight(chainResult.getBlockHeight());
                log.info("结算链上交易已提交待回执，settleNo={}, txHash={}", settleNo, chainResult.getTxHash());
            } else {
                record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                record.setFailReason(chainResult.getFailReason());
                log.warn("结算上链失败，settleNo={}, reason={}", settleNo, chainResult.getFailReason());
            }
        } catch (Exception e) {
            record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
            record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            record.setFailReason(e.getMessage());
            log.error("结算上链异常，settleNo={}", settleNo, e);
        }

        record.setUpdatedAt(DateTimeUtil.nowUtc());
        settlementRecordMapper.updateById(record);

        // 8. 写入状态变更历史和审计日志
        auditService.writeStatusHistory(
                BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                SettlementStatusEnum.SETTLE_READY.getCode(), record.getStatus(),
                "订单结算", null, null);
        String auditResult = SettlementStatusEnum.SETTLE_SUCCESS.getCode().equals(record.getStatus()) ? "SUCCESS"
                : SettlementStatusEnum.SETTLE_FAILED.getCode().equals(record.getStatus()) ? "FAIL" : "PROCESSING";
        auditService.writeAuditLog(
                BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                "SETTLE_ORDER", "订单结算: orderNo=" + orderNo + ", totalAmount=" + totalAmount,
                null, "SYSTEM", null,
                auditResult,
                null);

        log.info("结算流程完成，settleNo={}, status={}", settleNo, record.getStatus());

        traceEventService.writeTraceEvent(BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                "SETTLEMENT_SUBMITTED", "结算已创建并提交链上", null, "SYSTEM", null);

        return buildSettlementRecordVO(record, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettlementRecordVO getSettlementByOrderNo(String orderNo) {
        SettlementRecordEntity record = settlementRecordMapper.selectByOrderNo(orderNo);
        if (record == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        List<SettlementItemEntity> items = settlementItemMapper.selectBySettleId(record.getId());
        return buildSettlementRecordVO(record, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettlementRecordVO getSettlementBySettleNo(String settleNo) {
        SettlementRecordEntity record = settlementRecordMapper.selectBySettleNo(settleNo);
        if (record == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        List<SettlementItemEntity> items = settlementItemMapper.selectBySettleId(record.getId());
        return buildSettlementRecordVO(record, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<SettlementRecordVO> listSettlements(String status, PageQuery query) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SettlementRecordEntity> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageNo(), query.getPageSize());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SettlementRecordEntity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SettlementRecordEntity>()
                        .eq(status != null && !status.isBlank(), SettlementRecordEntity::getStatus, status)
                        .orderByDesc(SettlementRecordEntity::getSettleTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SettlementRecordEntity> result =
                settlementRecordMapper.selectPage(page, wrapper);

        List<SettlementRecordVO> voList = result.getRecords().stream()
                .map(record -> {
                    List<SettlementItemEntity> items = settlementItemMapper.selectBySettleId(record.getId());
                    return buildSettlementRecordVO(record, items);
                })
                .toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 重试规则：
     * <ol>
     *   <li>仅 SETTLE_FAILED 状态允许重试</li>
     *   <li>重新构建摘要哈希并提交链上</li>
     *   <li>累加重试次数</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRecordVO retrySettlement(String settleNo) {
        log.info("重试结算，settleNo={}", settleNo);

        SettlementRecordEntity record = settlementRecordMapper.selectBySettleNo(settleNo);
        if (record == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        if (!SettlementStatusEnum.SETTLE_FAILED.getCode().equals(record.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "仅失败状态的结算可重试", null, record.getStatus());
        }

        List<SettlementItemEntity> items = settlementItemMapper.selectBySettleId(record.getId());
        String summaryHash = buildSummaryHash(settleNo, items);

        String fromStatus = record.getStatus();
        record.setStatus(SettlementStatusEnum.SETTLE_PROCESSING.getCode());
        record.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        record.setRetryCount(record.getRetryCount() + 1);
        record.setFailReason(null);

        try {
            ChainSubmitResult chainResult = settlementChainAdapter.registerSettlement(
                    record.getId(), settleNo, record.getOrderNo(),
                    record.getTotalAmount(), summaryHash, record.getSettleTime());

            if (chainResult.isSuccess()) {
                record.setStatus(SettlementStatusEnum.SETTLE_PROCESSING.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                record.setTxHash(chainResult.getTxHash());
                record.setBlockHeight(chainResult.getBlockHeight());
                log.info("结算重试链上交易已提交待回执，settleNo={}, txHash={}", settleNo, chainResult.getTxHash());
            } else {
                record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                record.setFailReason(chainResult.getFailReason());
                log.warn("结算重试上链失败，settleNo={}, reason={}", settleNo, chainResult.getFailReason());
            }
        } catch (Exception e) {
            record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
            record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            record.setFailReason(e.getMessage());
            log.error("结算重试上链异常，settleNo={}", settleNo, e);
        }

        record.setUpdatedAt(DateTimeUtil.nowUtc());
        settlementRecordMapper.updateById(record);

        auditService.writeStatusHistory(
                BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                fromStatus, record.getStatus(),
                "结算重试(第" + record.getRetryCount() + "次)", null, null);

        traceEventService.writeTraceEvent(BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                "SETTLEMENT_RETRIED", "结算重试第" + record.getRetryCount() + "次", null, "SYSTEM", null);

        return buildSettlementRecordVO(record, items);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 逆分账规则：
     * <ol>
     *   <li>原结算必须为 SETTLE_SUCCESS 状态</li>
     *   <li>同一笔结算不允许重复逆分账</li>
     *   <li>逆分账金额等于原结算总金额</li>
     *   <li>逆分账信息同步上链存证</li>
     *   <li>成功后原结算状态变更为 REVERSE_SUCCESS</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReverseSettlementVO reverseSettlement(String settleNo, String reason) {
        log.info("发起逆分账，settleNo={}, reason={}", settleNo, reason);

        // 1. 查询原结算记录
        SettlementRecordEntity record = settlementRecordMapper.selectBySettleNo(settleNo);
        if (record == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算记录不存在");
        }
        if (!SettlementStatusEnum.SETTLE_SUCCESS.getCode().equals(record.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "仅成功状态的结算可逆分账", null, record.getStatus());
        }

        // 2. 检查是否已有逆分账记录
        ReverseSettlementRecordEntity existingReverse = reverseSettlementRecordMapper.selectBySettleNo(settleNo);
        if (existingReverse != null) {
            throw new BizException(ErrorCodeEnum.REVERSE_SETTLEMENT_FAILED, "该结算已存在逆分账记录");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        String reverseNo = BizNoUtil.reverseNo();

        // 3. 创建逆分账记录
        ReverseSettlementRecordEntity reverseRecord = new ReverseSettlementRecordEntity();
        reverseRecord.setReverseNo(reverseNo);
        reverseRecord.setSettleId(record.getId());
        reverseRecord.setSettleNo(settleNo);
        reverseRecord.setOrderId(record.getOrderId());
        reverseRecord.setOrderNo(record.getOrderNo());
        reverseRecord.setReverseAmount(record.getTotalAmount());
        reverseRecord.setStatus(SettlementStatusEnum.REVERSE_PENDING.getCode());
        reverseRecord.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        reverseRecord.setReason(reason);
        reverseRecord.setApplyTime(now);
        reverseRecord.setCreatedAt(now);
        reverseRecord.setUpdatedAt(now);
        reverseSettlementRecordMapper.insert(reverseRecord);

        // 4. 提交链上存证
        boolean chainSubmitSuccess = false;
        try {
            ChainSubmitResult chainResult = settlementChainAdapter.registerReverseSettlement(
                    reverseRecord.getId(), reverseNo, settleNo,
                    record.getTotalAmount(), reason, now);

            if (chainResult.isSuccess()) {
                reverseRecord.setStatus(SettlementStatusEnum.REVERSE_PENDING.getCode());
                reverseRecord.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                reverseRecord.setTxHash(chainResult.getTxHash());
                reverseRecord.setBlockHeight(chainResult.getBlockHeight());
                chainSubmitSuccess = true;
                log.info("逆分账链上交易已提交待回执，reverseNo={}, txHash={}", reverseNo, chainResult.getTxHash());
            } else {
                reverseRecord.setStatus(SettlementStatusEnum.REVERSE_FAILED.getCode());
                reverseRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                reverseRecord.setFailReason(chainResult.getFailReason());
                log.warn("逆分账上链失败，reverseNo={}, reason={}", reverseNo, chainResult.getFailReason());
            }
        } catch (Exception e) {
            reverseRecord.setStatus(SettlementStatusEnum.REVERSE_FAILED.getCode());
            reverseRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            reverseRecord.setFailReason(e.getMessage());
            log.error("逆分账上链异常，reverseNo={}", reverseNo, e);
        }

        reverseRecord.setUpdatedAt(DateTimeUtil.nowUtc());
        reverseSettlementRecordMapper.updateById(reverseRecord);

        // 仅当链提交成功时，才把原结算推进到 REVERSE_PENDING
        String fromSettleStatus = record.getStatus();
        if (chainSubmitSuccess) {
            record.setStatus(SettlementStatusEnum.REVERSE_PENDING.getCode());
            record.setUpdatedAt(DateTimeUtil.nowUtc());
            settlementRecordMapper.updateById(record);

            auditService.writeStatusHistory(
                    BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                    fromSettleStatus, record.getStatus(),
                    "逆分账链上提交成功，原结算进入逆分账中", null, null);
        }

        // 5. 写入逆分账记录的审计日志
        auditService.writeStatusHistory(
                BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverseRecord.getId(), reverseNo,
                SettlementStatusEnum.REVERSE_PENDING.getCode(), reverseRecord.getStatus(),
                "逆分账: " + reason, null, null);
        auditService.writeAuditLog(
                BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverseRecord.getId(), reverseNo,
                "REVERSE_SETTLEMENT", "逆分账: settleNo=" + settleNo + ", amount=" + record.getTotalAmount() + ", reason=" + reason,
                null, "SYSTEM", null,
                chainSubmitSuccess ? "PROCESSING" : "FAIL",
                null);

        log.info("逆分账流程完成，reverseNo={}, status={}", reverseNo, reverseRecord.getStatus());

        // 轨迹事件仅在链提交成功时写"逆分账已提交"，不写"已完成"
        if (chainSubmitSuccess) {
            traceEventService.writeTraceEvent(BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverseRecord.getId(), reverseNo,
                    "REVERSE_SETTLEMENT_SUBMITTED", "逆分账已提交链上", null, "SYSTEM", null);
        } else {
            traceEventService.writeTraceEvent(BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverseRecord.getId(), reverseNo,
                    "REVERSE_SETTLEMENT_FAILED", "逆分账链上提交失败", null, "SYSTEM", null);
        }

        return buildReverseSettlementVO(reverseRecord);
    }

    /**
     * 计算分账金额
     * <p>
     * 按比例计算金额，向下取整到分，确保不会超出总金额。
     * </p>
     *
     * @param totalAmount 总金额（分）
     * @param ratio       分账比例
     * @return 分账金额（分）
     */
    private long calculateAmount(long totalAmount, BigDecimal ratio) {
        return BigDecimal.valueOf(totalAmount)
                .multiply(ratio)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
    }

    /**
     * 构建结算明细实体
     *
     * @param settleId  结算记录ID
     * @param settleNo  结算编号
     * @param accountId 收款账户ID
     * @param roleType  角色类型
     * @param ratio     分账比例
     * @param amount    分账金额
     * @param now       当前时间
     * @return 结算明细实体
     */
    private SettlementItemEntity buildSettlementItem(Long settleId, String settleNo, Long accountId,
                                                      String roleType, BigDecimal ratio, long amount,
                                                      LocalDateTime now) {
        SettlementItemEntity item = new SettlementItemEntity();
        item.setSettleId(settleId);
        item.setSettleNo(settleNo);
        item.setAccountId(accountId);
        item.setRoleType(roleType);
        item.setRatio(ratio);
        item.setAmount(amount);
        item.setStatus("PENDING");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    /**
     * 构建结算摘要哈希
     * <p>
     * 将结算编号和各明细信息拼接后计算SHA-256哈希，用于链上存证校验。
     * </p>
     *
     * @param settleNo 结算编号
     * @param items    结算明细列表
     * @return SHA-256哈希值
     */
    private String buildSummaryHash(String settleNo, List<SettlementItemEntity> items) {
        StringBuilder sb = new StringBuilder(settleNo);
        for (SettlementItemEntity item : items) {
            sb.append("|").append(item.getRoleType())
                    .append(":").append(item.getAccountId())
                    .append(":").append(item.getRatio())
                    .append(":").append(item.getAmount());
        }
        return HashUtil.sha256(sb.toString());
    }

    /**
     * 构建结算记录视图对象
     *
     * @param record 结算记录实体
     * @param items  结算明细列表
     * @return 结算记录视图对象
     */
    private SettlementRecordVO buildSettlementRecordVO(SettlementRecordEntity record,
                                                        List<SettlementItemEntity> items) {
        SettlementRecordVO vo = new SettlementRecordVO();
        vo.setSettleNo(record.getSettleNo());
        vo.setOrderNo(record.getOrderNo());
        vo.setWorkNo(record.getWorkNo());
        vo.setTotalAmount(record.getTotalAmount());
        vo.setSettleStatus(record.getStatus());
        vo.setChainStatus(record.getChainStatus());
        vo.setTxHash(record.getTxHash());
        vo.setBlockHeight(record.getBlockHeight());
        vo.setSettleTime(record.getSettleTime());
        vo.setCompleteTime(record.getCompleteTime());
        vo.setItems(items.stream().map(this::buildSettlementItemVO).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 构建结算明细视图对象
     *
     * @param item 结算明细实体
     * @return 结算明细视图对象
     */
    private SettlementItemVO buildSettlementItemVO(SettlementItemEntity item) {
        SettlementItemVO vo = new SettlementItemVO();
        vo.setRoleType(item.getRoleType());
        AccountEntity account = accountMapper.selectById(item.getAccountId());
        vo.setAccountNo(account != null ? account.getAccountNo() : null);
        vo.setRatio(item.getRatio());
        vo.setAmount(item.getAmount());
        vo.setItemStatus(item.getStatus());
        return vo;
    }

    /**
     * 构建逆分账记录视图对象
     *
     * @param record 逆分账记录实体
     * @return 逆分账视图对象
     */
    private ReverseSettlementVO buildReverseSettlementVO(ReverseSettlementRecordEntity record) {
        ReverseSettlementVO vo = new ReverseSettlementVO();
        vo.setReverseNo(record.getReverseNo());
        vo.setSettleNo(record.getSettleNo());
        vo.setOrderNo(record.getOrderNo());
        vo.setReverseAmount(record.getReverseAmount());
        vo.setReverseStatus(record.getStatus());
        vo.setChainStatus(record.getChainStatus());
        vo.setTxHash(record.getTxHash());
        vo.setBlockHeight(record.getBlockHeight());
        vo.setApplyTime(record.getApplyTime());
        vo.setCompleteTime(record.getCompleteTime());
        return vo;
    }
}
