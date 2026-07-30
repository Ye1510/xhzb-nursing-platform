package com.xhzb.nursing.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.StringUtils;
import com.xhzb.nursing.domain.vo.DeviceDetailVo;
import com.xhzb.nursing.domain.vo.DevicePropertyVo;
import com.xhzb.nursing.domain.vo.ProductVo;
import com.xhzb.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.DeviceMapper;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService
{


    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private IoTDAClient client;

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 同步产品列表
     */
    @Override
    public void syncProductList() {
        //请求参数
        ListProductsRequest listProductsRequest = new ListProductsRequest();
        //设置条数
        listProductsRequest.setLimit(50);
        //发起请求
        ListProductsResponse response = client.listProducts(listProductsRequest);
        if(response.getHttpStatusCode() != 200){
            throw new BaseException("物联网接口 - 查询产品，同步失败");
        }
        //存储到redis
        redisTemplate.opsForValue().set(CacheConstants.IOT_ALL_PRODUCT_LIST, JSONUtil.toJsonStr(response.getProducts()));

    }

    @Override
    public List<ProductVo> allProduct() {
        //从redis中查询数据
        String jsonStr = redisTemplate.opsForValue().get(CacheConstants.IOT_ALL_PRODUCT_LIST);
        //如果数据为空，则返回一个空集合
        if(StringUtils.isEmpty(jsonStr)){
            return Collections.emptyList();
        }
        //解析数据，并返回
        return JSONUtil.toList(jsonStr, ProductVo.class);
    }

    /**
     * 查询设备管理列表
     *
     * @param device 设备管理
     * @return 设备管理
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 注册设备
     *
     * @param device 设备信息
     * @return 注册成功的设备
     */
    @Override
    public Device registerDevice(Device device) {
        // 1. 校验设备名称是否重复
        checkDeviceNameUnique(device.getDeviceName());

        // 2. 校验设备标识符(nodeId)是否重复
        checkNodeIdUnique(device.getNodeId());

        // 3. 校验同一个位置是否绑定了相同的产品
        checkLocationProductUnique(device.getBindingLocation(), device.getLocationType(),
                device.getPhysicalLocationType(), device.getProductKey());

        // 4. 调用华为云IoT平台注册设备
        AddDeviceRequest request = new AddDeviceRequest();
        AddDevice addDevice = new AddDevice();
        addDevice.setDeviceName(device.getDeviceName());
        addDevice.setProductId(device.getProductKey());
        addDevice.setNodeId(device.getNodeId());
        addDevice.setDescription(device.getDeviceDescription());

        // 设置认证信息：密钥认证，由平台自动生成密钥
        AuthInfo authInfo = new AuthInfo();
        authInfo.setAuthType("SECRET");
        authInfo.setSecureAccess(true);
        addDevice.setAuthInfo(authInfo);

        request.setBody(addDevice);

        AddDeviceResponse response;
        try {
            response = client.addDevice(request);
        } catch (ServiceResponseException e) {
            throw new BaseException("华为云IoT注册设备失败：" + e.getErrorMsg());
        }

        // 5. 将注册结果写入本地设备表
        device.setIotId(response.getDeviceId());
        if (response.getAuthInfo() != null) {
            device.setSecret(response.getAuthInfo().getSecret());
        }
        deviceMapper.insert(device);

        return device;
    }

    /**
     * 查询设备详细信息
     * 先从本地数据库查询，再调用华为云IoT接口补充状态和激活时间，最后合并返回
     *
     * @param iotId 物联网设备ID
     * @return 设备详细信息
     */
    @Override
    public DeviceDetailVo getDeviceDetail(String iotId) {
        // 1. 从MySQL查询设备本地数据
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getIotId, iotId);
        Device device = deviceMapper.selectOne(queryWrapper);
        if (device == null) {
            throw new ServiceException("设备不存在，iotId：" + iotId);
        }

        // 2. 调用华为云IoT查询设备接口，获取设备状态和激活时间
        ShowDeviceRequest request = new ShowDeviceRequest();
        request.setDeviceId(iotId);

        ShowDeviceResponse response;
        try {
            response = client.showDevice(request);
        } catch (ServiceResponseException e) {
            throw new BaseException("华为云IoT查询设备失败：" + e.getErrorMsg());
        }

        // 3. 组装返回数据
        DeviceDetailVo vo = new DeviceDetailVo();
        vo.setId(device.getId());
        vo.setIotId(device.getIotId());
        vo.setDeviceName(device.getDeviceName());
        vo.setNodeId(device.getNodeId());
        vo.setSecret(device.getSecret());
        vo.setProductKey(device.getProductKey());
        vo.setProductName(device.getProductName());
        vo.setLocationType(device.getLocationType());
        vo.setBindingLocation(device.getBindingLocation());
        vo.setRemark(device.getRemark());
        vo.setCreateTime(LocalDateTimeUtil.of(device.getCreateTime()));

        // 设置华为云IoT返回的设备状态
        vo.setDeviceStatus(response.getStatus());
        // 设置设备激活时间（华为云返回ISO 8601格式，如"2019-03-03T08:10:11.122Z"）
        if (response.getActiveTime() != null) {
            String activeTimeStr = response.getActiveTime();
            // 处理毫秒精度差异：可能是 .122Z 或 .12Z 或 Z
            try {
                java.time.Instant instant = java.time.Instant.parse(activeTimeStr);
                vo.setActiveTime(java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
            } catch (Exception e) {
                // 解析失败则忽略，activeTime保持为null
            }
        }


        return vo;
    }

    /**
     * 查询设备上报的属性数据
     * 调用华为云IoT设备影子接口，获取设备上报的最新属性数据，
     * 并将UTC时间转换为北京时间
     *
     * @param iotId 物联网设备ID
     * @return 设备上报的属性数据列表
     */
    @Override
    public List<DevicePropertyVo> queryServiceProperties(String iotId) {
        // 调用华为云IoT查询设备影子接口
        ShowDeviceShadowRequest request = new ShowDeviceShadowRequest();
        request.setDeviceId(iotId);

        ShowDeviceShadowResponse response;
        try {
            response = client.showDeviceShadow(request);
        } catch (ServiceResponseException e) {
            throw new BaseException("华为云IoT查询设备影子数据失败：" + e.getErrorMsg());
        }

        if (response.getShadow() == null || response.getShadow().isEmpty()) {
            return Collections.emptyList();
        }

        // 构建北京时间格式化器
        java.time.format.DateTimeFormatter sourceFormatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        java.time.format.DateTimeFormatter targetFormatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<DevicePropertyVo> resultList = new java.util.ArrayList<>();

        // 遍历设备影子数据，提取reported区的属性
        for (var shadowData : response.getShadow()) {
            // 优先取reported区（设备上报的数据），没有则取desired区
            var reportedProps = shadowData.getReported();
            if (reportedProps == null || reportedProps.getProperties() == null) {
                continue;
            }

            // 将properties对象转为Map
            String propertiesJson = JSONUtil.toJsonStr(reportedProps.getProperties());
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(propertiesJson);

            // 转换event_time：UTC → 北京时间
            String eventTimeBeijing = null;
            if (reportedProps.getEventTime() != null) {
                try {
                    java.time.LocalDateTime utcTime = java.time.LocalDateTime.parse(
                            reportedProps.getEventTime(), sourceFormatter);
                    java.time.ZonedDateTime beijingTime = utcTime
                            .atZone(java.time.ZoneOffset.UTC)
                            .withZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"));
                    eventTimeBeijing = beijingTime.format(targetFormatter);
                } catch (Exception e) {
                    // 解析失败则使用原始值
                    eventTimeBeijing = reportedProps.getEventTime();
                }
            }

            // 将每个property展开为一条记录
            for (String key : jsonObject.keySet()) {
                DevicePropertyVo propertyVo = new DevicePropertyVo();
                propertyVo.setFunctionId(key);
                propertyVo.setEventTime(eventTimeBeijing);
                propertyVo.setValue(jsonObject.get(key));
                resultList.add(propertyVo);
            }
        }

        return resultList;
    }

    /**
     * 查询产品详情
     * @param productKey
     * @return
     */
    @Override
    public AjaxResult queryProduct(String productKey) {
        //参数校验
        if(StringUtils.isEmpty(productKey)){
            throw new BaseException("请输入正确的参数");
        }
        //调用华为云物联网接口
        ShowProductRequest showProductRequest = new ShowProductRequest();
        showProductRequest.setProductId(productKey);
        ShowProductResponse response;

        try {
            response = client.showProduct(showProductRequest);
        } catch (Exception e) {
            throw new BaseException("查询产品详情失败");
        }
        //判断是否存在服务数据
        List<ServiceCapability> serviceCapabilities = response.getServiceCapabilities();
        if(CollUtil.isEmpty(serviceCapabilities)){
            return AjaxResult.success(Collections.emptyList());
        }

        return AjaxResult.success(serviceCapabilities);
    }

    /**
     * 校验设备名称是否唯一
     */
    private void checkDeviceNameUnique(String deviceName) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceName, deviceName);
        Long count = deviceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("设备名称已存在：" + deviceName);
        }
    }

    /**
     * 校验设备标识符(nodeId)是否唯一
     */
    private void checkNodeIdUnique(String nodeId) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getNodeId, nodeId);
        Long count = deviceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("设备标识符已存在：" + nodeId);
        }
    }

    /**
     * 校验同一个位置是否绑定了相同的产品
     */
    private void checkLocationProductUnique(String bindingLocation, Integer locationType,
                                            Integer physicalLocationType, String productKey) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getBindingLocation, bindingLocation)
                .eq(Device::getLocationType, locationType)
                .eq(Device::getPhysicalLocationType, physicalLocationType)
                .eq(Device::getProductKey, productKey);
        Long count = deviceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("该位置已绑定相同的产品，不能重复绑定");
        }
    }

    /**
     * 校验同一个位置是否绑定了相同的产品（排除当前设备自身）
     */
    private void checkLocationProductUniqueExcludeSelf(Device device) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getBindingLocation, device.getBindingLocation())
                .eq(Device::getLocationType, device.getLocationType())
                .eq(Device::getPhysicalLocationType, device.getPhysicalLocationType())
                .eq(Device::getProductKey, device.getProductKey())
                .ne(Device::getId, device.getId());
        Long count = deviceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("该位置已绑定相同的产品，不能重复绑定");
        }
    }

    /**
     * 修改设备
     * 先修改华为云IoT平台，再修改本地数据库
     * 修改后不能在同一个位置绑定同一个产品
     *
     * @param device 设备信息
     * @return 修改成功的设备
     */
    @Override
    public Device updateDeviceInfo(Device device) {
        // 1. 查询本地设备数据，获取物联网设备ID
        Device localDevice = deviceMapper.selectDeviceById(device.getId());
        if (localDevice == null) {
            throw new ServiceException("设备不存在，id：" + device.getId());
        }

        // 2. 校验设备名称是否重复（排除自身）
        checkDeviceNameUniqueExcludeSelf(device.getDeviceName(), device.getId());

        // 3. 校验修改后不能在同一个位置绑定同一个产品（排除自身）
        checkLocationProductUniqueExcludeSelf(device);

        // 4. 调用华为云IoT平台修改设备（修改设备名称和描述）
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setDeviceId(localDevice.getIotId());
        UpdateDevice updateDevice = new UpdateDevice();
        updateDevice.setDeviceName(device.getDeviceName());
        updateDevice.setDescription(device.getDeviceDescription());
        request.setBody(updateDevice);
        try {
            client.updateDevice(request);
        } catch (ServiceResponseException e) {
            throw new BaseException("华为云IoT修改设备失败：" + e.getErrorMsg());
        }

        // 5. 更新本地数据库
        device.setUpdateTime(new Date());
        deviceMapper.updateDevice(device);

        return device;
    }

    /**
     * 删除设备
     * 先删除华为云IoT平台设备，再删除本地数据库
     *
     * @param ids 设备主键集合
     */
    @Override
    public void deleteDeviceInfo(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        for (Long id : ids) {
            // 1. 查询本地设备数据，获取物联网设备ID
            Device localDevice = deviceMapper.selectDeviceById(id);
            if (localDevice == null) {
                throw new ServiceException("设备不存在，id：" + id);
            }

            // 2. 调用华为云IoT平台删除设备
            DeleteDeviceRequest request = new DeleteDeviceRequest();
            request.setDeviceId(localDevice.getIotId());
            try {
                client.deleteDevice(request);
            } catch (ServiceResponseException e) {
                throw new BaseException("华为云IoT删除设备失败：" + e.getErrorMsg());
            }

            // 3. 删除本地数据库
            deviceMapper.deleteDeviceById(id);
        }
    }

    /**
     * 校验设备名称是否唯一（排除自身）
     */
    private void checkDeviceNameUniqueExcludeSelf(String deviceName, Long id) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceName, deviceName)
                .ne(Device::getId, id);
        Long count = deviceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("设备名称已存在：" + deviceName);
        }
    }

}
