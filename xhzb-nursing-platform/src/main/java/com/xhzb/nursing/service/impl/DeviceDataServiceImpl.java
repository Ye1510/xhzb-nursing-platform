package com.xhzb.nursing.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.vo.HealthDataVo;
import com.xhzb.nursing.domain.vo.TrendDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.DeviceDataMapper;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.service.IDeviceDataService;
import com.xhzb.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 设备上报数据Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Slf4j
@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService
{
    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** Redis Hash大key，存储设备最新上报数据（与讲义一致） */
    private static final String IOT_DEVICE_LATEST_DATA_KEY = CacheConstants.IOT_DEVICE_DATA_LATEST;

    /**
     * 查询设备上报数据
     *
     * @param id 设备上报数据主键
     * @return 设备上报数据
     */
    @Override
    public DeviceData selectDeviceDataById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询设备上报数据列表
     *
     * @param deviceData 设备上报数据
     * @return 设备上报数据
     */
    @Override
    public List<DeviceData> selectDeviceDataList(DeviceData deviceData)
    {
        return deviceDataMapper.selectDeviceDataList(deviceData);
    }

    /**
     * 新增设备上报数据（同时写入MySQL和Redis最新数据缓存）
     *
     * @param deviceData 设备上报数据
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData)
    {
        boolean saved = save(deviceData);
        if (saved && deviceData.getIotId() != null) {
            // 更新Redis：每个functionId保留一条最新
            try {
                refreshRedisLatestData(deviceData);
            } catch (Exception e) {
                log.warn("更新设备数据到Redis失败，iotId={}", deviceData.getIotId(), e);
            }
        }
        return saved ? 1 : 0;
    }

    /**
     * 根据iotId+functionId更新Redis最新数据：functionId冲突取alarmTime更新的
     */
    private void refreshRedisLatestData(DeviceData newData) {
        String iotId = newData.getIotId();
        Object oldJson = stringRedisTemplate.opsForHash().get(IOT_DEVICE_LATEST_DATA_KEY, iotId);
        List<DeviceData> list;
        if (oldJson != null) {
            list = JSONUtil.toList(oldJson.toString(), DeviceData.class);
        } else {
            list = new ArrayList<>();
        }
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            DeviceData old = list.get(i);
            if (old.getFunctionId() != null && old.getFunctionId().equals(newData.getFunctionId())) {
                LocalDateTime oldTime = old.getAlarmTime();
                LocalDateTime newTime = newData.getAlarmTime();
                if (newTime != null && (oldTime == null || !newTime.isBefore(oldTime))) {
                    list.set(i, newData);
                }
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            list.add(newData);
        }
        stringRedisTemplate.opsForHash().put(IOT_DEVICE_LATEST_DATA_KEY, iotId, JSONUtil.toJsonStr(list));
    }

    /**
     * 修改设备上报数据
     *
     * @param deviceData 设备上报数据
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData)
    {
        return updateById(deviceData)? 1 : 0;
    }

    /**
     * 批量删除设备上报数据
     *
     * @param ids 需要删除的设备上报数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除设备上报数据信息
     *
     * @param id 设备上报数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    /**
     * 解析IoT平台事件时间字符串为北京时间
     * <p>
     * 输入格式: {@code yyyyMMdd'T'HHmmss'Z'} (UTC时间)
     * 输出: 北京时间 (UTC+8) LocalDateTime
     */
    private LocalDateTime parseEventTimeToBeijing(String eventTimeStr) {
        if (eventTimeStr == null || eventTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        LocalDateTime utcTime = LocalDateTime.parse(eventTimeStr, formatter);
        return utcTime.plusHours(8);
    }

    /**
     * 处理设备上报数据
     * <p>
     * 解析AMQP消息中的JSON数据，提取device_id、properties、event_time，
     * 查询设备是否存在，若存在则批量保存设备上报的属性数据。
     *
     * @param contentStr AMQP消息内容JSON字符串
     */
    @Override
    public void processDeviceReport(String contentStr) {
        // 1. 解析JSON
        JSONObject contentJson = JSONUtil.parseObj(contentStr);
        JSONObject notifyData = contentJson.getJSONObject("notify_data");
        if (notifyData == null) {
            log.warn("notify_data为空，跳过处理");
            return;
        }

        // 2. 提取device_id
        JSONObject header = notifyData.getJSONObject("header");
        if (header == null) {
            log.warn("header为空，跳过处理");
            return;
        }
        String deviceId = header.getStr("device_id");

        // 3. 提取body中的services
        JSONObject body = notifyData.getJSONObject("body");
        if (body == null) {
            log.warn("body为空，跳过处理");
            return;
        }
        List<JSONObject> services = body.getBeanList("services", JSONObject.class);
        if (services == null || services.isEmpty()) {
            log.warn("services为空，跳过处理");
            return;
        }

        // 4. 根据device_id查询设备，判断是否存在
        Device device = deviceService.getOne(
                Wrappers.<Device>lambdaQuery().eq(Device::getIotId, deviceId));
        if (device == null) {
            log.warn("设备不存在，device_id={}，跳过处理", deviceId);
            return;
        }

        // 5. 遍历services，批量保存properties中的数据
        List<DeviceData> dataList = new ArrayList<>();
        for (JSONObject service : services) {
            String eventTime = service.getStr("event_time");
            LocalDateTime alarmTime = parseEventTimeToBeijing(eventTime);

            JSONObject properties = service.getJSONObject("properties");
            if (properties == null || properties.isEmpty()) {
                log.warn("properties为空，device_id={}", deviceId);
                continue;
            }

            for (String propertyKey : properties.keySet()) {
                Object propertyValue = properties.get(propertyKey);

                DeviceData deviceData = new DeviceData();
                deviceData.setIotId(deviceId);
                deviceData.setDeviceName(device.getDeviceName());
                deviceData.setProductKey(device.getProductKey());
                deviceData.setProductName(device.getProductName());
                deviceData.setFunctionId(propertyKey);
                deviceData.setDataValue(String.valueOf(propertyValue));
                deviceData.setAlarmTime(alarmTime);
                deviceData.setAccessLocation(device.getBindingLocation());
                deviceData.setLocationType(device.getLocationType());
                deviceData.setPhysicalLocationType(device.getPhysicalLocationType());
                deviceData.setDeviceDescription(device.getDeviceDescription());

                dataList.add(deviceData);
            }
        }
        // 6. 批量保存到MySQL
        if (!dataList.isEmpty()) {
            saveBatch(dataList);
            log.info("设备数据上报保存成功，device_id={}，保存条数={}", deviceId, dataList.size());

            // 7. 存入Redis Hash，大key固定，小key为设备id，每次上报覆盖最新（合并已存数据，按functionId保留最新）
            try {
                refreshRedisBatch(deviceId, dataList);
            } catch (Exception e) {
                log.warn("设备数据写入Redis失败，device_id={}", deviceId, e);
            }
            log.info("设备数据已更新到Redis，device_id={}", deviceId);
        }
    }

    /**
     * 批量刷新Redis中某设备最新数据：按functionId去重，相同functionId保留alarmTime更新的数据
     */
    private void refreshRedisBatch(String deviceId, List<DeviceData> latestList) {
        Object oldJson = stringRedisTemplate.opsForHash().get(IOT_DEVICE_LATEST_DATA_KEY, deviceId);
        List<DeviceData> merged;
        if (oldJson != null) {
            merged = new ArrayList<>(JSONUtil.toList(oldJson.toString(), DeviceData.class));
        } else {
            merged = new ArrayList<>();
        }
        // 按 functionId → 下标 建索引
        Map<String, Integer> idxMap = new HashMap<>();
        for (int i = 0; i < merged.size(); i++) {
            DeviceData d = merged.get(i);
            if (d.getFunctionId() != null) {
                idxMap.put(d.getFunctionId(), i);
            }
        }
        for (DeviceData item : latestList) {
            Integer idx = idxMap.get(item.getFunctionId());
            if (idx == null) {
                merged.add(item);
                idxMap.put(item.getFunctionId(), merged.size() - 1);
            } else {
                DeviceData old = merged.get(idx);
                LocalDateTime oldTime = old.getAlarmTime();
                LocalDateTime newTime = item.getAlarmTime();
                if (newTime != null && (oldTime == null || !newTime.isBefore(oldTime))) {
                    merged.set(idx, item);
                }
            }
        }
        stringRedisTemplate.opsForHash().put(IOT_DEVICE_LATEST_DATA_KEY, deviceId, JSONUtil.toJsonStr(merged));
    }

    /**
     * 查询设备最新健康数据（从Redis获取，每个functionId一条）
     */
    @Override
    public List<HealthDataVo> queryLatestHealthData(String iotId) {
        List<HealthDataVo> result = new ArrayList<>();
        Object json = stringRedisTemplate.opsForHash().get(IOT_DEVICE_LATEST_DATA_KEY, iotId);
        if (json == null) {
            return result;
        }
        try {
            List<DeviceData> list = JSONUtil.toList(json.toString(), DeviceData.class);
            for (DeviceData d : list) {
                HealthDataVo vo = new HealthDataVo();
                vo.setFunctionId(d.getFunctionId());
                if (d.getAlarmTime() != null) {
                    vo.setEventTime(d.getAlarmTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                try {
                    vo.setValue(Double.parseDouble(d.getDataValue()));
                } catch (Exception e) {
                    vo.setValue(null);
                }
                result.add(vo);
            }
        } catch (Exception e) {
            log.warn("解析Redis健康数据失败，iotId={}", iotId, e);
        }
        return result;
    }

    /**
     * 按天统计：补齐 00:00~23:00 共24个点
     */
    @Override
    public List<TrendDataVo> queryDeviceDataListByDay(String iotId, String functionId, String dateStr) {
        LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<TrendDataVo> data = deviceDataMapper.queryDeviceDataListByDay(iotId, functionId, start, end);
        Map<String, Double> map = new HashMap<>();
        if (data != null) {
            for (TrendDataVo t : data) {
                if (t.getDateTime() != null) {
                    map.put(t.getDateTime(), t.getDataValue() == null ? 0.0 : t.getDataValue());
                }
            }
        }
        List<TrendDataVo> result = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            TrendDataVo vo = new TrendDataVo();
            String key = String.format("%02d:00", h);
            vo.setDateTime(key);
            vo.setDataValue(map.getOrDefault(key, 0.0));
            result.add(vo);
        }
        return result;
    }

    /**
     * 按周统计：指定日期所在自然周（周一至周日）共7天
     */
    @Override
    public List<TrendDataVo> queryDeviceDataListByWeek(String iotId, String functionId, String dateStr) {
        LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate monday = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = start.plusDays(7);
        List<TrendDataVo> data = deviceDataMapper.queryDeviceDataListByWeek(iotId, functionId, start, end);
        Map<String, Double> map = new HashMap<>();
        if (data != null) {
            for (TrendDataVo t : data) {
                if (t.getDateTime() != null) {
                    map.put(t.getDateTime(), t.getDataValue() == null ? 0.0 : t.getDataValue());
                }
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM.dd");
        List<TrendDataVo> result = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            TrendDataVo vo = new TrendDataVo();
            String key = monday.plusDays(i).format(fmt);
            vo.setDateTime(key);
            vo.setDataValue(map.getOrDefault(key, 0.0));
            result.add(vo);
        }
        return result;
    }
}
