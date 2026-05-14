package com.lifechain.regulator.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.regulator.entity.RegulatorReportEntity;
import com.lifechain.regulator.mapper.RegulatorReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportChainReceiptHandler implements ChainReceiptHandler {

    private final RegulatorReportMapper regulatorReportMapper;
    private final AuditService auditService;

    @Override
    public String getBizType() {
        return BizTypeEnum.REPORT.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        RegulatorReportEntity report = regulatorReportMapper.selectById(record.getBizId());
        if (report == null) {
            log.warn("报告回执处理失败，未找到报告记录，bizId={}", record.getBizId());
            return;
        }
        String fromChainStatus = report.getChainStatus();
        if (ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(fromChainStatus)) {
            return;
        }
        report.setStatus("COMPLETED");
        report.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        report.setTxHash(record.getTxHash());
        report.setBlockHeight(record.getBlockHeight());
        regulatorReportMapper.updateById(report);

        auditService.writeStatusHistory(BizTypeEnum.REPORT.getCode(), report.getId(), report.getReportNo(),
                "GENERATING", "COMPLETED",
                "报告链上回执确认成功", null, null);
        auditService.writeAuditLog(BizTypeEnum.REPORT.getCode(), report.getId(), report.getReportNo(),
                "CHAIN_RECEIPT_SUCCESS", "报告链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("报告回执处理完成，reportNo={} -> COMPLETED", report.getReportNo());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        RegulatorReportEntity report = regulatorReportMapper.selectById(record.getBizId());
        if (report == null) return;
        report.setStatus("FAILED");
        report.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        regulatorReportMapper.updateById(report);

        auditService.writeStatusHistory(BizTypeEnum.REPORT.getCode(), report.getId(), report.getReportNo(),
                "GENERATING", "FAILED",
                "报告链上回执失败: " + record.getFailReason(), null, null);
    }
}
