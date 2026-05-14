package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.CertificateTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 证书模板数据访问层
 * <p>
 * 提供证书模板表的基础CRUD操作及按模板编码查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface CertificateTemplateMapper extends BaseMapper<CertificateTemplateEntity> {

    /**
     * 根据模板编码查询模板
     *
     * @param templateCode 模板编码
     * @return 模板实体，不存在返回null
     */
    @Select("SELECT * FROM certificate_template WHERE template_code = #{templateCode} AND deleted_flag = 0")
    CertificateTemplateEntity selectByTemplateCode(@Param("templateCode") String templateCode);
}
