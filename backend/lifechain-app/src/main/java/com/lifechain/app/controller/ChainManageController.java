package com.lifechain.app.controller;

import com.lifechain.chain.model.ChainRetryRequest;
import com.lifechain.chain.model.ChainTxRecordVO;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.chain.record.ChainTxRecordService;
import com.lifechain.chain.service.FabricChainService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "链管理", description = "链回执查询、交易查询与重试")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ChainManageController {

    private final ChainTxRecordService chainTxRecordService;
    private final FabricChainService fabricChainService;

    @GetMapping("/api/admin/chain/receipts/{bizType}/{bizNo}")
    @Operation(summary = "查询链回执", description = "根据业务类型和业务编号查询链交易回执")
    public ApiResponse<List<ChainTxRecordVO>> getReceipts(
            @PathVariable String bizType, @PathVariable String bizNo) {
        List<ChainTxRecordEntity> records = chainTxRecordService.listByBizTypeAndBizNo(bizType, bizNo);
        return ApiResponse.success(records.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping("/api/admin/chain/retry")
    @Operation(summary = "链交易重试", description = "对失败或超时的链交易发起真实链上验证重试")
    @Idempotent
    public ApiResponse<ChainTxRecordVO> retryChainTx(@Valid @RequestBody ChainRetryRequest request) {
        log.info("链交易重试，txHash={}", request.getTxHash());
        ChainTxRecordEntity record = chainTxRecordService.getByTxHash(request.getTxHash());
        if (record == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "链交易记录不存在: " + request.getTxHash());
        }
        fabricChainService.retrySubmit(record.getId());
        // 重新查询最新状态
        record = chainTxRecordService.getByTxHash(request.getTxHash());
        return ApiResponse.success(toVO(record));
    }

    @GetMapping("/api/admin/chain/tx/{txHash}")
    @Operation(summary = "查询链交易详情", description = "根据交易哈希查询链交易记录详情")
    public ApiResponse<ChainTxRecordVO> getTxByHash(@PathVariable String txHash) {
        ChainTxRecordEntity record = chainTxRecordService.getByTxHash(txHash);
        return ApiResponse.success(record != null ? toVO(record) : null);
    }

    private ChainTxRecordVO toVO(ChainTxRecordEntity entity) {
        if (entity == null) return null;
        return ChainTxRecordVO.builder()
                .bizType(entity.getBizType())
                .bizNo(entity.getBizNo())
                .txType(entity.getTxType())
                .channelName(entity.getChannelName())
                .chaincodeName(entity.getChaincodeName())
                .txHash(entity.getTxHash())
                .blockHeight(entity.getBlockHeight())
                .chainStatus(entity.getChainStatus())
                .endorsementSummary(entity.getEndorsementSummary())
                .failReason(entity.getFailReason())
                .submitTime(entity.getSubmitTime())
                .confirmTime(entity.getConfirmTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
