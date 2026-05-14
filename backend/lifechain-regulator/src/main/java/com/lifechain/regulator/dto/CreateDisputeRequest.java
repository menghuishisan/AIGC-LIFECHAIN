package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建争议案件请求。
 * <p>
 * 包含争议关联的订单编号、作品编号、被申请人、争议类型与说明等信息，
 * 用于申请人发起争议案件。
 * </p>
 */
@Data
public class CreateDisputeRequest implements Serializable {

    /** 关联订单编号。 */
    private String orderNo;

    /** 关联作品编号。 */
    private String workNo;

    /** 被申请人账户编号。 */
    @NotBlank(message = "被申请人不能为空")
    private String respondentAccountNo;

    /** 争议类型。 */
    @NotBlank(message = "争议类型不能为空")
    private String disputeType;

    /** 争议描述。 */
    @NotBlank(message = "争议描述不能为空")
    @Size(max = 2000, message = "争议描述最长2000个字符")
    private String description;

    /** 初始证据文件 URL 列表，单次建案最多 20 个。 */
    @Size(max = 20, message = "初始证据数量不能超过20个")
    private List<String> evidenceUrls;

    /** 幂等请求 ID。 */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
