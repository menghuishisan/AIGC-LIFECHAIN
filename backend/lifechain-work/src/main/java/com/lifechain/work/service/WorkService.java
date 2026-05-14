package com.lifechain.work.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 作品服务接口
 * <p>
 * 提供作品上传、详情查询、元数据更新、特征提取、
 * 我的作品列表和市场作品列表等核心功能。
 * </p>
 *
 * @author LifeChain
 */
public interface WorkService {

    /**
     * 上传作品
     * <p>
     * 创建作品记录，上传文件到对象存储，保存AIGC元数据，
     * 计算文件哈希，生成作品编号，写入状态历史和审计日志。
     * </p>
     *
     * @param accountId 创作者账户ID
     * @param request   上传请求参数
     * @param files     作品文件列表
     * @return 作品详情
     */
    WorkDetailVO uploadWork(Long accountId, WorkUploadRequest request, List<MultipartFile> files);

    /**
     * 查询作品详情
     * <p>
     * 加载作品的所有关联信息（文件、元数据、特征、确权、证书），
     * 并根据状态计算当前允许的操作列表。
     * </p>
     *
     * @param workNo          作品编号
     * @param viewerAccountId 查看者账户ID（用于权限判断，可为null）
     * @return 作品详情
     */
    WorkDetailVO getWorkDetail(String workNo, Long viewerAccountId);

    /**
     * 更新作品元数据
     * <p>
     * 仅在作品处于DRAFT或UPLOADED状态时允许更新标题、描述、封面和AIGC元数据。
     * 更新后重新计算元数据哈希。
     * </p>
     *
     * @param accountId 操作者账户ID
     * @param workNo    作品编号
     * @param request   更新请求参数
     */
    void updateWorkMeta(Long accountId, String workNo, WorkMetaUpdateRequest request);

    /**
     * 触发特征提取
     * <p>
     * 验证作品状态为UPLOADED后设置为FEATURE_PENDING，
     * 调用特征提取服务计算感知哈希，完成后设置为READY_FOR_CLAIM，
     * 同时执行与现有作品的相似度检测。
     * </p>
     *
     * @param accountId 操作者账户ID
     * @param workNo    作品编号
     */
    void triggerFeatureExtract(Long accountId, String workNo);

    /**
     * 查询作品特征
     *
     * @param workNo          作品编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 作品特征信息
     */
    WorkFeatureVO getWorkFeature(String workNo, Long viewerAccountId);

    /**
     * 查询我的作品列表（分页）
     *
     * @param accountId 创作者账户ID
     * @param status    状态筛选（可为null）
     * @param query     分页参数
     * @return 分页结果
     */
    PageResult<WorkListVO> listMyWorks(Long accountId, String status, PageQuery query);

    /**
     * 查询市场作品列表（分页）
     * <p>
     * 仅返回LISTED状态的作品。
     * </p>
     *
     * @param workType 作品类型筛选（可为null）
     * @param keyword  标题关键词搜索（可为null）
     * @param query    分页参数
     * @return 分页结果
     */
    PageResult<WorkListVO> listMarketWorks(String workType, String keyword, PageQuery query);

    /**
     * 查询市场作品详情
     * <p>
     * 仅返回LISTED状态的作品详情，非上架作品返回不存在。
     * </p>
     *
     * @param workNo 作品编号
     * @return 作品详情
     */
    WorkDetailVO getMarketWorkDetail(String workNo);
}
