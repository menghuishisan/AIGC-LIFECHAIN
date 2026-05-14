package com.lifechain.work.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.CertDetailVO;
import com.lifechain.work.dto.VerifyQueryLogVO;
import com.lifechain.work.dto.VerifyRequest;
import com.lifechain.work.dto.VerifyResultVO;

/**
 * 证书服务接口
 * <p>
 * 提供证书生成、详情查询、下载以及验真查询等核心功能。
 * 验真查询分为三个级别：公开（PUBLIC）、登录用户（LOGIN）、监管方（REGULATOR），
 * 返回的信息详细程度依次递增。
 * </p>
 *
 * @author LifeChain
 */
public interface CertificateService {

    /**
     * 生成证书
     * <p>
     * 验证确权已成功、作品为OWNERSHIP_CONFIRMED状态，创建证书记录，
     * 生成证书内容（JSON），计算证书哈希，上传证书文件到对象存储。
     * 支持版本递增（已存在有效证书时创建新版本）。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param claimNo    确权编号
     * @return 证书详情
     */
    CertDetailVO generateCertificate(Long operatorId, String claimNo);

    /**
     * 查询证书详情
     *
     * @param certNo          证书编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 证书详情
     */
    CertDetailVO getCertificateDetail(String certNo, Long viewerAccountId);

    /**
     * 下载证书文件
     *
     * @param certNo          证书编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 证书文件字节数组
     */
    byte[] downloadCertificate(String certNo, Long viewerAccountId);

    /**
     * 公开验真查询
     * <p>
     * 无需登录，返回有限的摘要信息。
     * 记录查询日志。
     * </p>
     *
     * @param request 验真请求
     * @param queryIp 查询方IP
     * @return 验真结果
     */
    VerifyResultVO verifyPublic(VerifyRequest request, String queryIp);

    /**
     * 登录用户验真查询
     * <p>
     * 需登录，较公开查询返回更多详情（含创作者DID）。
     * 记录查询日志。
     * </p>
     *
     * @param request   验真请求
     * @param accountId 查询人账户ID
     * @param queryIp   查询方IP
     * @return 验真结果
     */
    VerifyResultVO verifyLogin(VerifyRequest request, Long accountId, String queryIp);

    /**
     * 监管方验真查询
     * <p>
     * 需监管权限，返回完整信息（含链上详情）。
     * 记录查询日志。
     * </p>
     *
     * @param request   验真请求
     * @param accountId 查询人账户ID
     * @param queryIp   查询方IP
     * @return 验真结果
     */
    VerifyResultVO verifyRegulator(VerifyRequest request, Long accountId, String queryIp);

    /**
     * 分页查询验真日志
     */
    PageResult<VerifyQueryLogVO> listVerifyLogs(PageQuery query);
}
