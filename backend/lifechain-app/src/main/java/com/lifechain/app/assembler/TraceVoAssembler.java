package com.lifechain.app.assembler;

import com.lifechain.common.util.FieldVisibilityUtil;

/**
 * 溯源查询视图对象可见性装配器
 * <p>
 * 统一处理溯源事件 extraData 的可见性策略，
 * 将可见性逻辑从 QueryService 中抽离，确保单一机制。
 * </p>
 */
public final class TraceVoAssembler {

    private TraceVoAssembler() {
    }

    /**
     * 判断溯源事件 extraData 是否对当前查看者可见
     *
     * @return true 表示可查看 extraData，false 表示应置空
     */
    public static boolean isExtraDataVisible() {
        return FieldVisibilityUtil.isPrivilegedViewer();
    }
}
