package com.xhzb.nursing.controller.member;

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
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.service.IFamilyMemberElderService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 客户老人关联Controller
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/nursing/member")
@Tag(name = "客户老人关联相关接口")
public class FamilyMemberElderController extends BaseController
{
    @Autowired
    private IFamilyMemberElderService familyMemberElderService;

    /**
     * 查询客户老人关联列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:list')")
    @GetMapping("/list")
    @Operation(summary = "查询客户老人关联列表")
    public TableDataInfo list(FamilyMemberElder familyMemberElder)
    {
        startPage();
        List<FamilyMemberElder> list = familyMemberElderService.selectFamilyMemberElderList(familyMemberElder);
        return getDataTable(list);
    }

    /**
     * 导出客户老人关联列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:export')")
    @Log(title = "客户老人关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出客户老人关联列表")
    public void export(HttpServletResponse response, FamilyMemberElder familyMemberElder)
    {
        List<FamilyMemberElder> list = familyMemberElderService.selectFamilyMemberElderList(familyMemberElder);
        ExcelUtil<FamilyMemberElder> util = new ExcelUtil<FamilyMemberElder>(FamilyMemberElder.class);
        util.exportExcel(response, list, "客户老人关联数据");
    }

    /**
     * 获取客户老人关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取客户老人关联详细信息")
    public AjaxResult getInfo(@Schema(name = "客户老人关联ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(familyMemberElderService.selectFamilyMemberElderById(id));
    }

    /**
     * 新增客户老人关联
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:add')")
    @Log(title = "客户老人关联", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增客户老人关联")
    public AjaxResult add(@RequestBody FamilyMemberElder familyMemberElder)
    {
        return toAjax(familyMemberElderService.insertFamilyMemberElder(familyMemberElder));
    }

    /**
     * 修改客户老人关联
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:edit')")
    @Log(title = "客户老人关联", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改客户老人关联")
    public AjaxResult edit(@RequestBody FamilyMemberElder familyMemberElder)
    {
        return toAjax(familyMemberElderService.updateFamilyMemberElder(familyMemberElder));
    }

    /**
     * 删除客户老人关联
     */
    @PreAuthorize("@ss.hasPermi('nursing:member:remove')")
    @Log(title = "客户老人关联", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除客户老人关联")
    public AjaxResult remove(@Schema(name = "客户老人关联ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(familyMemberElderService.deleteFamilyMemberElderByIds(ids));
    }
}
