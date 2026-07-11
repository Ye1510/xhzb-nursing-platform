package com.xhzb.nursing.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.xhzb.nursing.domain.Reservation;

/**
 * 预约信息Mapper接口
 *
 * @author ruoyi
 * @date 2026-08-28
 */
public interface ReservationMapper
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
     * 删除预约信息
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteReservationById(Long id);

    /**
     * 批量删除预约信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteReservationByIds(Long[] ids);

    /**
     * 查询当天取消预约数量
     *
     * @param userId 用户ID
     * @param start 今天开始时间
     * @param end 今天结束时间
     * @return 取消预约次数
     */
    public int countCancelled(@org.apache.ibatis.annotations.Param("userId") Long userId,
                              @org.apache.ibatis.annotations.Param("start") Date start,
                              @org.apache.ibatis.annotations.Param("end") Date end);

    /**
     * 查询每个时间段剩余预约次数
     *
     * @param start 今天开始时间
     * @return 时间段剩余预约次数列表
     */
    public List<Map<String, Object>> countByTime(@org.apache.ibatis.annotations.Param("start") Date start);

    /**
     * 查询当前用户的预约列表
     *
     * @param reservation 查询条件
     * @param userId 用户ID
     * @return 预约列表
     */
    public List<Reservation> selectReservationListByUser(@org.apache.ibatis.annotations.Param("reservation") Reservation reservation,
                                                         @org.apache.ibatis.annotations.Param("userId") Long userId);
}
