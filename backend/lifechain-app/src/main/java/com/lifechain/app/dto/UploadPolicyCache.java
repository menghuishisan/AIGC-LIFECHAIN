package com.lifechain.app.dto;

import lombok.Data;

/**
 * 上传策略缓存对象。
 * <p>
 * 保存服务端签发的临时上传策略关键数据，回调时必须与缓存命中结果一致，
 * 以防客户端伪造对象名称、业务类型或跨用户重放。
 * </p>
 */
@Data
public class UploadPolicyCache {

    /** 上传令牌。 */
    private String uploadToken;

    /** 对象名称。 */
    private String objectName;

    /** 对象存储路径中的业务前缀。 */
    private String storageBizType;

    /** 上传用户 ID。 */
    private Long uploaderId;
}
