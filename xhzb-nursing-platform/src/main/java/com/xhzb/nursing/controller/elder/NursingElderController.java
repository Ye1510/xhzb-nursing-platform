package com.xhzb.nursing.controller.elder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.NursingElder;
import com.xhzb.nursing.domain.dto.SetNursingDto;
import com.xhzb.nursing.mapper.NursingElderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 护理员老人关联Controller
 */
@RestController
@RequestMapping("/elder/nursingElder")
@Tag(name = "护理员老人关联接口")
public class NursingElderController extends BaseController {

    @Autowired
    private NursingElderMapper nursingElderMapper;

    /**
     * 设置护理员（批量）
     * 先删除老人所有关联，再批量新增
     */
    @PostMapping("/setNursing")
    @Operation(summary = "设置护理员")
    public AjaxResult setNursing(@RequestBody List<SetNursingDto> list) {
        Long currentUserId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        for (SetNursingDto dto : list) {
            // 先删除该老人所有护理员关联
            nursingElderMapper.delete(new LambdaQueryWrapper<NursingElder>()
                    .eq(NursingElder::getElderId, dto.getElderId()));

            // 再批量新增
            if (dto.getNursingIds() != null) {
                for (Long nursingId : dto.getNursingIds()) {
                    NursingElder ne = new NursingElder();
                    ne.setElderId(dto.getElderId());
                    ne.setNursingId(nursingId);
                    ne.setCreateTime(now);
                    ne.setCreateBy(currentUserId);
                    nursingElderMapper.insert(ne);
                }
            }
        }
        return success(true);
    }
}
