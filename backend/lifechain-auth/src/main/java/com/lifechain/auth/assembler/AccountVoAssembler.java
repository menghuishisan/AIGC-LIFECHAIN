package com.lifechain.auth.assembler;

import com.lifechain.auth.dto.AccountProfileVO;
import com.lifechain.auth.dto.SubjectInfoVO;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.common.util.MaskUtil;

/**
 * 账户视图对象可见性装配器
 * <p>
 * 统一处理账户相关 VO 的字段脱敏策略，
 * 将可见性逻辑从 ServiceImpl 中抽离，确保单一机制。
 * </p>
 */
public final class AccountVoAssembler {

    private AccountVoAssembler() {
    }

    /**
     * 对 AccountProfileVO 应用字段可见性策略
     * <p>
     * 管理员/监管员可查看全部原始字段，普通用户和公开接口做脱敏处理。
     * </p>
     */
    public static void applyVisibility(AccountProfileVO vo) {
        if (vo == null) {
            return;
        }
        boolean privileged = FieldVisibilityUtil.isPrivilegedViewer();
        if (privileged) {
            return;
        }
        vo.setMobile(MaskUtil.maskMobile(vo.getMobile()));
        vo.setEmail(MaskUtil.maskEmail(vo.getEmail()));
        applyVisibility(vo.getSubjectInfo());
    }

    /**
     * 对 SubjectInfoVO 应用字段可见性策略
     */
    public static void applyVisibility(SubjectInfoVO vo) {
        if (vo == null) {
            return;
        }
        boolean privileged = FieldVisibilityUtil.isPrivilegedViewer();
        if (privileged) {
            return;
        }
        vo.setRealName(MaskUtil.maskName(vo.getRealName()));
        vo.setIdCardNo(MaskUtil.maskIdCard(vo.getIdCardNo()));
    }
}
