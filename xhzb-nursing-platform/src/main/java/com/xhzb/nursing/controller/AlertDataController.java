package com.xhzb.nursing.controller;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import com.xhzb.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 报警数据Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/nursing/alertData")
@Tag(name = "报警数据相关接口")
public class AlertDataController extends BaseController
{
    @Autowired
    private IAlertDataService alertDataService;

    /**
     * 查询报警数据列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:list')")
    @GetMapping("/list")
    @Operation(summary = "查询报警数据列表")
    public TableDataInfo list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(required = false) Integer status)
    {
        LambdaQueryWrapper<AlertData> wrapper = new LambdaQueryWrapper<>();
        if (deviceName != null && !deviceName.isEmpty()) {
            wrapper.eq(AlertData::getDeviceName, deviceName);
        }
        if (status != null) {
            wrapper.eq(AlertData::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(AlertData::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AlertData::getCreateTime, endTime);
        }
        wrapper.orderByDesc(AlertData::getCreateTime);
        Page<AlertData> page = new Page<>(pageNum, pageSize);
        Page<AlertData> result = alertDataService.page(page, wrapper);
        TableDataInfo tableData = getDataTable(result.getRecords());
        tableData.setTotal(result.getTotal());
        return tableData;
    }

    /**
     * 导出报警数据列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:export')")
    @Log(title = "报警数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出报警数据列表")
    public void export(HttpServletResponse response, AlertData alertData)
    {
        List<AlertData> list = alertDataService.selectAlertDataList(alertData);
        ExcelUtil<AlertData> util = new ExcelUtil<AlertData>(AlertData.class);
        util.exportExcel(response, list, "报警数据数据");
    }

    /**
     * 获取报警数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取报警数据详细信息")
    public AjaxResult getInfo(@Schema(name = "报警数据ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(alertDataService.selectAlertDataById(id));
    }

    /**
     * 新增报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:add')")
    @Log(title = "报警数据", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增报警数据")
    public AjaxResult add(@RequestBody AlertData alertData)
    {
        return toAjax(alertDataService.insertAlertData(alertData));
    }

    /**
     * 修改报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:edit')")
    @Log(title = "报警数据", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改报警数据")
    public AjaxResult edit(@RequestBody AlertData alertData)
    {
        return toAjax(alertDataService.updateAlertData(alertData));
    }

    /**
     * 删除报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:remove')")
    @Log(title = "报警数据", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除报警数据")
    public AjaxResult remove(@Schema(name = "报警数据ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(alertDataService.deleteAlertDataByIds(ids));
    }

    /**
     * 处理设备报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:alertData:edit')")
    @Log(title = "报警数据", businessType = BusinessType.UPDATE)
    @PutMapping("/handleAlertData")
    @Operation(summary = "处理设备报警数据")
    public AjaxResult handleAlertData(@RequestBody AlertData alertData)
    {
        AlertData update = new AlertData();
        update.setId(alertData.getId());
        update.setProcessingResult(alertData.getProcessingResult());
        update.setProcessingTime(alertData.getProcessingTime() != null ? alertData.getProcessingTime() : LocalDateTime.now());
        update.setStatus(1); // 已处理
        update.setProcessorId(SecurityUtils.getUserId());
        update.setProcessorName(SecurityUtils.getUsername());
        update.setUpdateTime(Date.from(java.time.ZonedDateTime.now().toInstant()));
        return toAjax(alertDataService.updateById(update) ? 1 : 0);
    }
}
