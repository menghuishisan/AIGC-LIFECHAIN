package com.lifechain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 主体信息视图对象
 * <p>
 * 展示实名认证后的主体详情，敏感字段（姓名、证件号）
 * 对非管理员角色进行脱敏处理。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectInfoVO implements Serializable {

    /** 主体编号 */
    private String subjectNo;

    /** 主体类型（PERSONAL/ENTERPRISE） */
    private String subjectType;

    /** 真实姓名（非管理员脱敏） */
    private String realName;

    /** 证件类型 */
    private String idCardType;

    /** 证件号码（脱敏） */
    private String idCardNo;
}
