package com.lifechain.trade.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.CreateListingRequest;
import com.lifechain.trade.dto.ListingDetailVO;
import com.lifechain.trade.dto.ListingReviewRequest;

/**
 * 上架服务接口
 * <p>
 * 提供作品上架、审核、下架及市场列表查询等功能。
 * 上架流程：创建上架（待审核）→ 管理员审核 → 通过后上架到市场。
 * </p>
 *
 * @author LifeChain
 */
public interface ListingService {

    /**
     * 创建上架申请
     * <p>
     * 校验作品已确权且未冻结，校验创作者身份，校验无活跃上架记录，
     * 创建状态为 PENDING_REVIEW 的上架记录并写入状态变更历史。
     * </p>
     *
     * @param creatorAccountId 创作者账户ID
     * @param request          上架请求参数
     * @return 上架详情
     */
    ListingDetailVO createListing(Long creatorAccountId, CreateListingRequest request);

    /**
     * 审核上架申请
     * <p>
     * 管理员审核上架申请，通过则设为 LISTED 并更新上架时间和作品状态，
     * 驳回则设为 REJECTED。写入审计日志和状态变更历史。
     * </p>
     *
     * @param reviewerId 审核人ID
     * @param request    审核请求参数
     */
    void reviewListing(Long reviewerId, ListingReviewRequest request);

    /**
     * 查询上架详情
     *
     * @param listingNo 上架编号
     * @return 上架详情
     */
    ListingDetailVO getListingDetail(String listingNo);

    /**
     * 分页查询市场上架列表
     *
     * @param licenseType 授权类型（可选）
     * @param query       分页参数
     * @return 分页上架列表
     */
    PageResult<ListingDetailVO> listMarketListings(String licenseType, PageQuery query);

    /**
     * 分页查询我的上架列表（创作者）
     *
     * @param creatorAccountId 创作者账户ID
     * @param status           上架状态（可选）
     * @param query            分页参数
     * @return 分页上架列表
     */
    PageResult<ListingDetailVO> listMyListings(Long creatorAccountId, String status, PageQuery query);

    /**
     * 分页查询上架审核列表（管理员）
     *
     * @param reviewStatus 审核状态（可选）
     * @param query        分页参数
     * @return 分页上架列表
     */
    PageResult<ListingDetailVO> listAdminListings(String reviewStatus, PageQuery query);

    /**
     * 下架作品
     * <p>
     * 创作者主动下架已上架的作品，更新上架状态为 UNLISTED 并同步更新作品状态。
     * </p>
     *
     * @param creatorAccountId 创作者账户ID
     * @param listingNo        上架编号
     */
    void unlistWork(Long creatorAccountId, String listingNo);
}
