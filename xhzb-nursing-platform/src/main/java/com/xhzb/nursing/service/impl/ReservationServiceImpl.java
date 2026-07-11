package com.xhzb.nursing.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.ReservationMapper;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.service.IReservationService;

/**
 * 预约信息Service业务层处理
 *
 * @author ruoyi
 * @date 2026-08-28
 */
@Service
public class ReservationServiceImpl implements IReservationService
{
    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 查询预约信息
     *
     * @param id 主键
     * @return 预约信息
     */
    @Override
    public Reservation selectReservationById(Long id)
    {
        return reservationMapper.selectReservationById(id);
    }

    /**
     * 查询预约信息列表
     *
     * @param reservation 预约信息
     * @return 预约信息
     */
    @Override
    public List<Reservation> selectReservationList(Reservation reservation)
    {
        return reservationMapper.selectReservationList(reservation);
    }

    /**
     * 新增预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    @Override
    public int insertReservation(Reservation reservation)
    {
        return reservationMapper.insertReservation(reservation);
    }

    /**
     * 修改预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    @Override
    public int updateReservation(Reservation reservation)
    {
        return reservationMapper.updateReservation(reservation);
    }

    /**
     * 批量删除预约信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    @Override
    public int deleteReservationByIds(Long[] ids)
    {
        return reservationMapper.deleteReservationByIds(ids);
    }

    /**
     * 删除预约信息
     *
     * @param id 预约信息主键
     * @return 结果
     */
    @Override
    public int deleteReservationById(Long id)
    {
        return reservationMapper.deleteReservationById(id);
    }

    /**
     * 查询当天取消预约数量
     *
     * @param userId 用户ID
     * @return 取消预约次数
     */
    @Override
    public int countCancelled(Long userId)
    {
        Date[] todayRange = getTodayRange();
        return reservationMapper.countCancelled(userId, todayRange[0], todayRange[1]);
    }

    /**
     * 查询每个时间段剩余预约次数
     * 每个时间段最大预约数为6，返回已有预约的时间段及其剩余次数
     *
     * @return 时间段剩余预约次数列表
     */
    @Override
    public List<Map<String, Object>> countByTime()
    {
        Date[] todayRange = getTodayRange();
        return reservationMapper.countByTime(todayRange[0]);
    }

    /**
     * 分页查询当前用户的预约
     *
     * @param reservation 查询条件
     * @param userId 用户ID
     * @return 预约列表
     */
    @Override
    public List<Reservation> selectUserReservationList(Reservation reservation, Long userId)
    {
        return reservationMapper.selectReservationListByUser(reservation, userId);
    }

    /**
     * 取消预约
     *
     * @param id 预约ID
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public int cancelReservation(Long id, Long userId)
    {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStatus(2); // 2-已取消
        reservation.setUpdateBy(userId.toString());
        reservation.setUpdateTime(new Date());
        return reservationMapper.updateReservation(reservation);
    }

    /**
     * 获取今天的时间范围 [start, end)
     */
    private Date[] getTodayRange()
    {
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date start = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date end = calendar.getTime();
        return new Date[] { start, end };
    }
}
