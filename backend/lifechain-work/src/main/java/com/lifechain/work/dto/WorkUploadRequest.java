package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 作品上传请求
 * <p>
 * 上传新作品时的请求参数，包含作品基本信息和AIGC元数据。
 * 文件通过Multipart方式单独上传。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkUploadRequest implements Serializable {

    /** 作品标题 */
    @NotBlank(message = "作品标题不能为空")
    private String title;

    /** 作品类型（TEXT/IMAGE/AUDIO/VIDEO/MODEL） */
    @NotBlank(message = "作品类型不能为空")
    private String workType;

    /** 作品描述 */
    private String description;

    /** 封面地址 */
    private String coverUrl;

    /** AIGC元数据 */
    private AigcMetaDTO aigcMeta;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
