package com.lifechain.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 特征提取消息
 * <p>
 * 作品触发特征提取时发送，由 FeatureExtractConsumer 异步消费，
 * 调用 Python 特征服务完成感知哈希提取和相似度检测。
 * </p>
 *
 * @author LifeChain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureExtractMessage implements Serializable {
    /** 作品ID */
    private Long workId;
    /** 作品编号 */
    private String workNo;
    /** 作品类型（IMAGE/AUDIO/VIDEO/TEXT） */
    private String workType;
    /** 文件哈希值 */
    private String fileHash;
    /** 文件存储路径 */
    private String filePath;
    /** 创作者账户ID */
    private Long accountId;
}
