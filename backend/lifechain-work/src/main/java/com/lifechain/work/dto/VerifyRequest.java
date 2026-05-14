package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 验真请求
 * <p>
 * 验真查询时的请求参数，支持按证书编号、作品编号或文件哈希进行查询。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class VerifyRequest implements Serializable {

    /** 查询类型（CERT_NO/WORK_NO/FILE_HASH） */
    @NotBlank(message = "查询类型不能为空")
    private String queryType;

    /** 查询值 */
    @NotBlank(message = "查询值不能为空")
    private String queryValue;
}
