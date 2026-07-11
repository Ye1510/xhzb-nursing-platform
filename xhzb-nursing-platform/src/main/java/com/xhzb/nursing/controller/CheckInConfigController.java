package com.xhzb.nursing.controller;

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
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.CheckInConfig;
import com.xhzb.nursing.service.ICheckInConfigService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 入住配置Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/nursing/checkInConfig")
@Tag(name = "入住配置相关接口")
public class CheckInConfigController extends BaseController
{
    @Autowired
    private ICheckInConfigService checkInConfigService;

    /**
     * 查询入住配置列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:list')")
    @GetMapping("/list")
    @Operation(summary = "查询入住配置列表")
    public TableDataInfo list(CheckInConfig checkInConfig)
    {
        startPage();
        List<CheckInConfig> list = checkInConfigService.selectCheckInConfigList(checkInConfig);
        return getDataTable(list);
    }

    /**
     * 导出入住配置列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:export')")
    @Log(title = "入住配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出入住配置列表")
    public void export(HttpServletResponse response, CheckInConfig checkInConfig)
    {
        List<CheckInConfig> list = checkInConfigService.selectCheckInConfigList(checkInConfig);
        ExcelUtil<CheckInConfig> util = new ExcelUtil<CheckInConfig>(CheckInConfig.class);
        util.exportExcel(response, list, "入住配置数据");
    }

    /**
     * 获取入住配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取入住配置详细信息")
    public AjaxResult getInfo(@Schema(name = "入住配置ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(checkInConfigService.selectCheckInConfigById(id));
    }

    /**
     * 新增入住配置
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:add')")
    @Log(title = "入住配置", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增入住配置")
    public AjaxResult add(@RequestBody CheckInConfig checkInConfig)
    {
        return toAjax(checkInConfigService.insertCheckInConfig(checkInConfig));
    }

    /**
     * 修改入住配置
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:edit')")
    @Log(title = "入住配置", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改入住配置")
    public AjaxResult edit(@RequestBody CheckInConfig checkInConfig)
    {
        return toAjax(checkInConfigService.updateCheckInConfig(checkInConfig));
    }

    /**
     * 删除入住配置
     */
    @PreAuthorize("@ss.hasPermi('nursing:checkInConfig:remove')")
    @Log(title = "入住配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除入住配置")
    public AjaxResult remove(@Schema(name = "入住配置ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(checkInConfigService.deleteCheckInConfigByIds(ids));
    }
}
