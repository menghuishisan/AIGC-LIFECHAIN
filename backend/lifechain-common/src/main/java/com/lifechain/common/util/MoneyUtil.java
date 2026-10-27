package com.lifechain.common.util;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 人民币金额唯一契约：业务、数据库和链码均用整数分，渠道边界才转换元。 */
public final class MoneyUtil {
    public static final long MAX_AMOUNT_FEN = 10_000_000_000L;
    private MoneyUtil() {}

    public static long requireFen(Long fen) {
        if (fen == null || fen < 1 || fen > MAX_AMOUNT_FEN) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "金额必须为1至10000000000整数分");
        }
        return fen;
    }

    public static void requireCny(String currency) {
        if (!"CNY".equals(currency)) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "当前交易仅支持人民币CNY");
        }
    }

    public static String fenToYuan(Long fen) {
        return BigDecimal.valueOf(requireFen(fen), 2).toPlainString();
    }

    public static long yuanToFen(String yuan) {
        if (yuan == null || !yuan.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "渠道金额必须为至多两位小数的人民币元");
        }
        try {
            return requireFen(new BigDecimal(yuan).movePointRight(2).longValueExact());
        } catch (ArithmeticException e) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "渠道金额超出范围");
        }
    }

    public static void requireRatios(BigDecimal creator, BigDecimal platform) {
        if (creator == null || platform == null || creator.signum() < 0 || platform.signum() < 0
                || creator.compareTo(BigDecimal.ONE) > 0 || platform.compareTo(BigDecimal.ONE) > 0
                || creator.stripTrailingZeros().scale() > 4 || platform.stripTrailingZeros().scale() > 4
                || creator.add(platform).compareTo(BigDecimal.ONE) != 0) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "创作者与平台分账比例须在0至1之间、至多四位小数且合计为1");
        }
    }

    /** 创作者向下取整到分，余分全部归平台，保证两项之和严格等于实付金额。 */
    public static long creatorShare(Long total, BigDecimal creator, BigDecimal platform) {
        requireRatios(creator, platform);
        return BigDecimal.valueOf(requireFen(total)).multiply(creator).setScale(0, RoundingMode.FLOOR).longValueExact();
    }
}
