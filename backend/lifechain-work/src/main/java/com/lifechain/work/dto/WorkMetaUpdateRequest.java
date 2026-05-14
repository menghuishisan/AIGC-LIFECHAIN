package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 作品元数据更新请求
 * <p>
 * 更新已上传作品的标题、描述、封面及AIGC元数据信息。
 * 仅在作品处于DRAFT或UPLOADED状态时允许更新。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkMetaUpdateRequest implements Serializable {

    /** 作品标题 */
    private String title;

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
