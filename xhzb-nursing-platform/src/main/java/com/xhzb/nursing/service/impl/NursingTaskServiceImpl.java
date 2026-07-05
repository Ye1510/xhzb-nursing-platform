package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.NursingTask;
import com.xhzb.nursing.domain.vo.NursingTaskVo;
import com.xhzb.nursing.mapper.NursingTaskMapper;
import com.xhzb.nursing.service.INursingTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 护理任务Service业务层
 */
@Slf4j
@Service
public class NursingTaskServiceImpl extends ServiceImpl<NursingTaskMapper, NursingTask> implements INursingTaskService {

    @Override
    public IPage<NursingTaskVo> selectNursingTaskList(Integer pageNum, Integer pageSize,
                                                      String elderName, Long nurseId, Long projectId,
                                                      Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        Page<NursingTaskVo> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectNursingTaskVoPage(page, elderName, nurseId, projectId, status, startTime, endTime);
    }

    @Override
    public NursingTaskVo selectNursingTaskById(Long id) {
        return baseMapper.selectNursingTaskVoById(id);
    }

    @Override
    public void cancelTask(Long taskId, String reason) {
        NursingTask task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("护理任务不存在");
        }
        task.setStatus(3); // 已关闭
        task.setCancelReason(reason);
        task.setUpdateTime(Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        task.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        updateById(task);
    }

    @Override
    public void doTask(Long taskId, String mark, String taskImage, LocalDateTime estimatedServerTime) {
        NursingTask task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("护理任务不存在");
        }
        task.setStatus(2); // 已执行
        task.setMark(mark);
        task.setTaskImage(taskImage);
        task.setRealServerTime(LocalDateTime.now());
        if (estimatedServerTime != null) {
            task.setEstimatedServerTime(estimatedServerTime);
        }
        task.setUpdateTime(Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        task.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        updateById(task);
    }

    @Override
    public void updateTime(Long taskId, LocalDateTime estimatedServerTime) {
        NursingTask task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("护理任务不存在");
        }
        task.setEstimatedServerTime(estimatedServerTime);
        task.setUpdateTime(Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        task.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        updateById(task);
    }
}
