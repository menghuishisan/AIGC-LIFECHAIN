package com.lifechain.app.dto;

import lombok.Data;

/**
 * 上传策略响应。
 * <p>
 * 返回前端完成对象存储直传所需的预签名上传地址，以及回调确认所需的上传令牌。
 * </p>
 */
@Data
public class UploadPolicyResponse {

    /** 预签名上传地址。 */
    private String uploadUrl;

    /** 对象名称。 */
    private String objectName;

    /** 临时访问地址。 */
    private String accessUrl;

    /** 上传回调校验令牌。 */
    private String uploadToken;
}
