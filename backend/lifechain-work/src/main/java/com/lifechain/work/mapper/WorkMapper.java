package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.WorkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作品数据访问层
 * <p>
 * 提供作品表的基础CRUD操作及按作品编号、创作者账户ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkMapper extends BaseMapper<WorkEntity> {

    /**
     * 根据作品编号查询作品
     *
     * @param workNo 作品编号
     * @return 作品实体，不存在返回null
     */
    @Select("SELECT * FROM work WHERE work_no = #{workNo} AND deleted_flag = 0")
    WorkEntity selectByWorkNo(@Param("workNo") String workNo);

    /**
     * 根据创作者账户ID和状态查询作品列表
     *
     * @param creatorAccountId 创作者账户ID
     * @param status           作品状态，传null时不过滤
     * @return 作品列表
     */
    @Select("<script>" +
            "SELECT * FROM work WHERE creator_account_id = #{creatorAccountId} AND deleted_flag = 0" +
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<WorkEntity> selectByCreatorAccountIdAndStatus(@Param("creatorAccountId") Long creatorAccountId,
                                                       @Param("status") String status);
}
