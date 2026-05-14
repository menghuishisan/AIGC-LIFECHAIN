package com.lifechain.app.service;

import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.work.entity.CertificateEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.CertificateMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 附件业务绑定权限校验服务。
 * <p>
 * 上传回调阶段只允许把附件绑定到当前用户有权操作的业务对象，
 * 防止客户端伪造业务编号，把附件挂到他人的资源下。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileBindingPermissionService {

    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;
    private final CertificateMapper certificateMapper;

    /**
     * 校验当前用户是否有权绑定指定业务对象。
     *
     * @param bizType 业务类型
     * @param bizNo 业务编号
     * @param operatorId 当前操作用户 ID
     */
    public void validateBindPermission(String bizType, String bizNo, Long operatorId) {
        if (bizType == null || bizNo == null || bizNo.isBlank()) {
            return;
        }
        if (FieldVisibilityUtil.isPrivilegedViewer()) {
            return;
        }

        switch (bizType) {
            case "WORK" -> validateWorkPermission(bizNo, operatorId);
            case "CERTIFICATE" -> validateCertificatePermission(bizNo, operatorId);
            case "ACCOUNT" -> validateAccountPermission(bizNo, operatorId);
            case "GENERAL" -> {
                // 通用附件允许保持未绑定，或绑定到用户自己的通用业务对象。
            }
            default -> throw new BizException(ErrorCodeEnum.PARAM_INVALID, "不支持的附件业务类型: " + bizType);
        }
    }

    private void validateWorkPermission(String workNo, Long operatorId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND, "作品不存在: " + workNo);
        }
        if (!operatorId.equals(work.getCreatorAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权绑定他人作品附件");
        }
    }

    private void validateCertificatePermission(String certNo, Long operatorId) {
        CertificateEntity certificate = certificateMapper.selectByCertNo(certNo);
        if (certificate == null) {
            throw new BizException(ErrorCodeEnum.CERT_NOT_FOUND, "证书不存在: " + certNo);
        }
        if (!operatorId.equals(certificate.getHolderAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权绑定他人证书附件");
        }
    }

    private void validateAccountPermission(String accountNo, Long operatorId) {
        AccountEntity account = accountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在: " + accountNo);
        }
        if (!operatorId.equals(account.getId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权绑定他人账户附件");
        }
    }
}
