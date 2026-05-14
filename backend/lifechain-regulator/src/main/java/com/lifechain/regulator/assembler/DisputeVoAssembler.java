package com.lifechain.regulator.assembler;

import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ViewerRole;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.regulator.dto.DisputeCaseVO;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 监管模块视图对象可见性装配器
 * <p>
 * 统一处理争议案件相关 VO 的字段裁剪和证据过滤策略，
 * 将可见性逻辑从 ServiceImpl 中抽离，确保单一机制。
 * </p>
 */
public final class DisputeVoAssembler {

    private DisputeVoAssembler() {
    }

    /**
     * 对 DisputeCaseVO 应用字段可见性策略
     *
     * @param vo                争议案件 VO（含完整字段）
     * @param disputeRoleResolver 根据 accountId 解析其在本争议中的角色（APPLICANT/RESPONDENT/OPERATOR）
     */
    public static void applyVisibility(DisputeCaseVO vo, Function<Long, String> disputeRoleResolver) {
        if (vo == null) {
            return;
        }
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR) {
            return;
        }

        DisputeCaseVO.BasicInfo basic = vo.getBasicInfo();
        DisputeCaseVO.RelationInfo relation = vo.getRelationInfo();

        if (role == ViewerRole.PUBLIC) {
            basic.setEvidences(null);
            basic.setProcessRecords(null);
            relation.setApplicantAccountNo(null);
            relation.setRespondentAccountNo(null);
            vo.setChainInfo(null);
        } else {
            // USER：仅可见本人角色相关证据
            Long currentUserId = UserContext.getUserId();
            if (basic.getEvidences() != null && currentUserId != null) {
                String currentRole = disputeRoleResolver.apply(currentUserId);
                basic.setEvidences(basic.getEvidences().stream()
                        .filter(e -> currentRole.equals(e.getSubmitterRole()))
                        .collect(Collectors.toList()));
            }
            relation.setApplicantAccountNo(null);
            relation.setRespondentAccountNo(null);
            vo.setChainInfo(null);
        }
    }
}
