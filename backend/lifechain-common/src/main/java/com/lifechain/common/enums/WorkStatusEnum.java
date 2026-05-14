package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作品状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum WorkStatusEnum {

    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已上传 */
    UPLOADED("UPLOADED", "已上传"),
    /** 特征提取中 */
    FEATURE_PENDING("FEATURE_PENDING", "特征提取中"),
    /** 可确权 */
    READY_FOR_CLAIM("READY_FOR_CLAIM", "可确权"),
    /** 高相似度待人工复核 */
    SIMILARITY_HIGH_RISK("SIMILARITY_HIGH_RISK", "高相似度待人工复核"),
    /** 确权审核中 */
    CLAIM_REVIEWING("CLAIM_REVIEWING", "确权审核中"),
    /** 确权上链中 */
    CLAIM_CHAIN_PENDING("CLAIM_CHAIN_PENDING", "确权上链中"),
    /** 确权成功 */
    OWNERSHIP_CONFIRMED("OWNERSHIP_CONFIRMED", "确权成功"),
    /** 确权失败 */
    CLAIM_FAILED("CLAIM_FAILED", "确权失败"),
    /** 已上架 */
    LISTED("LISTED", "已上架"),
    /** 已下架 */
    UNLISTED("UNLISTED", "已下架"),
    /** 风险冻结 */
    RISK_FROZEN("RISK_FROZEN", "风险冻结"),
    /** 已归档 */
    ARCHIVED("ARCHIVED", "已归档");

    /** 状态编码 */
    private final String code;
    /** 状态描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 状态编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static WorkStatusEnum fromCode(String code) {
        for (WorkStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的作品状态编码: " + code);
    }
}
