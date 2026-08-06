package com.xhzb.nursing.controller.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.dto.MemberElderBindDto;
import com.xhzb.nursing.domain.dto.WxLoginDto;
import com.xhzb.nursing.domain.vo.MemberElderVo;
import com.xhzb.nursing.domain.vo.WxLoginVo;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.nursing.service.IDeviceDataService;
import com.xhzb.nursing.service.IFamilyMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 老人家属Controller
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/member/user")
@Tag(name = "小程序用户相关接口")
public class FamilyMemberController extends BaseController
{
    @Autowired
    private IFamilyMemberService familyMemberService;

    @Autowired
    private IDeviceDataService deviceDataService;

    @Autowired
    private IAlertDataService alertDataService;

    /**
     * 微信小程序登录
     */
    @PostMapping("/login")
    @Operation(summary = "微信小程序登录")
    public AjaxResult login(@Valid @RequestBody WxLoginDto dto)
    {
        WxLoginVo result = familyMemberService.login(dto);
        return success(result);
    }

    /**
     * 绑定老人
     */
    @PostMapping("/add")
    @Operation(summary = "绑定老人")
    public AjaxResult add(@RequestBody MemberElderBindDto dto)
    {
        Long familyMemberId = UserThreadLocal.getUserId();
        familyMemberService.bindElder(familyMemberId, dto.getName(), dto.getIdCard(), dto.getRemark());
        return success();
    }

    /**
     * 分页查询已绑定的老人列表
     */
    @GetMapping("/list-by-page")
    @Operation(summary = "分页查询已绑定老人列表")
    public AjaxResult listByPage()
    {
        Long familyMemberId = UserThreadLocal.getUserId();
        List<MemberElderVo> list = familyMemberService.listBoundElders(familyMemberId);
        AjaxResult ajax = AjaxResult.success(list);
        ajax.put("total", list.size());
        return ajax;
    }

    /**
     * 查询我的老人（全部）
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的老人列表")
    public AjaxResult my()
    {
        Long familyMemberId = UserThreadLocal.getUserId();
        return AjaxResult.success(familyMemberService.listBoundElders(familyMemberId));
    }

    /**
     * 解绑老人
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "解绑老人")
    public AjaxResult deleteById(@RequestParam Long id)
    {
        Long familyMemberId = UserThreadLocal.getUserId();
        familyMemberService.unbindElder(id, familyMemberId);
        return success();
    }

    /**
     * 查询健康数据（当前最新值，来自Redis缓存）
     */
    @GetMapping("/queryServiceProperties/{iotId}")
    @Operation(summary = "查询健康数据(当前最新)")
    public AjaxResult queryServiceProperties(
            @Parameter(description = "设备iotId") @PathVariable String iotId) {
        return success(deviceDataService.queryLatestHealthData(iotId));
    }

    /**
     * 按天统计查询指标数据
     *
     * @param iotId      设备ID
     * @param functionId 物模型功能ID（如 HeartRate）
     * @param date       日期 yyyy-MM-dd，不填默认今天
     */
    @GetMapping("/queryDeviceDataListByDay")
    @Operation(summary = "按天统计查询指标数据")
    public AjaxResult queryDeviceDataListByDay(
            @RequestParam String iotId,
            @RequestParam String functionId,
            @RequestParam(required = false) String date) {
        if (date == null || date.isEmpty()) {
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return success(deviceDataService.queryDeviceDataListByDay(iotId, functionId, date));
    }

    /**
     * 按周统计查询指标数据
     *
     * @param iotId      设备ID
     * @param functionId 物模型功能ID
     * @param date       日期 yyyy-MM-dd，不填默认今天（取所在自然周 周一至周日）
     */
    @GetMapping("/queryDeviceDataListByWeek")
    @Operation(summary = "按周统计查询指标数据")
    public AjaxResult queryDeviceDataListByWeek(
            @RequestParam String iotId,
            @RequestParam String functionId,
            @RequestParam(required = false) String date) {
        if (date == null || date.isEmpty()) {
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return success(deviceDataService.queryDeviceDataListByWeek(iotId, functionId, date));
    }

    /**
     * 分页查询异常告警数据（小程序端）
     *
     * @param iotId      设备ID
     * @param functionId 功能标识符
     * @param startTime  开始时间（毫秒时间戳）
     * @param endTime    结束时间（毫秒时间戳）
     * @param pageNum    页码
     * @param pageSize   每页条数
     */
    @GetMapping("/pageQueryAlertData")
    @Operation(summary = "分页查询异常告警数据")
    public AjaxResult pageQueryAlertData(
            @RequestParam String iotId,
            @RequestParam String functionId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<AlertData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertData::getIotId, iotId);
        wrapper.eq(AlertData::getFunctionId, functionId);
        if (startTime != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
            wrapper.ge(AlertData::getCreateTime, start);
        }
        if (endTime != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
            wrapper.le(AlertData::getCreateTime, end);
        }
        wrapper.orderByDesc(AlertData::getCreateTime);
        Page<AlertData> page = new Page<>(pageNum, pageSize);
        Page<AlertData> result = alertDataService.page(page, wrapper);
        AjaxResult ajax = AjaxResult.success(result.getRecords());
        ajax.put("total", result.getTotal());
        return ajax;
    }
}
