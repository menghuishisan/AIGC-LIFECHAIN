package com.lifechain.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件上传回调请求。
 * <p>
 * 客户端完成对象存储直传后，必须携带服务端签发的上传令牌回调确认，
 * 防止伪造对象名称或跨用户重放上传结果。
 * </p>
 */
@Data
public class FileCallbackRequest {

    @NotBlank(message = "objectName不能为空")
    private String objectName;

    @NotBlank(message = "uploadToken不能为空")
    private String uploadToken;

    @Min(value = 0, message = "fileSize不能为负数")
    private Long fileSize;

    private String contentType;

    /** 业务类型。 */
    private String bizType;

    /** 业务编号。 */
    private String bizNo;

    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
