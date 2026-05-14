package com.lifechain.work.service;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 内容特征提取服务实现
 * <p>
 * 基于文件内容计算感知哈希和特征向量。
 * 当前采用确定性哈希派生算法：
 * <ul>
 *   <li>感知哈希：SHA-256(PERCEPTUAL:{fileHash}) 截取前 32 位</li>
 *   <li>特征值：SHA-256(FEATURE:{fileHash}) 全量</li>
 * </ul>
 * 后续可通过替换此实现对接外部特征提取引擎（图像 pHash、音频指纹、视频帧指纹等）。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
public class FeatureExtractServiceImpl implements FeatureExtractService {

    @Override
    public FeatureResult extract(String fileHash, String fileType, String filePath) {
        if (fileHash == null || fileHash.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "文件哈希不能为空，无法提取特征");
        }

        log.info("开始特征提取, fileType={}, filePath={}", fileType, filePath);

        String perceptualHash = HashUtil.sha256("PERCEPTUAL:" + fileHash).substring(0, 32);
        String featureValue = HashUtil.sha256("FEATURE:" + fileHash);

        log.info("特征提取完成, perceptualHash={}", perceptualHash);
        return new FeatureResult("PERCEPTUAL_HASH", featureValue, perceptualHash);
    }
}
