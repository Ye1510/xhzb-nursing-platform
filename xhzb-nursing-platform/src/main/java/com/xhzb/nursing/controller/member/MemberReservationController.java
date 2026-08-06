package com.xhzb.nursing.controller.member;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 小程序端-参观预约接口
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/member/reservation")
@Tag(name = "预约信息相关接口")
public class MemberReservationController extends BaseController
{
    @Autowired
    private IReservationService reservationService;

    /**
     * 查询取消预约数量
     * 当天取消预约次数大于3次，则不让预约
     */
    @GetMapping("/cancelled-count")
    @Operation(summary = "查询取消预约数量")
    public R<Integer> getCancelledReservationCount()
    {
        Long userId = UserThreadLocal.getUserId();
        return R.ok(reservationService.countCancelled(userId));
    }

    /**
     * 查询每个时间段剩余预约次数
     * 每个时间段最大预约数为6，返回已有预约的时间段及其剩余次数
     */
    @GetMapping("/countByTime")
    @Operation(summary = "查询每个时间段剩余预约次数")
    public R<List<Map<String, Object>>> countByTime()
    {
        return R.ok(reservationService.countByTime());
    }

    /**
     * 新增预约
     */
    @PostMapping
    @Operation(summary = "新增预约")
    public R<Void> addReservation(@RequestBody Reservation reservation)
    {
        Long userId = UserThreadLocal.getUserId();
        reservation.setCreateBy(userId.toString());
        reservation.setCreateTime(new Date());
        reservation.setStatus(0); // 0-待上门
        int rows = reservationService.insertReservation(reservation);
        return rows > 0 ? R.ok() : R.fail("新增预约失败");
    }

    /**
     * 分页查询预约
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询预约")
    public R<TableDataInfo> page(Reservation reservation)
    {
        Long userId = UserThreadLocal.getUserId();
        startPage();
        List<Reservation> list = reservationService.selectUserReservationList(reservation, userId);
        return R.ok(getDataTable(list));
    }

    /**
     * 取消预约
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预约")
    public R<Void> cancelReservation(@PathVariable Long id)
    {
        Long userId = UserThreadLocal.getUserId();
        int rows = reservationService.cancelReservation(id, userId);
        return rows > 0 ? R.ok() : R.fail("取消预约失败");
    }
}
