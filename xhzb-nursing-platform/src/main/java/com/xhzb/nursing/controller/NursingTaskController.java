package com.xhzb.nursing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.nursing.domain.vo.NursingTaskVo;
import com.xhzb.nursing.service.INursingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 护理任务Controller
 */
@RestController
@RequestMapping("/nursing/nursingTask")
@Tag(name = "护理任务相关接口")
public class NursingTaskController extends BaseController {

    @Autowired
    private INursingTaskService nursingTaskService;

    /**
     * 查询护理任务列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:list')")
    @GetMapping("/list")
    @Operation(summary = "查询护理任务列表")
    public TableDataInfo list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String elderName,
            @RequestParam(required = false) Long nurseId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        IPage<NursingTaskVo> page = nursingTaskService.selectNursingTaskList(pageNum, pageSize, elderName, nurseId, projectId, status, startTime, endTime);
        TableDataInfo tableData = getDataTable(page.getRecords());
        tableData.setTotal(page.getTotal());
        return tableData;
    }

    /**
     * 获取护理任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:query')")
    @GetMapping("/{id}")
    @Operation(summary = "获取护理任务详细信息")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(nursingTaskService.selectNursingTaskById(id));
    }

    /**
     * 取消任务
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:edit')")
    @PutMapping("/cancel")
    @Operation(summary = "取消护理任务")
    public AjaxResult cancel(@RequestBody Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String reason = params.get("reason") != null ? params.get("reason").toString() : "";
        nursingTaskService.cancelTask(taskId, reason);
        return success();
    }

    /**
     * 执行任务
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:edit')")
    @PutMapping("/do")
    @Operation(summary = "执行护理任务")
    public AjaxResult doTask(@RequestBody Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String mark = params.get("mark") != null ? params.get("mark").toString() : "";
        String taskImage = params.get("taskImage") != null ? params.get("taskImage").toString() : null;
        String timeStr = params.get("estimatedServerTime") != null ? params.get("estimatedServerTime").toString() : null;
        LocalDateTime estimatedServerTime = null;
        if (timeStr != null && !timeStr.isEmpty()) {
            estimatedServerTime = LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        nursingTaskService.doTask(taskId, mark, taskImage, estimatedServerTime);
        return success();
    }

    /**
     * 任务改期
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:edit')")
    @PutMapping("/updateTime")
    @Operation(summary = "护理任务改期")
    public AjaxResult updateTime(@RequestBody Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String timeStr = params.get("estimatedServerTime").toString();
        LocalDateTime estimatedServerTime = LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        nursingTaskService.updateTime(taskId, estimatedServerTime);
        return success();
    }
}
