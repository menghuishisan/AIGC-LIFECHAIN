package com.lifechain.app.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifechain.common.util.HashUtil;
import com.lifechain.work.entity.WorkFileEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** 服务端生成受限副本，预览失败时不允许降级返回原文件。 */
@Service
public class PreviewGenerationService {
    private final RestTemplate http;
    private final String featureUrl;

    public PreviewGenerationService(@Value("${feature-service.url}") String featureUrl) {
        this.featureUrl = featureUrl;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(180_000);
        this.http = new RestTemplate(factory);
    }

    public record PreviewArtifact(@JsonProperty("object_name") String objectName,
                                  @JsonProperty("file_type") String fileType,
                                  @JsonProperty("file_size") Long fileSize,
                                  @JsonProperty("duration_seconds") Integer durationSeconds) { }

    public PreviewArtifact generate(WorkFileEntity file, String workType) {
        String extension = switch (workType) {
            case "IMAGE" -> "jpg";
            case "AUDIO" -> "m4a";
            case "VIDEO" -> "mp4";
            default -> throw new IllegalArgumentException("不支持的预览类型");
        };
        // 项目也包含 XML 转换器，显式指定 JSON，避免 Map 被写为 XML 后遭服务端拒绝。
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(Map.of("object_name", file.getFilePath(), "work_type", workType), headers);
        var artifact = http.postForObject(featureUrl + "/preview",
                request, PreviewArtifact.class);
        String expected = "previews/v1/" + HashUtil.sha256(file.getFilePath()) + "." + extension;
        if (artifact == null || !expected.equals(artifact.objectName()) || artifact.fileSize() == null || artifact.fileSize() <= 0) {
            throw new IllegalStateException("预览服务未返回有效的受限副本");
        }
        return artifact;
    }
}
