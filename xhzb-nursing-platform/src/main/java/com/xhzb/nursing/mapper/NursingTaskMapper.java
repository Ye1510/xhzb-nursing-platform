package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xhzb.nursing.domain.NursingTask;
import com.xhzb.nursing.domain.vo.NursingTaskVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NursingTaskMapper extends BaseMapper<NursingTask> {

    /**
     * 分页查询护理任务列表（联表查询老人、护理员姓名等）
     */
    IPage<NursingTaskVo> selectNursingTaskVoPage(IPage<NursingTaskVo> page,
                                                  @Param("elderName") String elderName,
                                                  @Param("nurseId") Long nurseId,
                                                  @Param("projectId") Long projectId,
                                                  @Param("status") Integer status,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询护理任务详情（含老人信息、护理等级、护理员姓名等）
     */
    NursingTaskVo selectNursingTaskVoById(@Param("id") Long id);
}
