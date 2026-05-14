package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权列表视图对象
 * <p>
 * 用于授权记录列表展示的精简信息，包括授权编号、作品信息、授权类型、状态及有效期等。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class LicenseListVO implements Serializable {

    /** 授权编号 */
    private String licenseNo;

    /** 作品编号 */
    private String workNo;

    /** 作品标题 */
    private String workTitle;

    /** 授权类型 */
    private String licenseType;

    /** 授权状态 */
    private String licenseStatus;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 到期时间 */
    private LocalDateTime expireTime;
}
