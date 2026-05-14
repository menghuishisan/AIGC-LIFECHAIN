package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>
 * 分段规则：
 * 000000 - 成功
 * 1xxxxx - 鉴权类
 * 2xxxxx - 参数类
 * 3xxxxx - 状态与业务规则类
 * 4xxxxx - 资源与权限边界类
 * 5xxxxx - 外部依赖与异步类
 * 6xxxxx - 支付结算类
 * 7xxxxx - 监管争议类
 * 9xxxxx - 系统异常类
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {

    // ========== 成功 ==========
    SUCCESS("000000", "成功"),

    // ========== 1xxxxx 鉴权类 ==========
    UNAUTHORIZED("100001", "未登录或令牌失效"),
    TOKEN_EXPIRED("100002", "令牌已过期"),
    TOKEN_INVALID("100007", "令牌已失效，请重新登录"),
    FORBIDDEN("100003", "权限不足"),
    REQUEST_DUPLICATED("100004", "重复请求"),
    ACCOUNT_LOCKED("100005", "账户已锁定"),
    RATE_LIMIT_EXCEEDED("100006", "请求频率超限"),

    // ========== 2xxxxx 参数类 ==========
    PARAM_INVALID("200001", "参数不合法"),
    PARAM_MISSING("200002", "缺少必要参数"),
    FILE_TYPE_UNSUPPORTED("200003", "不支持的文件类型"),
    FILE_SIZE_EXCEEDED("200004", "文件大小超限"),

    // ========== 3xxxxx 状态与业务规则类 ==========
    STATUS_INVALID("300001", "当前状态不允许执行该操作"),
    STATUS_TRANSITION_DENIED("300002", "状态流转不合法"),
    ACCOUNT_FROZEN("300003", "账户已冻结"),
    DID_NOT_ACTIVE("300004", "DID未生效"),
    WORK_NOT_CONFIRMED("300005", "作品未完成确权"),
    FEATURE_EXTRACT_FAILED("300006", "特征提取失败"),
    WORK_NOT_LISTED("300007", "作品不可交易"),
    DUPLICATE_WORK_DETECTED("300008", "检测到重复作品"),
    ORDER_ALREADY_PAID("300009", "订单已支付"),
    LICENSE_NOT_ACTIVE("300010", "授权未生效"),
    SETTLE_RULE_MISSING("300011", "缺少分账规则"),
    WORK_FROZEN("300012", "作品已冻结"),
    ORDER_FROZEN("300013", "订单已冻结"),

    // ========== 4xxxxx 资源与权限边界类 ==========
    RESOURCE_NOT_FOUND("400001", "资源不存在"),
    RESOURCE_ACCESS_DENIED("400002", "无权访问该资源"),
    ACCOUNT_NOT_FOUND("400003", "账户不存在"),
    WORK_NOT_FOUND("400004", "作品不存在"),
    ORDER_NOT_FOUND("400005", "订单不存在"),
    CLAIM_NOT_FOUND("400006", "确权申请不存在"),
    CERT_NOT_FOUND("400007", "证书不存在"),
    LICENSE_NOT_FOUND("400008", "授权记录不存在"),

    // ========== 5xxxxx 外部依赖与异步类 ==========
    CHAIN_SUBMIT_FAILED("500001", "Fabric交易提交失败"),
    CHAIN_RECEIPT_TIMEOUT("500002", "链上回执超时"),
    CHAIN_RECEIPT_FAILED("500003", "链上回执失败"),
    STORAGE_UPLOAD_FAILED("500004", "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED("500005", "文件下载失败"),
    CERT_GENERATE_FAILED("500006", "证书生成失败"),
    NOTIFICATION_FAILED("500007", "通知发送失败"),
    STORAGE_DELETE_FAILED("500008", "文件删除失败"),

    // ========== 6xxxxx 支付结算类 ==========
    PAY_CREATE_FAILED("600001", "创建支付单失败"),
    PAY_SIGN_VERIFY_FAILED("600002", "支付签名验证失败"),
    PAY_CONFIRM_FAILED("600003", "支付确认失败"),
    SETTLEMENT_FAILED("600004", "分账失败"),
    SETTLEMENT_PARTIAL("600005", "分账部分成功"),
    REFUND_FAILED("600006", "退款失败"),
    REVERSE_SETTLEMENT_FAILED("600007", "逆分账失败"),
    PAY_AMOUNT_MISMATCH("600008", "支付金额不一致"),
    PAY_CHANNEL_UNSUPPORTED("600009", "不支持的支付渠道"),

    // ========== 7xxxxx 监管争议类 ==========
    RISK_MARK_FAILED("700001", "风险标记失败"),
    FREEZE_APPLY_FAILED("700002", "冻结申请失败"),
    FREEZE_APPROVE_FAILED("700003", "冻结生效失败"),
    UNFREEZE_FAILED("700004", "解冻失败"),
    REGULATOR_DIRECT_FREEZE_DENIED("700005", "不满足直接冻结条件"),
    DISPUTE_SUBMIT_FAILED("700006", "争议提交失败"),
    DISPUTE_ALREADY_CLOSED("700007", "争议已关闭"),
    DISPUTE_PROCESS_FAILED("700008", "争议处理失败"),
    REPORT_GENERATE_FAILED("700009", "监管报告生成失败"),

    // ========== 9xxxxx 系统异常类 ==========
    SYSTEM_ERROR("900001", "系统异常"),
    DATABASE_ERROR("900002", "数据库异常"),
    CACHE_ERROR("900003", "缓存异常"),
    CONCURRENT_CONFLICT("900004", "并发冲突，请重试");

    /** 错误码 */
    private final String code;

    /** 错误描述 */
    private final String description;

    /**
     * 根据错误码查找枚举
     *
     * @param code 错误码
     * @return 对应的枚举值
     */
    public static ErrorCodeEnum fromCode(String code) {
        for (ErrorCodeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知错误码: " + code);
    }
}
