package com.lifechain.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务编号生成工具
 * <p>
 * 统一生成各类业务编号，格式为：前缀 + 日期 + 雪花ID后N位，保证唯一性和可读性。
 * </p>
 */
public final class BizNoUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BizNoUtil() {
    }

    /**
     * 生成业务编号
     *
     * @param prefix 业务前缀，如 ACC、DID、WORK、CLM、CERT 等
     * @return 业务编号
     */
    public static String generate(String prefix) {
        String date = LocalDateTime.now(DateTimeUtil.UTC_ZONE).format(DATE_FORMATTER);
        // 使用雪花ID后12位确保唯一
        String snowflakePart = String.valueOf(SnowflakeIdUtil.nextId());
        String suffix = snowflakePart.substring(Math.max(0, snowflakePart.length() - 12));
        return prefix + date + suffix;
    }

    /**
     * 生成账户编号
     */
    public static String accountNo() {
        return generate("ACC");
    }

    /**
     * 生成主体编号
     */
    public static String subjectNo() {
        return generate("SUB");
    }

    /**
     * 生成DID编号
     */
    public static String didNo() {
        return generate("DID");
    }

    /**
     * 生成作品编号
     */
    public static String workNo() {
        return generate("WORK");
    }

    /**
     * 生成确权申请编号
     */
    public static String claimNo() {
        return generate("CLM");
    }

    /**
     * 生成证书编号
     */
    public static String certNo() {
        return generate("CERT");
    }

    /**
     * 生成上架编号
     */
    public static String listingNo() {
        return generate("LST");
    }

    /**
     * 生成订单编号
     */
    public static String orderNo() {
        return generate("ORD");
    }

    /**
     * 生成支付编号
     */
    public static String paymentNo() {
        return generate("PAY");
    }

    /**
     * 生成授权编号
     */
    public static String licenseNo() {
        return generate("LIC");
    }

    /**
     * 生成结算编号
     */
    public static String settleNo() {
        return generate("STL");
    }

    /**
     * 生成逆分账编号
     */
    public static String reverseNo() {
        return generate("REV");
    }

    /**
     * 生成风险事件编号
     */
    public static String riskNo() {
        return generate("RISK");
    }

    /**
     * 生成冻结编号
     */
    public static String freezeNo() {
        return generate("FRZ");
    }

    /**
     * 生成争议工单编号
     */
    public static String caseNo() {
        return generate("DSP");
    }

    /**
     * 生成监管报告编号
     */
    public static String reportNo() {
        return generate("RPT");
    }

    /**
     * 生成通知编号
     */
    public static String noticeNo() {
        return generate("NTC");
    }

    /**
     * 生成退款编号
     */
    public static String refundNo() {
        return generate("RFD");
    }
}
