package com.lifechain.work.assembler;

import com.lifechain.common.enums.ViewerRole;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.work.dto.CertDetailVO;
import com.lifechain.work.dto.ClaimDetailVO;
import com.lifechain.work.dto.WorkDetailVO;

/**
 * 作品模块视图对象可见性装配器
 * <p>
 * 统一处理作品、确权、证书相关 VO 的字段裁剪策略，
 * 将可见性逻辑从 ServiceImpl 中抽离，确保单一机制。
 * </p>
 */
public final class WorkVoAssembler {

    private WorkVoAssembler() {
    }

    /**
     * 对 WorkDetailVO 应用字段可见性策略
     *
     * @param vo      作品详情 VO（含完整字段）
     * @param isOwner 当前查看者是否为作品创作者
     */
    public static void applyVisibility(WorkDetailVO vo, boolean isOwner) {
        if (vo == null) {
            return;
        }
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR) {
            return;
        }

        WorkDetailVO.BasicInfo basic = vo.getBasicInfo();
        if (role == ViewerRole.PUBLIC) {
            if (basic.getFiles() != null) {
                basic.getFiles().forEach(f -> f.setFileUrl(null));
            }
            basic.setAigcMeta(null);
            basic.setFeature(null);
            vo.setChainInfo(null);
            vo.setRelationInfo(null);
        } else {
            // USER：非本人作品裁掉敏感细节
            if (!isOwner) {
                basic.setFeature(null);
                basic.setAigcMeta(null);
                if (basic.getFiles() != null) {
                    basic.getFiles().forEach(f -> f.setFileUrl(null));
                }
            }
            vo.setChainInfo(null);
        }
    }

    /**
     * 对 ClaimDetailVO 应用字段可见性策略
     */
    public static void applyVisibility(ClaimDetailVO vo) {
        if (vo == null) {
            return;
        }
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR) {
            return;
        }
        // PUBLIC 和 USER 均不可见内部审核意见和链信息
        if (vo.getStatusInfo() != null) {
            vo.getStatusInfo().setReviewComment(null);
            vo.getStatusInfo().setRejectReason(null);
        }
        vo.setChainInfo(null);
    }

    /**
     * 对 CertDetailVO 应用字段可见性策略
     */
    public static void applyVisibility(CertDetailVO vo) {
        if (vo == null) {
            return;
        }
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR) {
            return;
        }
        vo.setChainInfo(null);
    }
}
