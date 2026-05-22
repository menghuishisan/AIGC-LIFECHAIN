package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 作品文件预览URL响应对象
 * <p>
 * 封装预览URL、访问级别、文件元信息等，供前端渲染预览组件使用。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class PreviewUrlVO implements Serializable {

    /** 签名预览URL */
    private String previewUrl;

    /** 访问级别：FULL-完整访问 / LIMITED-有限预览 */
    private String accessLevel;

    /** 文件类型（如 PNG、MP3、MP4） */
    private String fileType;

    /** 文件名称 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 预览时长限制（秒），仅 LIMITED 级别的音视频文件有值 */
    private Integer previewDurationSeconds;
}
