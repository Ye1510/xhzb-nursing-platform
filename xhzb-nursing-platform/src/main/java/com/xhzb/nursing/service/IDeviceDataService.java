package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.vo.HealthDataVo;
import com.xhzb.nursing.domain.vo.TrendDataVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 设备上报数据Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IDeviceDataService extends IService<DeviceData>
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
     * 批量删除设备上报数据
     * 
     * @param ids 需要删除的设备上报数据主键集合
     * @return 结果
     */
    public int deleteDeviceDataByIds(Long[] ids);

    /**
     * 删除设备上报数据信息
     *
     * @param id 设备上报数据主键
     * @return 结果
     */
    public int deleteDeviceDataById(Long id);

    /**
     * 处理AMQP接收到的设备上报消息
     *
     * @param contentStr AMQP消息内容JSON字符串
     */
    public void processDeviceReport(String contentStr);

    /**
     * 查询设备当前最新上报的健康数据（从Redis获取，每个functionId一条）
     *
     * @param iotId 设备iotId
     * @return 健康数据列表
     */
    List<HealthDataVo> queryLatestHealthData(String iotId);

    /**
     * 按天统计：当天 00:00~23:59 共 24 个整点数据
     *
     * @param iotId      设备ID
     * @param functionId 功能ID
     * @param date       日期（yyyy-MM-dd）
     * @return 24个统计点，缺省dataValue=0
     */
    List<TrendDataVo> queryDeviceDataListByDay(String iotId, String functionId, String date);

    /**
     * 按周统计：以指定日期所在自然周（周一到周日）共 7 天数据
     *
     * @param iotId      设备ID
     * @param functionId 功能ID
     * @param date       日期（yyyy-MM-dd），用来定位周
     * @return 7个统计点，缺省dataValue=0
     */
    List<TrendDataVo> queryDeviceDataListByWeek(String iotId, String functionId, String date);
}
