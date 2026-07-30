package com.xhzb.nursing.service;

import java.util.List;

import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.nursing.domain.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.vo.DeviceDetailVo;
import com.xhzb.nursing.domain.vo.DevicePropertyVo;
import com.xhzb.nursing.domain.vo.ProductVo;

/**
 * 设备管理Service接口
 *
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IDeviceService extends IService<Device>
{

    /**
     * 查询设备管理列表
     *
     * @param device 设备管理
     * @return 设备管理集合
     */
    public List<Device> selectDeviceList(Device device);


    void syncProductList();

    List<ProductVo> allProduct();

    /**
     * 注册设备
     *
     * @param device 设备信息
     * @return 注册成功的设备
     */
    Device registerDevice(Device device);

    /**
     * 查询设备详细信息
     *
     * @param iotId 物联网设备ID
     * @return 设备详细信息
     */
    DeviceDetailVo getDeviceDetail(String iotId);

    /**
     * 查询设备上报的属性数据
     *
     * @param iotId 物联网设备ID
     * @return 设备上报的属性数据列表
     */
    List<DevicePropertyVo> queryServiceProperties(String iotId);

    AjaxResult queryProduct(String productKey);

    /**
     * 修改设备
     * 先修改华为云IoT平台，再修改本地数据库
     * 修改后不能在同一个位置绑定同一个产品
     *
     * @param device 设备信息
     * @return 修改成功的设备
     */
    Device updateDeviceInfo(Device device);

    /**
     * 删除设备
     * 先删除华为云IoT平台设备，再删除本地数据库
     *
     * @param ids 设备主键集合
     */
    void deleteDeviceInfo(Long[] ids);
}
