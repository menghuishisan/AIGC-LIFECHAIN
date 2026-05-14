package com.lifechain.trade.assembler;

import com.lifechain.common.enums.ViewerRole;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.trade.dto.LicenseDetailVO;
import com.lifechain.trade.dto.OrderDetailVO;

/**
 * 交易模块视图对象可见性装配器
 * <p>
 * 统一处理订单、授权相关 VO 的字段裁剪策略，
 * 将可见性逻辑从 ServiceImpl 中抽离，确保单一机制。
 * </p>
 */
public final class TradeVoAssembler {

    private TradeVoAssembler() {
    }

    /**
     * 对 OrderDetailVO 应用字段可见性策略
     */
    public static void applyVisibility(OrderDetailVO vo) {
        if (vo == null) {
            return;
        }
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR) {
            return;
        }

        if (role == ViewerRole.PUBLIC) {
            vo.getBasicInfo().setPayChannel(null);
            vo.getBasicInfo().setPayAmount(null);
            vo.getStatusInfo().setPayStatus(null);
        }
        // PUBLIC 和 USER 均隐藏链信息
        vo.setChainInfo(null);
    }

    /**
     * 对 LicenseDetailVO 应用字段可见性策略
     */
    public static void applyVisibility(LicenseDetailVO vo) {
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
