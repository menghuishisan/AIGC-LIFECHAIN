package com.lifechain.infra.attachment;

/**
 * 业务编号→业务主键解析器（SPI）
 * <p>
 * 由上层模块实现，按 bizType + bizNo 反查数据库主键，
 * 用于文件回调后补全 bizId，确保附件记录稳定绑定到真实业务对象。
 * </p>
 *
 * @author LifeChain
 */
public interface BizIdResolver {

    /**
     * 根据业务类型和业务编号解析对应的数据库主键
     *
     * @param bizType 业务类型（WORK/ORDER/DISPUTE等）
     * @param bizNo   业务编号
     * @return 对应的数据库主键，未找到时返回 null
     */
    Long resolve(String bizType, String bizNo);
}
