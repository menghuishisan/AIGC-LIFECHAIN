package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.app.dto.RegulatorSearchResultVO;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.entity.DisputeCaseEntity;
import com.lifechain.regulator.entity.FreezeRecordEntity;
import com.lifechain.regulator.entity.RiskEventEntity;
import com.lifechain.regulator.mapper.DisputeCaseMapper;
import com.lifechain.regulator.mapper.FreezeRecordMapper;
import com.lifechain.regulator.mapper.RiskEventMapper;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.CertificateEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.CertificateMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegulatorSearchService {

    private final RiskEventMapper riskEventMapper;
    private final FreezeRecordMapper freezeRecordMapper;
    private final DisputeCaseMapper disputeCaseMapper;
    private final WorkMapper workMapper;
    private final CertificateMapper certificateMapper;
    private final AccountMapper accountMapper;
    private final TradeOrderMapper tradeOrderMapper;

    public PageResult<RegulatorSearchResultVO> search(String keyword, String targetType, PageQuery query) {
        List<RegulatorSearchResultVO> allResults = new ArrayList<>();

        if (keyword == null || keyword.isBlank()) {
            return PageResult.of(List.of(), 0L, query.getPageNo(), query.getPageSize());
        }

        int limit = query.getPageSize() * 2;

        if (targetType == null || "RISK_EVENT".equals(targetType)) {
            searchRiskEvents(keyword, limit, allResults);
        }
        if (targetType == null || "FREEZE".equals(targetType)) {
            searchFreezeRecords(keyword, limit, allResults);
        }
        if (targetType == null || "DISPUTE".equals(targetType)) {
            searchDisputes(keyword, limit, allResults);
        }
        if (targetType == null || "WORK".equals(targetType)) {
            searchWorks(keyword, limit, allResults);
        }
        if (targetType == null || "CERTIFICATE".equals(targetType)) {
            searchCertificates(keyword, limit, allResults);
        }
        if (targetType == null || "ACCOUNT".equals(targetType)) {
            searchAccounts(keyword, limit, allResults);
        }
        if (targetType == null || "ORDER".equals(targetType)) {
            searchOrders(keyword, limit, allResults);
        }

        allResults.sort(Comparator.comparing(RegulatorSearchResultVO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long total = allResults.size();
        int start = (query.getPageNo() - 1) * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), allResults.size());
        List<RegulatorSearchResultVO> paged = start < allResults.size()
                ? allResults.subList(start, end) : List.of();

        return PageResult.of(paged, total, query.getPageNo(), query.getPageSize());
    }

    private void searchRiskEvents(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<RiskEventEntity> page = riskEventMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<RiskEventEntity>()
                        .like(RiskEventEntity::getRiskNo, keyword)
                        .or().like(RiskEventEntity::getTargetNo, keyword)
                        .or().like(RiskEventEntity::getRiskDescription, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("RISK_EVENT").objectNo(e.getRiskNo())
                .title(e.getRiskDescription()).status(e.getStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchFreezeRecords(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<FreezeRecordEntity> page = freezeRecordMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<FreezeRecordEntity>()
                        .like(FreezeRecordEntity::getFreezeNo, keyword)
                        .or().like(FreezeRecordEntity::getTargetNo, keyword)
                        .or().like(FreezeRecordEntity::getFreezeReason, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("FREEZE").objectNo(e.getFreezeNo())
                .title(e.getFreezeReason()).status(e.getFreezeStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchDisputes(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<DisputeCaseEntity> page = disputeCaseMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<DisputeCaseEntity>()
                        .like(DisputeCaseEntity::getCaseNo, keyword)
                        .or().like(DisputeCaseEntity::getOrderNo, keyword)
                        .or().like(DisputeCaseEntity::getWorkNo, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("DISPUTE").objectNo(e.getCaseNo())
                .title(e.getDisputeType()).status(e.getStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchWorks(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<WorkEntity> page = workMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<WorkEntity>()
                        .like(WorkEntity::getWorkNo, keyword)
                        .or().like(WorkEntity::getTitle, keyword)
                        .or().like(WorkEntity::getDescription, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("WORK").objectNo(e.getWorkNo())
                .title(e.getTitle()).status(e.getStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchCertificates(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<CertificateEntity> page = certificateMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<CertificateEntity>()
                        .like(CertificateEntity::getCertNo, keyword)
                        .or().like(CertificateEntity::getWorkNo, keyword)
                        .or().like(CertificateEntity::getClaimNo, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("CERTIFICATE").objectNo(e.getCertNo())
                .title(e.getWorkNo()).status(e.getStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchAccounts(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<AccountEntity> page = accountMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<AccountEntity>()
                        .like(AccountEntity::getAccountNo, keyword)
                        .or().like(AccountEntity::getMobile, keyword)
                        .or().like(AccountEntity::getNickname, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("ACCOUNT").objectNo(e.getAccountNo())
                .title(e.getNickname()).status(e.getStatus()).createdAt(e.getCreatedAt()).build()));
    }

    private void searchOrders(String keyword, int limit, List<RegulatorSearchResultVO> results) {
        Page<TradeOrderEntity> page = tradeOrderMapper.selectPage(new Page<>(1, limit),
                new LambdaQueryWrapper<TradeOrderEntity>()
                        .like(TradeOrderEntity::getOrderNo, keyword)
                        .or().like(TradeOrderEntity::getWorkNo, keyword));
        page.getRecords().forEach(e -> results.add(RegulatorSearchResultVO.builder()
                .objectType("ORDER").objectNo(e.getOrderNo())
                .title(e.getWorkNo()).status(e.getOrderStatus()).createdAt(e.getCreatedAt()).build()));
    }
}
