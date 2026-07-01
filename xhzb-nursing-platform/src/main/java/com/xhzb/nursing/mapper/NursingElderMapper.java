package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.common.core.domain.entity.SysUser;
import com.xhzb.nursing.domain.NursingElder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NursingElderMapper extends BaseMapper<NursingElder> {

    /**
     * 根据老人ID查询关联的护理员（用户）列表
     */
    @Select("SELECT DISTINCT su.user_id, su.nick_name " +
            "FROM nursing_elder ne " +
            "LEFT JOIN sys_user su ON ne.nursing_id = su.user_id " +
            "WHERE ne.elder_id = #{elderId} AND su.user_id IS NOT NULL")
    List<SysUser> selectUserByElderId(@Param("elderId") Long elderId);
}
