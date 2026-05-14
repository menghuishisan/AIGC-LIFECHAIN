package com.lifechain.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.LicenseStatusEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.trade.assembler.TradeVoAssembler;
import com.lifechain.trade.dto.CreateLicenseTemplateRequest;
import com.lifechain.trade.dto.LicenseDetailVO;
import com.lifechain.trade.dto.LicenseTemplateVO;
import com.lifechain.trade.entity.LicenseRecordEntity;
import com.lifechain.trade.entity.LicenseTemplateEntity;
import com.lifechain.trade.mapper.LicenseRecordMapper;
import com.lifechain.trade.mapper.LicenseTemplateMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 授权服务实现
 * <p>
 * 实现授权记录的查询功能，包括授权详情查询和我的授权列表分页查询。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRecordMapper licenseRecordMapper;
    private final LicenseTemplateMapper licenseTemplateMapper;
    private final TradeOrderMapper orderMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public LicenseDetailVO getLicenseDetail(String licenseNo, Long viewerAccountId) {
        LicenseRecordEntity entity = licenseRecordMapper.selectByLicenseNo(licenseNo);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.LICENSE_NOT_FOUND, "授权记录不存在: " + licenseNo);
        }
        // 归属校验：仅授权方、被授权方或管理员/监管员可查看
        if (!entity.getLicensorAccountId().equals(viewerAccountId)
                && !entity.getLicenseeAccountId().equals(viewerAccountId)
                && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该授权详情");
        }
        return toLicenseDetailVO(entity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 查询当前用户作为被授权方的所有授权记录，按创建时间倒序分页。
     * </p>
     */
    @Override
    public PageResult<LicenseDetailVO> listMyLicenses(Long accountId, PageQuery query) {
        Page<LicenseRecordEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        IPage<LicenseRecordEntity> result = licenseRecordMapper.selectByLicenseeAccountId(page, accountId);

        List<LicenseDetailVO> vos = result.getRecords().stream()
                .map(this::toLicenseDetailVO)
                .toList();
        return PageResult.of(vos, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeLicenseByOrderNo(String orderNo, String reason) {
        log.info("撤销授权，orderNo={}, reason={}", orderNo, reason);
        // 通过 orderNo 查询关联的订单，再通过 orderId 查授权
        com.lifechain.trade.entity.TradeOrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("撤销授权时订单不存在，orderNo={}", orderNo);
            return;
        }
        LicenseRecordEntity license = licenseRecordMapper.selectByOrderId(order.getId());
        if (license == null) {
            log.warn("撤销授权时授权记录不存在，orderNo={}", orderNo);
            return;
        }
        if (LicenseStatusEnum.LICENSE_REVOKED.getCode().equals(license.getLicenseStatus())) {
            log.info("授权已处于撤销状态，orderNo={}, licenseNo={}", orderNo, license.getLicenseNo());
            return;
        }
        license.setLicenseStatus(LicenseStatusEnum.LICENSE_REVOKED.getCode());
        license.setUpdatedAt(DateTimeUtil.nowUtc());
        licenseRecordMapper.updateById(license);
        log.info("授权撤销完成，orderNo={}, licenseNo={}", orderNo, license.getLicenseNo());
    }

    /**
     * 将授权记录实体转换为详情视图对象
     *
     * @param entity 授权记录实体
     * @return 授权详情VO
     */
    private LicenseDetailVO toLicenseDetailVO(LicenseRecordEntity entity) {
        LicenseDetailVO vo = new LicenseDetailVO();

        var basic = new LicenseDetailVO.BasicInfo();
        basic.setLicenseNo(entity.getLicenseNo());
        basic.setLicenseType(entity.getLicenseType());
        basic.setScopeDescription(entity.getScopeDescription());
        vo.setBasicInfo(basic);

        var status = new LicenseDetailVO.StatusInfo();
        status.setLicenseStatus(entity.getLicenseStatus());
        vo.setStatusInfo(status);

        var time = new LicenseDetailVO.TimeInfo();
        time.setEffectiveTime(entity.getEffectiveTime());
        time.setExpireTime(entity.getExpireTime());
        vo.setTimeInfo(time);

        var relation = new LicenseDetailVO.RelationInfo();
        relation.setOrderNo(entity.getOrderNo());
        relation.setWorkNo(entity.getWorkNo());
        vo.setRelationInfo(relation);

        var chain = new LicenseDetailVO.ChainInfo();
        chain.setChainStatus(entity.getChainStatus());
        chain.setTxHash(entity.getTxHash());
        chain.setBlockHeight(entity.getBlockHeight());
        chain.setLicenseHash(entity.getLicenseHash());
        vo.setChainInfo(chain);

        vo.setAllowedActions(List.of());

        // 统一可见性装配
        TradeVoAssembler.applyVisibility(vo);

        return vo;
    }

    @Override
    public LicenseTemplateVO createTemplate(CreateLicenseTemplateRequest request) {
        LicenseTemplateEntity entity = new LicenseTemplateEntity();
        entity.setTemplateName(request.getTemplateName());
        entity.setLicenseType(request.getLicenseType());
        entity.setScopeDescription(request.getScopeDescription());
        entity.setDurationDays(request.getDurationDays());
        entity.setPriceAmount(request.getPriceAmount());
        entity.setCurrency(request.getCurrency());
        entity.setDescription(request.getDescription());
        entity.setTemplateCode(BizNoUtil.generate("LTPL"));
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(DateTimeUtil.nowUtc());
        entity.setUpdatedAt(DateTimeUtil.nowUtc());
        licenseTemplateMapper.insert(entity);
        log.info("创建授权模板，templateCode={}", entity.getTemplateCode());
        return LicenseTemplateVO.builder()
                .templateName(entity.getTemplateName())
                .templateCode(entity.getTemplateCode())
                .licenseType(entity.getLicenseType())
                .scopeDescription(entity.getScopeDescription())
                .durationDays(entity.getDurationDays())
                .priceAmount(entity.getPriceAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<LicenseTemplateVO> listTemplates(PageQuery query) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LicenseTemplateEntity> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageNo(), query.getPageSize());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LicenseTemplateEntity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LicenseTemplateEntity>()
                        .orderByDesc(LicenseTemplateEntity::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LicenseTemplateEntity> result =
                licenseTemplateMapper.selectPage(page, wrapper);

        java.util.List<LicenseTemplateVO> voList = result.getRecords().stream()
                .map(e -> LicenseTemplateVO.builder()
                        .templateName(e.getTemplateName())
                        .templateCode(e.getTemplateCode())
                        .licenseType(e.getLicenseType())
                        .scopeDescription(e.getScopeDescription())
                        .durationDays(e.getDurationDays())
                        .priceAmount(e.getPriceAmount())
                        .currency(e.getCurrency())
                        .status(e.getStatus())
                        .description(e.getDescription())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LicenseTemplateVO getTemplateDetail(String templateCode) {
        LicenseTemplateEntity entity = licenseTemplateMapper.selectByTemplateCode(templateCode);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "授权模板不存在: " + templateCode);
        }
        return LicenseTemplateVO.builder()
                .templateName(entity.getTemplateName())
                .templateCode(entity.getTemplateCode())
                .licenseType(entity.getLicenseType())
                .scopeDescription(entity.getScopeDescription())
                .durationDays(entity.getDurationDays())
                .priceAmount(entity.getPriceAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
