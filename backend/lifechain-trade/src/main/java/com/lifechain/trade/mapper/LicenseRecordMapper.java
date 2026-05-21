package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.trade.entity.LicenseRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 授权记录数据访问层
 * <p>
 * 提供授权记录表的基础CRUD操作及常用查询方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface LicenseRecordMapper extends BaseMapper<LicenseRecordEntity> {

    /**
     * 根据授权编号查询
     *
     * @param licenseNo 授权编号
     * @return 授权记录实体，不存在则返回null
     */
    default LicenseRecordEntity selectByLicenseNo(@Param("licenseNo") String licenseNo) {
        return selectOne(new LambdaQueryWrapper<LicenseRecordEntity>()
                .eq(LicenseRecordEntity::getLicenseNo, licenseNo));
    }

    /**
     * 根据订单ID查询授权记录
     *
     * @param orderId 订单ID
     * @return 授权记录实体，不存在则返回null
     */
    default LicenseRecordEntity selectByOrderId(@Param("orderId") Long orderId) {
        return selectOne(new LambdaQueryWrapper<LicenseRecordEntity>()
                .eq(LicenseRecordEntity::getOrderId, orderId));
    }

    /**
     * 按被授权方账户ID分页查询
     *
     * @param page              分页参数
     * @param licenseeAccountId 被授权方账户ID
     * @return 分页结果
     */
    default IPage<LicenseRecordEntity> selectByLicenseeAccountId(Page<LicenseRecordEntity> page,
                                                                  @Param("licenseeAccountId") Long licenseeAccountId) {
        LambdaQueryWrapper<LicenseRecordEntity> wrapper = new LambdaQueryWrapper<LicenseRecordEntity>()
                .eq(LicenseRecordEntity::getLicenseeAccountId, licenseeAccountId)
                .orderByDesc(LicenseRecordEntity::getCreatedAt);
        return selectPage(page, wrapper);
    }

    default boolean existsActiveLicense(@Param("workId") Long workId,
                                        @Param("licenseeAccountId") Long licenseeAccountId) {
        return selectCount(new LambdaQueryWrapper<LicenseRecordEntity>()
                .eq(LicenseRecordEntity::getWorkId, workId)
                .eq(LicenseRecordEntity::getLicenseeAccountId, licenseeAccountId)
                .eq(LicenseRecordEntity::getLicenseStatus, "LICENSE_ACTIVE")) > 0;
    }
}
