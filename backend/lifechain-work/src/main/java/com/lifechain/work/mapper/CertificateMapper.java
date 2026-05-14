package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.CertificateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 证书数据访问层
 * <p>
 * 提供证书表的基础CRUD操作及按证书编号、作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface CertificateMapper extends BaseMapper<CertificateEntity> {

    /**
     * 根据证书编号查询证书
     *
     * @param certNo 证书编号
     * @return 证书实体，不存在返回null
     */
    @Select("SELECT * FROM certificate WHERE cert_no = #{certNo} AND deleted_flag = 0")
    CertificateEntity selectByCertNo(@Param("certNo") String certNo);

    /**
     * 根据作品ID查询证书列表
     *
     * @param workId 作品ID
     * @return 证书列表
     */
    @Select("SELECT * FROM certificate WHERE work_id = #{workId} AND deleted_flag = 0 ORDER BY version DESC")
    List<CertificateEntity> selectByWorkId(@Param("workId") Long workId);
}
