package com.lifechain.work.service;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 内容特征提取服务实现
 * <p>
 * 通过 HTTP 调用 Python 特征提取服务（FastAPI），按作品类型获取 256-bit 二进制感知指纹。
 * IMAGE/VIDEO 走 PDQ，AUDIO/TEXT 走 MinHash，MODEL 走 D2 形状描述符。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureExtractServiceImpl implements FeatureExtractService {

    private final RestTemplate restTemplate = createRestTemplate();

    /** 远端提取失败必须在有限时间内返回，不能无限占用业务线程。 */
    private static RestTemplate createRestTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(180_000);
        return new RestTemplate(factory);
    }

    @Value("${feature-service.url}")
    private String featureServiceUrl;

    @Override
    @SuppressWarnings("unchecked")
    public FeatureResult extract(String workType, String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "文件路径不能为空");
        }
        if (workType == null || workType.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "作品类型不能为空");
        }

        log.info("调用特征提取服务: workType={}, filePath={}", workType, filePath);

        // 仅传受管对象键，由特征服务使用内部 MinIO 客户端读取，禁止任意 URL 下载。
        Map<String, String> request = Map.of(
                "object_name", filePath,
                "work_type", workType
        );

        // 显式声明 JSON Content-Type，避免 RestTemplate 默认行为变化导致 415
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        // 调用 Python 特征提取服务
        Map<String, Object> body = restTemplate.postForObject(
                featureServiceUrl + "/extract", entity, Map.class);

        // 解析响应结果：所有作品类型必须返回非空指纹（不允许空回退）
        if (body == null || body.get("perceptual_hash") == null) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "特征提取服务返回为空");
        }

        String algo = (String) body.get("algo");
        String algoVersion = (String) body.get("algo_version");
        String perceptualHash = (String) body.get("perceptual_hash");
        String extra = (String) body.get("extra");

        log.info("特征提取完成: workType={}, algo={}, version={}, hash={}",
                workType, algo, algoVersion, perceptualHash);
        return new FeatureResult(workType, algo, algoVersion, perceptualHash, extra);
    }
}
