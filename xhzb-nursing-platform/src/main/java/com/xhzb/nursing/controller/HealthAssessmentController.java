package com.xhzb.nursing.controller;

import java.util.List;
import java.util.UUID;

import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
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
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 健康评估记录Controller
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/nursing/healthAssessment")
@Tag(name = "健康评估记录相关接口")
public class HealthAssessmentController extends BaseController
{
    @Autowired
    private IHealthAssessmentService healthAssessmentService;

    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    @Autowired
    private OSSAliyunFileStorageService fileStorageService;

    /**
     * 上传体检报告等附件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传健康评估附件（体检报告PDF等）")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try {
            // 文件名 ---> UUID.后缀
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // 把文件上传到oss中
            String url = fileStorageService.store(filename, file.getInputStream());

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", url);
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 查询健康评估记录列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:list')")
    @GetMapping("/list")
    @Operation(summary = "查询健康评估记录列表")
    public TableDataInfo list(HealthAssessment healthAssessment)
    {
        startPage();
        List<HealthAssessment> list = healthAssessmentService.selectHealthAssessmentList(healthAssessment);
        return getDataTable(list);
    }

    /**
     * 导出健康评估记录列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:export')")
    @Log(title = "健康评估记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出健康评估记录列表")
    public void export(HttpServletResponse response, HealthAssessment healthAssessment)
    {
        List<HealthAssessment> list = healthAssessmentService.selectHealthAssessmentList(healthAssessment);
        ExcelUtil<HealthAssessment> util = new ExcelUtil<HealthAssessment>(HealthAssessment.class);
        util.exportExcel(response, list, "健康评估记录数据");
    }

    /**
     * 获取健康评估记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取健康评估记录详细信息")
    public AjaxResult getInfo(@Schema(name = "健康评估记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(healthAssessmentService.selectHealthAssessmentById(id));
    }

    /**
     * 新增健康评估记录
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:add')")
    @Log(title = "健康评估记录", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增健康评估记录")
    public AjaxResult add(@RequestBody ElderAssessmentDto dto)
    {
        return success(healthAssessmentService.insertHealthAssessment(dto));
    }
    /**
     * 修改健康评估记录
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:edit')")
    @Log(title = "健康评估记录", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改健康评估记录")
    public AjaxResult edit(@RequestBody ElderAssessmentDto dto)
    {
        return success(healthAssessmentService.updateHealthAssessment(dto));
    }

    /**
     * 取消健康评估
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:edit')")
    @Log(title = "健康评估记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    @Operation(summary = "取消健康评估")
    public AjaxResult cancel(@Schema(name = "健康评估记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return toAjax(healthAssessmentService.cancelHealthAssessment(id));
    }

    /**
     * 删除健康评估记录
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:remove')")
    @Log(title = "健康评估记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除健康评估记录")
    public AjaxResult remove(@Schema(name = "健康评估记录ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(healthAssessmentService.deleteHealthAssessmentByIds(ids));
    }

    /**
     * AI评估流程：保存评估数据并执行两阶段AI分析
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:add')")
    @Log(title = "AI健康评估", businessType = BusinessType.INSERT)
    @PostMapping("/assessmentData")
    @Operation(summary = "AI评估流程")
    public AjaxResult assessmentData(@RequestBody ElderAssessmentDto dto)
    {
        try {
            Long assessmentId = healthAssessmentService.processAssessment(dto);
            return success(assessmentId);
        } catch (Exception e) {
            return error("AI分析失败：" + e.getMessage());
        }
    }

    /**
     * 查看评估详情（评估结果报告）
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:query')")
    @GetMapping(value = "/report/{id}")
    @Operation(summary = "查看评估详情报告")
    public AjaxResult report(@Schema(name = "评估ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(healthAssessmentReportService.getByHealthAssessmentId(id));
    }

    /**
     * 查看老人信息详情（入住字段填充）
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:query')")
    @GetMapping(value = "/elder/{id}")
    @Operation(summary = "查看老人信息详情（入住字段填充）")
    public AjaxResult getElderInfo(@Schema(name = "评估ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(healthAssessmentService.getElderInfoByAssessmentId(id));
    }
}
