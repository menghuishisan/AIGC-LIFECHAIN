package com.lifechain.infra.attachment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final SysAttachmentMapper attachmentMapper;
    private final BizIdResolver bizIdResolver;

    @Override
    public SysAttachmentEntity saveAttachment(String bizType, Long bizId, String fileName,
                                              String filePath, Long fileSize, String fileType,
                                              String fileHash, String fileUrl, Long uploaderId) {
        SysAttachmentEntity entity = new SysAttachmentEntity();
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setFileName(fileName);
        entity.setFilePath(filePath);
        entity.setFileSize(fileSize);
        entity.setFileType(fileType);
        entity.setFileHash(fileHash);
        entity.setFileUrl(fileUrl);
        entity.setUploaderId(uploaderId);
        entity.setUploadTime(DateTimeUtil.nowUtc());
        attachmentMapper.insert(entity);
        log.info("附件保存成功, bizType={}, bizId={}, fileName={}", bizType, bizId, fileName);
        return entity;
    }

    @Override
    public List<SysAttachmentEntity> listByBiz(String bizType, Long bizId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<SysAttachmentEntity>()
                .eq(SysAttachmentEntity::getBizType, bizType)
                .eq(SysAttachmentEntity::getBizId, bizId)
                .orderByDesc(SysAttachmentEntity::getUploadTime));
    }

    @Override
    public PageResult<AttachmentVO> listAttachments(String bizType, String bizNo, int pageNo, int pageSize) {
        LambdaQueryWrapper<SysAttachmentEntity> wrapper = new LambdaQueryWrapper<SysAttachmentEntity>()
                .eq(bizType != null, SysAttachmentEntity::getBizType, bizType)
                .eq(bizNo != null, SysAttachmentEntity::getBizNo, bizNo)
                .orderByDesc(SysAttachmentEntity::getUploadTime);
        Page<SysAttachmentEntity> page = new Page<>(pageNo, pageSize);
        Page<SysAttachmentEntity> result = attachmentMapper.selectPage(page, wrapper);
        List<AttachmentVO> voList = result.getRecords().stream()
                .map(AttachmentVO::fromEntity).toList();
        return PageResult.of(voList, result.getTotal(), pageNo, pageSize);
    }

    @Override
    public void deleteAttachment(Long attachmentId, Long operatorId) {
        attachmentMapper.deleteById(attachmentId);
        log.info("附件删除(软删), attachmentId={}, operatorId={}", attachmentId, operatorId);
    }

    @Override
    public void bindAttachmentBiz(Long attachmentId, String bizType, String bizNo) {
        SysAttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity != null) {
            entity.setBizType(bizType);
            entity.setBizNo(bizNo);
            // 通过业务编号解析真实业务对象主键
            Long bizId = bizIdResolver.resolve(bizType, bizNo);
            if (bizId != null) {
                entity.setBizId(bizId);
            } else {
                log.warn("无法解析bizId, bizType={}, bizNo={}", bizType, bizNo);
            }
            attachmentMapper.updateById(entity);
            log.info("附件业务绑定完成, attachmentId={}, bizType={}, bizNo={}, bizId={}",
                    attachmentId, bizType, bizNo, bizId);
        }
    }
}
