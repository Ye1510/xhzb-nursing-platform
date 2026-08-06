package com.xhzb.nursing.controller.member;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.service.INursingProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 小程序端-服务项接口
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/member/orders/project")
@Tag(name = "服务项接口")
public class MemberNursingProjectController extends BaseController
{
    @Autowired
    private INursingProjectService nursingProjectService;

    /**
     * 分页查询护理项目列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询护理项目列表")
    public R<TableDataInfo> page(NursingProject nursingProject)
    {
        startPage();
        List<NursingProject> list = nursingProjectService.selectNursingProjectList(nursingProject);
        return R.ok(getDataTable(list));
    }

    /**
     * 根据编号查询护理项目信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据编号查询护理项目信息")
    public R<NursingProject> getInfo(@PathVariable Long id)
    {
        return R.ok(nursingProjectService.selectNursingProjectById(id));
    }
}
