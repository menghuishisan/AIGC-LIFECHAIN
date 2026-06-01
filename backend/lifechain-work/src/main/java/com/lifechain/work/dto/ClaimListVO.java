package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 确权列表视图对象
 * <p>
 * 用于"我的确权"和"管理员确权审核"列表展示的精简信息。
 * </p>
 */
@Data
public class ClaimListVO implements Serializable {

    /** 确权编号 */
    private String claimNo;

    /** 关联作品编号 */
    private String workNo;

    /** 作品标题 */
    private String workTitle;

    /** 确权状态 */
    private String status;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
