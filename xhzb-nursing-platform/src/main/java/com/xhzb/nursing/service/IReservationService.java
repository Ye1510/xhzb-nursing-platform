package com.xhzb.nursing.service;

import java.util.List;
import java.util.Map;
import com.xhzb.nursing.domain.Reservation;

/**
 * 预约信息Service接口
 *
 * @author ruoyi
 * @date 2026-08-28
 */
public interface IReservationService
{
    /**
     * 查询预约信息
     *
     * @param id 主键
     * @return 预约信息
     */
    public Reservation selectReservationById(Long id);

    /**
     * 查询预约信息列表
     *
     * @param reservation 预约信息
     * @return 预约信息集合
     */
    public List<Reservation> selectReservationList(Reservation reservation);

    /**
     * 新增预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    public int insertReservation(Reservation reservation);

    /**
     * 修改预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    public int updateReservation(Reservation reservation);

    /**
     * 批量删除预约信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteReservationByIds(Long[] ids);

    /**
     * 删除预约信息
     *
     * @param id 预约信息主键
     * @return 结果
     */
    public int deleteReservationById(Long id);

    /**
     * 查询当天取消预约数量
     *
     * @param userId 用户ID
     * @return 取消预约次数
     */
    public int countCancelled(Long userId);

    /**
     * 查询每个时间段剩余预约次数
     *
     * @return 时间段剩余预约次数列表
     */
    public List<Map<String, Object>> countByTime();

    /**
     * 分页查询当前用户的预约
     *
     * @param reservation 查询条件
     * @param userId 用户ID
     * @return 预约列表
     */
    public List<Reservation> selectUserReservationList(Reservation reservation, Long userId);

    /**
     * 取消预约
     *
     * @param id 预约ID
     * @param userId 用户ID
     * @return 结果
     */
    public int cancelReservation(Long id, Long userId);
}
