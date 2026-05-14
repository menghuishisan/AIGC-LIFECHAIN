package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEvidenceRequest {
    @NotBlank(message = "证据类型不能为空")
    private String evidenceType;
    @NotBlank(message = "文件URL不能为空")
    private String fileUrl;
    private String description;
    @NotBlank(message = "文件内容哈希不能为空")
    private String fileHash;
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
