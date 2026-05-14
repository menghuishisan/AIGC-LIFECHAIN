package com.lifechain.trade.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.CreateLicenseTemplateRequest;
import com.lifechain.trade.dto.LicenseDetailVO;
import com.lifechain.trade.dto.LicenseTemplateVO;

/**
 * 授权服务接口
 * <p>
 * 提供授权记录的查询功能，包括授权详情查询和我的授权列表查询。
 * </p>
 *
 * @author LifeChain
 */
public interface LicenseService {

    /**
     * 查询授权详情
     *
     * @param licenseNo       授权编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 授权详情
     */
    LicenseDetailVO getLicenseDetail(String licenseNo, Long viewerAccountId);

    /**
     * 分页查询我的授权列表
     *
     * @param accountId 当前用户账户ID（被授权方）
     * @param query     分页参数
     * @return 分页授权列表
     */
    PageResult<LicenseDetailVO> listMyLicenses(Long accountId, PageQuery query);

    /**
     * 撤销指定订单关联的授权
     *
     * @param orderNo 订单编号
     * @param reason  撤销原因
     */
    void revokeLicenseByOrderNo(String orderNo, String reason);

    /**
     * 创建授权模板
     */
    LicenseTemplateVO createTemplate(CreateLicenseTemplateRequest request);

    /**
     * 分页查询授权模板列表
     *
     * @param query 分页参数
     * @return 分页模板列表
     */
    PageResult<LicenseTemplateVO> listTemplates(PageQuery query);

    /**
     * 查询授权模板详情
     *
     * @param templateCode 模板编码
     * @return 模板详情
     */
    LicenseTemplateVO getTemplateDetail(String templateCode);
}
