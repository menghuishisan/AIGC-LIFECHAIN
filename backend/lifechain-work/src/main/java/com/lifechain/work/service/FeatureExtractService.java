package com.lifechain.work.service;

/**
 * 特征提取服务接口
 * <p>
 * 调用 Python 特征服务，按作品类型从文件中提取 256-bit 二进制感知指纹。
 * 算法：IMAGE/VIDEO=PDQ，AUDIO/TEXT=MinHash，MODEL=D2 形状描述符。
 * </p>
 *
 * @author LifeChain
 */
public interface FeatureExtractService {

    /**
     * 特征提取结果
     *
     * @param workType        作品类型（IMAGE/VIDEO/AUDIO/TEXT/MODEL）
     * @param algo            算法名称（PDQ/MINHASH/D2）
     * @param algoVersion     算法版本
     * @param perceptualHash  256-bit 指纹的 hex 编码（64 字符）
     * @param extra           算法专属辅助信息（JSON 字符串）
     */
    record FeatureResult(
            String workType,
            String algo,
            String algoVersion,
            String perceptualHash,
            String extra
    ) {}

    /**
     * 从作品文件提取 256-bit 感知指纹
     *
     * @param workType 作品类型
     * @param filePath 文件在 MinIO 中的存储路径
     * @return 提取结果
     */
    FeatureResult extract(String workType, String filePath);
}
