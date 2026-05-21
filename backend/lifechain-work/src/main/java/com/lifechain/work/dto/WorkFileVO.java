package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 作品文件视图对象
 * <p>
 * 返回作品关联文件的摘要信息，包括文件名、访问地址、文件类型、大小和用途。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkFileVO implements Serializable {

    /** 文件ID */
    private Long fileId;

    /** 文件名 */
    private String fileName;

    /** 访问地址 */
    private String fileUrl;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 用途（ORIGINAL/THUMBNAIL/PREVIEW） */
    private String purpose;
}
