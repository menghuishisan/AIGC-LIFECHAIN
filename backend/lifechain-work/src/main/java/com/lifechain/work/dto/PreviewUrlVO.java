package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PreviewUrlVO implements Serializable {

    private String previewUrl;

    private String accessLevel;

    private String fileType;

    private String fileName;

    private Long fileSize;

    private Integer previewDurationSeconds;
}
