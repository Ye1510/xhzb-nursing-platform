package com.xhzb.nursing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.NursingTask;
import com.xhzb.nursing.domain.vo.NursingTaskVo;

import java.time.LocalDateTime;

/**
 * 护理任务Service接口
 */
public interface INursingTaskService extends IService<NursingTask> {

    /**
     * 分页查询护理任务列表
     */
    IPage<NursingTaskVo> selectNursingTaskList(Integer pageNum, Integer pageSize,
                                               String elderName, Long nurseId, Long projectId,
                                               Integer status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取护理任务详情
     */
    NursingTaskVo selectNursingTaskById(Long id);

    /**
     * 取消任务
     */
    void cancelTask(Long taskId, String reason);

    /**
     * 执行任务
     */
    void doTask(Long taskId, String mark, String taskImage, LocalDateTime estimatedServerTime);

    /**
     * 任务改期
     */
    void updateTime(Long taskId, LocalDateTime estimatedServerTime);
}
