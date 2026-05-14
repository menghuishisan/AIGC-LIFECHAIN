package com.lifechain.app.service;

import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.infra.attachment.BizIdResolver;
import com.lifechain.regulator.entity.DisputeCaseEntity;
import com.lifechain.regulator.mapper.DisputeCaseMapper;
import com.lifechain.trade.entity.LicenseRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.entity.WorkListingEntity;
import com.lifechain.trade.mapper.LicenseRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.trade.mapper.WorkListingMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.CertificateMapper;
import com.lifechain.work.mapper.ClaimApplicationMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 业务编号→主键解析器实现
 * <p>
 * 放在 lifechain-app 模块，因为需要跨模块访问多个业务 Mapper。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizIdResolverImpl implements BizIdResolver {

    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final CertificateMapper certificateMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final WorkListingMapper workListingMapper;
    private final LicenseRecordMapper licenseRecordMapper;
    private final DisputeCaseMapper disputeCaseMapper;

    @Override
    public Long resolve(String bizType, String bizNo) {
        if (bizType == null || bizNo == null) {
            return null;
        }
        return switch (bizType.toUpperCase()) {
            case "ACCOUNT" -> {
                AccountEntity e = accountMapper.selectByAccountNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "WORK" -> {
                WorkEntity e = workMapper.selectByWorkNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "CLAIM" -> {
                var e = claimApplicationMapper.selectByClaimNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "CERTIFICATE" -> {
                var e = certificateMapper.selectByCertNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "ORDER" -> {
                TradeOrderEntity e = tradeOrderMapper.selectByOrderNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "LISTING" -> {
                WorkListingEntity e = workListingMapper.selectByListingNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "LICENSE" -> {
                LicenseRecordEntity e = licenseRecordMapper.selectByLicenseNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            case "DISPUTE" -> {
                DisputeCaseEntity e = disputeCaseMapper.selectByCaseNo(bizNo);
                yield e != null ? e.getId() : null;
            }
            default -> {
                log.warn("未知的业务类型，无法解析bizId: bizType={}, bizNo={}", bizType, bizNo);
                yield null;
            }
        };
    }
}
