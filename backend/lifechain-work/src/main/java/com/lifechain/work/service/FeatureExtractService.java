package com.lifechain.work.service;

/**
 * 特征提取服务接口
 * <p>
 * 负责从作品文件中提取内容特征（感知哈希、特征向量等），
 * 用于重复检测和版权保护。
 * </p>
 *
 * @author LifeChain
 */
public interface FeatureExtractService {

    /**
     * 特征提取结果
     */
    record FeatureResult(String featureType, String featureValue, String perceptualHash) {}

    /**
     * 从作品文件提取内容特征
     *
     * @param fileHash    文件哈希值
     * @param fileType    文件类型（image/audio/video/text等）
     * @param filePath    文件存储路径
     * @return 提取结果
     * @throws com.lifechain.common.exception.BizException 提取失败时抛出
     */
    FeatureResult extract(String fileHash, String fileType, String filePath);
}
