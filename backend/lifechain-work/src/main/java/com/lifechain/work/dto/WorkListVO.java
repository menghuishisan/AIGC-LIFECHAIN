package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作品列表视图对象
 * <p>
 * 返回作品的摘要信息，用于列表页展示，
 * 包括作品编号、标题、类型、封面、状态和创建时间。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkListVO implements Serializable {

    /** 作品编号 */
    private String workNo;

    /** 作品标题 */
    private String title;

    /** 作品类型 */
    private String workType;

    /** 封面地址 */
    private String coverUrl;

    /** 作品状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
