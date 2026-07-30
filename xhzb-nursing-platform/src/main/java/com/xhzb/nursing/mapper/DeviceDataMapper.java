package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.vo.TrendDataVo;

/**
 * 设备上报数据Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Mapper
public interface DeviceDataMapper extends BaseMapper<DeviceData>
{
    /**
     * 查询设备上报数据
     * 
     * @param id 设备上报数据主键
     * @return 设备上报数据
     */
    public DeviceData selectDeviceDataById(Long id);

    /**
     * 查询设备上报数据列表
     * 
     * @param deviceData 设备上报数据
     * @return 设备上报数据集合
     */
    public List<DeviceData> selectDeviceDataList(DeviceData deviceData);

    /**
     * 新增设备上报数据
     * 
     * @param deviceData 设备上报数据
     * @return 结果
     */
    public int insertDeviceData(DeviceData deviceData);

    /**
     * 修改设备上报数据
     * 
     * @param deviceData 设备上报数据
     * @return 结果
     */
    public int updateDeviceData(DeviceData deviceData);

    /**
     * 删除设备上报数据
     * 
     * @param id 设备上报数据主键
     * @return 结果
     */
    public int deleteDeviceDataById(Long id);

    /**
     * 批量删除设备上报数据
     * 
     * @param ids 需要删除的设备上报数据主键集合
     * @return 结果
     */
    public int deleteDeviceDataByIds(Long[] ids);

    /**
     * 按天统计指标（每小时一个点）
     *
     * @param iotId      设备ID
     * @param functionId 功能ID
     * @param start      当天 00:00
     * @param end        次日 00:00
     * @return {dateTime(HH:00), dataValue(该小时第一个非空值，若都空则0)}
     */
    List<TrendDataVo> queryDeviceDataListByDay(@Param("iotId") String iotId,
                                                @Param("functionId") String functionId,
                                                @Param("start") java.time.LocalDateTime start,
                                                @Param("end") java.time.LocalDateTime end);

    /**
     * 按周统计指标（每天一个点）
     *
     * @param iotId      设备ID
     * @param functionId 功能ID
     * @param start      周第一天 00:00
     * @param end        周最后一天之后的那天 00:00
     * @return {dateTime(MM.dd), dataValue(当天第一个非空值，若都空则0)}
     */
    List<TrendDataVo> queryDeviceDataListByWeek(@Param("iotId") String iotId,
                                                 @Param("functionId") String functionId,
                                                 @Param("start") java.time.LocalDateTime start,
                                                 @Param("end") java.time.LocalDateTime end);
}
