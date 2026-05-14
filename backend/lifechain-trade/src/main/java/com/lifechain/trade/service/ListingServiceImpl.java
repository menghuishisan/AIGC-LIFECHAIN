package com.lifechain.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.DidStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.ReviewResultEnum;
import com.lifechain.common.enums.WorkStatusEnum;
import com.lifechain.infra.notification.NotificationService;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.trade.dto.CreateListingRequest;
import com.lifechain.trade.dto.ListingDetailVO;
import com.lifechain.trade.dto.ListingReviewRequest;
import com.lifechain.trade.entity.LicenseTemplateEntity;
import com.lifechain.trade.entity.WorkListingEntity;
import com.lifechain.trade.mapper.LicenseTemplateMapper;
import com.lifechain.trade.mapper.WorkListingMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 上架服务实现
 * <p>
 * 实现作品上架、审核、下架及市场列表查询等完整业务逻辑。
 * 上架流程严格遵循状态机规范，所有状态变更均记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    /** 上架待审核状态 */
    private static final String LISTING_PENDING_REVIEW = "PENDING_REVIEW";
    /** 已上架状态 */
    private static final String LISTING_LISTED = "LISTED";
    /** 已下架状态 */
    private static final String LISTING_UNLISTED = "UNLISTED";
    /** 已驳回状态 */
    private static final String LISTING_REJECTED = "REJECTED";

    private final WorkListingMapper workListingMapper;
    private final LicenseTemplateMapper licenseTemplateMapper;
    private final WorkMapper workMapper;
    private final DidRecordMapper didRecordMapper;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final NotificationService notificationService;

    /**
     * {@inheritDoc}
     * <p>
     * 业务规则：
     * <ol>
     *   <li>作品必须处于 OWNERSHIP_CONFIRMED 或 UNLISTED 状态</li>
     *   <li>作品不能处于冻结状态</li>
     *   <li>创作者DID必须已生效</li>
     *   <li>仅创作者本人可上架自己的作品</li>
     *   <li>同一作品不能存在活跃上架记录（PENDING_REVIEW 或 LISTED）</li>
     *   <li>如指定授权模板编码，优先使用模板参数</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ListingDetailVO createListing(Long creatorAccountId, CreateListingRequest request) {
        log.info("创建上架申请, creatorAccountId={}, workNo={}", creatorAccountId, request.getWorkNo());

        // 1. 查询并校验作品
        WorkEntity work = workMapper.selectByWorkNo(request.getWorkNo());
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND, "作品不存在: " + request.getWorkNo());
        }
        if (!Objects.equals(work.getCreatorAccountId(), creatorAccountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权上架他人作品");
        }
        if (!WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode().equals(work.getStatus())
                && !WorkStatusEnum.UNLISTED.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_CONFIRMED, "作品未完成确权或状态不允许上架");
        }
        if (WorkStatusEnum.RISK_FROZEN.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_FROZEN, "作品已被冻结，无法上架");
        }

        // 1.5 校验创作者DID是否生效
        DidRecordEntity did = didRecordMapper.selectByAccountId(creatorAccountId);
        if (did == null || !DidStatusEnum.DID_ACTIVE.getCode().equals(did.getStatus())) {
            throw new BizException(ErrorCodeEnum.DID_NOT_ACTIVE, "创作者DID未生效，无法上架");
        }

        // 2. 检查是否存在活跃上架记录
        List<WorkListingEntity> activeListings = workListingMapper.selectByWorkId(work.getId());
        boolean hasActive = activeListings.stream()
                .anyMatch(l -> LISTING_PENDING_REVIEW.equals(l.getStatus())
                        || LISTING_LISTED.equals(l.getStatus()));
        if (hasActive) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "该作品已存在活跃的上架记录");
        }

        // 3. 解析授权参数（优先模板）
        String licenseType = request.getLicenseType();
        Long priceAmount = request.getPriceAmount();
        String scopeDescription = request.getScopeDescription();
        Integer durationDays = request.getDurationDays();
        Long templateId = null;

        if (request.getLicenseTemplateCode() != null && !request.getLicenseTemplateCode().isBlank()) {
            LicenseTemplateEntity template = licenseTemplateMapper.selectByTemplateCode(request.getLicenseTemplateCode());
            if (template == null) {
                throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "授权模板不存在: " + request.getLicenseTemplateCode());
            }
            if (!"ACTIVE".equals(template.getStatus())) {
                throw new BizException(ErrorCodeEnum.STATUS_INVALID, "授权模板已停用");
            }
            templateId = template.getId();
            licenseType = template.getLicenseType();
            priceAmount = template.getPriceAmount();
            scopeDescription = template.getScopeDescription();
            durationDays = template.getDurationDays();
        }

        if (licenseType == null || licenseType.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_MISSING, "授权类型不能为空");
        }
        if (priceAmount == null || priceAmount <= 0) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "价格必须大于0");
        }

        // 4. 创建上架记录
        WorkListingEntity listing = new WorkListingEntity();
        listing.setListingNo(BizNoUtil.listingNo());
        listing.setWorkId(work.getId());
        listing.setWorkNo(work.getWorkNo());
        listing.setCreatorAccountId(creatorAccountId);
        listing.setLicenseTemplateId(templateId);
        listing.setLicenseType(licenseType);
        listing.setPriceAmount(priceAmount);
        listing.setCurrency("CNY");
        listing.setStatus(LISTING_PENDING_REVIEW);
        listing.setReviewStatus(LISTING_PENDING_REVIEW);
        listing.setScopeDescription(scopeDescription);
        listing.setDurationDays(durationDays);
        workListingMapper.insert(listing);

        // 5. 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.LISTING.getCode(), listing.getId(), listing.getListingNo(),
                null, LISTING_PENDING_REVIEW,
                "创建上架申请", "LISTING_CREATE", creatorAccountId);

        traceEventService.writeTraceEvent("LISTING", listing.getId(), listing.getListingNo(),
                "LISTING_CREATED", "上架申请已创建", creatorAccountId, null, null);

        log.info("上架申请创建成功, listingNo={}", listing.getListingNo());
        return toListingDetailVO(listing, work);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 审核通过后更新上架状态为 LISTED，同时将作品状态更新为 LISTED。
     * 审核驳回后上架状态设为 REJECTED。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewListing(Long reviewerId, ListingReviewRequest request) {
        log.info("审核上架申请, reviewerId={}, listingNo={}, result={}",
                reviewerId, request.getListingNo(), request.getReviewResult());

        WorkListingEntity listing = workListingMapper.selectByListingNo(request.getListingNo());
        if (listing == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "上架记录不存在: " + request.getListingNo());
        }
        if (!LISTING_PENDING_REVIEW.equals(listing.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "上架记录当前状态不允许审核");
        }

        ReviewResultEnum reviewResult = ReviewResultEnum.fromCode(request.getReviewResult());
        String fromStatus = listing.getStatus();

        if (reviewResult == ReviewResultEnum.APPROVED) {
            listing.setStatus(LISTING_LISTED);
            listing.setReviewStatus(ReviewResultEnum.APPROVED.getCode());
            listing.setReviewerId(reviewerId);
            listing.setReviewComment(request.getReviewComment());
            listing.setListTime(DateTimeUtil.nowUtc());
            workListingMapper.updateById(listing);

            // 更新作品状态为已上架
            WorkEntity work = workMapper.selectById(listing.getWorkId());
            if (work != null) {
                work.setStatus(WorkStatusEnum.LISTED.getCode());
                workMapper.updateById(work);
            }

            log.info("上架审核通过, listingNo={}", listing.getListingNo());
        } else {
            listing.setStatus(LISTING_REJECTED);
            listing.setReviewStatus(ReviewResultEnum.REJECTED.getCode());
            listing.setReviewerId(reviewerId);
            listing.setReviewComment(request.getReviewComment());
            workListingMapper.updateById(listing);

            log.info("上架审核驳回, listingNo={}", listing.getListingNo());
        }

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.LISTING.getCode(), listing.getId(), listing.getListingNo(),
                "LISTING_REVIEW", "上架审核: " + reviewResult.getDescription(),
                reviewerId, "ADMIN", null,
                "SUCCESS", reviewResult.getCode());

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.LISTING.getCode(), listing.getId(), listing.getListingNo(),
                fromStatus, listing.getStatus(),
                "上架审核: " + reviewResult.getDescription(),
                reviewResult.getCode(), reviewerId);

        traceEventService.writeTraceEvent("LISTING", listing.getId(), listing.getListingNo(),
                "LISTING_REVIEWED", "上架审核: " + reviewResult.getDescription(), reviewerId, "ADMIN", null);

        // 通知创作者审核结果
        if (ReviewResultEnum.APPROVED.equals(reviewResult)) {
            notificationService.sendNotice(listing.getCreatorAccountId(), "上架审核通过",
                    "您的作品上架申请（" + listing.getListingNo() + "）已审核通过，作品已上架市场。",
                    "LISTING", "LISTING", listing.getListingNo());
        } else {
            String reason = request.getReviewComment() != null ? "驳回原因：" + request.getReviewComment() : "";
            notificationService.sendNotice(listing.getCreatorAccountId(), "上架审核驳回",
                    "您的作品上架申请（" + listing.getListingNo() + "）未通过审核。" + reason,
                    "LISTING", "LISTING", listing.getListingNo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListingDetailVO getListingDetail(String listingNo) {
        WorkListingEntity listing = workListingMapper.selectByListingNo(listingNo);
        if (listing == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "上架记录不存在: " + listingNo);
        }
        WorkEntity work = workMapper.selectById(listing.getWorkId());
        return toListingDetailVO(listing, work);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 支持按授权类型筛选，仅返回状态为 LISTED 的上架记录。
     * </p>
     */
    @Override
    public PageResult<ListingDetailVO> listMarketListings(String licenseType, PageQuery query) {
        Page<WorkListingEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<WorkListingEntity> wrapper = new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getStatus, LISTING_LISTED)
                .eq(licenseType != null && !licenseType.isBlank(),
                        WorkListingEntity::getLicenseType, licenseType)
                .orderByDesc(WorkListingEntity::getListTime);

        IPage<WorkListingEntity> result = workListingMapper.selectPage(page, wrapper);

        List<ListingDetailVO> records = result.getRecords().stream()
                .map(listing -> {
                    WorkEntity work = workMapper.selectById(listing.getWorkId());
                    return toListingDetailVO(listing, work);
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验上架记录状态为 LISTED 且创作者身份匹配后执行下架。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlistWork(Long creatorAccountId, String listingNo) {
        log.info("下架作品, creatorAccountId={}, listingNo={}", creatorAccountId, listingNo);

        WorkListingEntity listing = workListingMapper.selectByListingNo(listingNo);
        if (listing == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "上架记录不存在: " + listingNo);
        }
        if (!Objects.equals(listing.getCreatorAccountId(), creatorAccountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权下架他人作品");
        }
        if (!LISTING_LISTED.equals(listing.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "当前状态不允许下架");
        }

        String fromStatus = listing.getStatus();
        listing.setStatus(LISTING_UNLISTED);
        listing.setUnlistTime(DateTimeUtil.nowUtc());
        workListingMapper.updateById(listing);

        // 更新作品状态为已下架
        WorkEntity work = workMapper.selectById(listing.getWorkId());
        if (work != null) {
            work.setStatus(WorkStatusEnum.UNLISTED.getCode());
            workMapper.updateById(work);
        }

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.LISTING.getCode(), listing.getId(), listing.getListingNo(),
                fromStatus, LISTING_UNLISTED,
                "创作者主动下架", "UNLIST", creatorAccountId);

        traceEventService.writeTraceEvent("LISTING", listing.getId(), listing.getListingNo(),
                "LISTING_UNLISTED", "作品已下架", creatorAccountId, null, null);

        log.info("作品下架成功, listingNo={}", listingNo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ListingDetailVO> listMyListings(Long creatorAccountId, String status, PageQuery query) {
        Page<WorkListingEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<WorkListingEntity> wrapper = new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getCreatorAccountId, creatorAccountId)
                .eq(status != null && !status.isBlank(), WorkListingEntity::getStatus, status)
                .orderByDesc(WorkListingEntity::getCreatedAt);

        IPage<WorkListingEntity> result = workListingMapper.selectPage(page, wrapper);

        List<ListingDetailVO> records = result.getRecords().stream()
                .map(listing -> {
                    WorkEntity work = workMapper.selectById(listing.getWorkId());
                    return toListingDetailVO(listing, work);
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 管理后台上架审核列表，按 reviewStatus 筛选。
     * </p>
     */
    @Override
    public PageResult<ListingDetailVO> listAdminListings(String reviewStatus, PageQuery query) {
        Page<WorkListingEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<WorkListingEntity> wrapper = new LambdaQueryWrapper<WorkListingEntity>()
                .eq(reviewStatus != null && !reviewStatus.isBlank(),
                        WorkListingEntity::getReviewStatus, reviewStatus)
                .orderByDesc(WorkListingEntity::getCreatedAt);

        IPage<WorkListingEntity> result = workListingMapper.selectPage(page, wrapper);

        List<ListingDetailVO> records = result.getRecords().stream()
                .map(listing -> {
                    WorkEntity work = workMapper.selectById(listing.getWorkId());
                    return toListingDetailVO(listing, work);
                })
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 将上架实体转换为详情视图对象
     *
     * @param listing 上架实体
     * @param work    作品实体（可为null）
     * @return 上架详情VO
     */
    private ListingDetailVO toListingDetailVO(WorkListingEntity listing, WorkEntity work) {
        ListingDetailVO vo = new ListingDetailVO();
        vo.setListingNo(listing.getListingNo());
        vo.setWorkNo(listing.getWorkNo());
        vo.setWorkTitle(work != null ? work.getTitle() : null);
        vo.setCoverUrl(work != null ? work.getCoverUrl() : null);
        vo.setLicenseType(listing.getLicenseType());
        vo.setPriceAmount(listing.getPriceAmount());
        vo.setCurrency(listing.getCurrency());
        vo.setScopeDescription(listing.getScopeDescription());
        vo.setDurationDays(listing.getDurationDays());
        vo.setStatus(listing.getStatus());
        vo.setListTime(listing.getListTime());
        return vo;
    }
}
