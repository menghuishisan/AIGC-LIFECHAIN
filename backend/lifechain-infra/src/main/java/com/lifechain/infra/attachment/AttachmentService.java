package com.lifechain.infra.attachment;

import com.lifechain.common.model.PageResult;

import java.util.List;

/**
 * 附件服务接口
 */
public interface AttachmentService {

    /**
     * 保存附件记录
     */
    SysAttachmentEntity saveAttachment(String bizType, Long bizId, String fileName,
                                       String filePath, Long fileSize, String fileType,
                                       String fileHash, String fileUrl, Long uploaderId);

    /**
     * 查询业务关联的附件列表
     */
    List<SysAttachmentEntity> listByBiz(String bizType, Long bizId);

    /**
     * 分页查询附件
     */
    PageResult<AttachmentVO> listAttachments(String bizType, String bizNo, int pageNo, int pageSize);

    /**
     * 删除附件（软删除）
     */
    void deleteAttachment(Long attachmentId, Long operatorId);

    /**
     * 绑定附件的业务对象
     *
     * @param attachmentId 附件ID
     * @param bizType      业务类型
     * @param bizNo        业务编号
     */
    void bindAttachmentBiz(Long attachmentId, String bizType, String bizNo);
}
