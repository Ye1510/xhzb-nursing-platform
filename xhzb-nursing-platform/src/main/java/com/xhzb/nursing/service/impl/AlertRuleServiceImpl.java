package com.xhzb.nursing.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.AlertRuleMapper;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.nursing.service.IAlertRuleService;
import com.xhzb.nursing.service.IElderService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 报警规则Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Slf4j
@Service
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService
{
    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IAlertDataService alertDataService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private ElderMapper elderMapper;

    @Autowired
    private IElderService elderService;

    /** Redis Hash大key，存储设备最新上报数据 */
    private static final String IOT_DEVICE_LATEST_DATA_KEY = CacheConstants.IOT_DEVICE_DATA_LATEST;

    /** 报警计数Redis key前缀 */
    private static final String IOT_ALERT_COUNT_PREFIX = "iot:alert_count:";

    /** 沉默周期Redis key前缀 */
    private static final String IOT_ALERT_SILENT_PREFIX = "iot:alert_silent:";

    /**
     * 查询报警规则
     *
     * @param id 报警规则主键
     * @return 报警规则
     */
    @Override
    public AlertRule selectAlertRuleById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询报警规则列表
     *
     * @param alertRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule)
    {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    /**
     * 新增报警规则
     *
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int insertAlertRule(AlertRule alertRule)
    {
        return save(alertRule)? 1 : 0;
    }

    /**
     * 修改报警规则
     *
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int updateAlertRule(AlertRule alertRule)
    {
        return updateById(alertRule)? 1 : 0;
    }

    /**
     * 批量删除报警规则
     *
     * @param ids 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除报警规则信息
     *
     * @param id 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    /**
     * 设备数据报警过滤定时任务
     * <p>
     * 整体流程：
     * 1. 查询所有启用的报警规则
     * 2. 从Redis Hash读取所有设备最新上报数据
     * 3. 遍历每条设备数据，匹配对应报警规则
     * 4. 校验阈值、持续周期、沉默周期，触发报警
     */
    @Override
    public void alertFilter() {
        // ========== 一、查询所有正在生效的报警规则 ==========
        List<AlertRule> activeRules = list(Wrappers.<AlertRule>lambdaQuery().eq(AlertRule::getStatus, 1));
        if (activeRules.isEmpty()) {
            log.debug("无启用的报警规则，跳过报警过滤");
            return;
        }
        log.info("查询到{}条启用的报警规则", activeRules.size());

        // ========== 二、查询Redis中设备最新上报数据 ==========
        Map<Object, Object> deviceDataMap = stringRedisTemplate.opsForHash().entries(IOT_DEVICE_LATEST_DATA_KEY);
        // 2.1 如果为空直接结束
        if (deviceDataMap.isEmpty()) {
            log.debug("Redis中无设备上报数据，跳过报警过滤");
            return;
        }

        // 2.2 把Hash结构中的value值收集到一个大List集合中
        List<DeviceData> allDeviceData = new ArrayList<>();
        for (Object value : deviceDataMap.values()) {
            String jsonStr = (String) value;
            List<DeviceData> dataList = JSONUtil.toList(jsonStr, DeviceData.class);
            allDeviceData.addAll(dataList);
        }
        log.info("从Redis获取到{}条设备最新上报数据", allDeviceData.size());

        // ========== 三、遍历处理每条设备上报数据 ==========
        for (DeviceData deviceData : allDeviceData) {
            // 3.1 判断设备上报数据是否超过1分钟，超过则跳过
            // if (deviceData.getAlarmTime() != null
            //         && deviceData.getAlarmTime().plusMinutes(1).isBefore(LocalDateTime.now())) {
            //     continue;
            // }

            // 3.2 查询该产品下物模型下所有规则 + 该产品物模型下对应设备的规则
            String productKey = deviceData.getProductKey();
            String functionId = deviceData.getFunctionId();
            String iotId = deviceData.getIotId();
            // 关键字段为空的数据（如手动构造的测试数据）无法匹配规则，直接跳过，避免NPE
            if (productKey == null || functionId == null || iotId == null) {
                log.warn("设备数据缺少关键字段（productKey/functionId/iotId），跳过报警过滤，iotId={}", iotId);
                continue;
            }

            List<AlertRule> matchingRules = activeRules.stream()
                    .filter(r -> Objects.equals(productKey, r.getProductKey())
                            && Objects.equals(functionId, r.getFunctionId())
                            && (Objects.equals(r.getIotId(), "-1") || Objects.equals(iotId, r.getIotId())))
                    .collect(Collectors.toList());

            // 3.3 如果没有规则，跳过
            if (matchingRules.isEmpty()) {
                continue;
            }

            // 3.4 开始数据校验 — 处理每条匹配的报警规则
            processRulesForDeviceData(deviceData, matchingRules);
        }
    }

    /**
     * 处理单条设备数据对应的多条报警规则
     *
     * @param deviceData 设备上报数据
     * @param rules      匹配的报警规则列表
     */
    private void processRulesForDeviceData(DeviceData deviceData, List<AlertRule> rules) {
        for (AlertRule rule : rules) {
            try {
                processSingleRule(deviceData, rule);
            } catch (Exception e) {
                log.error("处理报警规则异常，rule_id={}，device_id={}，function_id={}",
                        rule.getId(), deviceData.getIotId(), deviceData.getFunctionId(), e);
            }
        }
    }

    /**
     * 处理单条报警规则
     */
    private void processSingleRule(DeviceData deviceData, AlertRule rule) {
        String iotId = deviceData.getIotId();
        String functionId = deviceData.getFunctionId();

        // 校验报警生效时段
        if (!isInEffectivePeriod(rule.getAlertEffectivePeriod())) {
            log.debug("当前时间不在报警生效时段内，rule_id={}，period={}", rule.getId(), rule.getAlertEffectivePeriod());
            return;
        }

        // 解析数据值
        double dataValue;
        try {
            dataValue = Double.parseDouble(deviceData.getDataValue());
        } catch (NumberFormatException e) {
            log.warn("设备数据值无法解析为数字，iot_id={}，function_id={}，data_value={}",
                    iotId, functionId, deviceData.getDataValue());
            return;
        }

        String countKey = IOT_ALERT_COUNT_PREFIX + rule.getId() + ":" + iotId + ":" + functionId;
        String silentKey = IOT_ALERT_SILENT_PREFIX + rule.getId() + ":" + iotId + ":" + functionId;

        // ========== 4.1 校验数据是否达到报警规则的阈值 ==========
        boolean thresholdReached = checkThreshold(dataValue, rule.getOperator(), rule.getValue());
        if (!thresholdReached) {
            // 未达阈值，删除Redis中报警次数
            stringRedisTemplate.delete(countKey);
            return;
        }

        // ========== 4.2 判断是否在沉默周期内 ==========
        String silentValue = stringRedisTemplate.opsForValue().get(silentKey);
        if (silentValue != null) {
            log.debug("设备处于沉默周期中，rule_id={}，iot_id={}", rule.getId(), iotId);
            return;
        }

        // ========== 4.3 累加报警次数，比较持续周期 ==========
        int duration = rule.getDuration() != null && rule.getDuration() > 0 ? rule.getDuration() : 1;
        Long alertCount = stringRedisTemplate.opsForValue().increment(countKey);
        // 设置计数key过期时间为2分钟，防止僵尸key堆积
        stringRedisTemplate.expire(countKey, Duration.ofMinutes(2));

        if (alertCount.intValue() != duration) {
            log.debug("报警次数未达持续周期，rule_id={}，当前次数={}，需要次数={}",
                    rule.getId(), alertCount, duration);
            return;
        }

        // ========== 4.4 达到报警条件：删除计数、添加沉默周期、保存报警数据 ==========
        stringRedisTemplate.delete(countKey);

        // 添加沉默周期，过期时间为规则配置的分钟数
        int silentPeriod = rule.getAlertSilentPeriod() != null && rule.getAlertSilentPeriod() > 0
                ? rule.getAlertSilentPeriod() : 5;
        stringRedisTemplate.opsForValue().set(silentKey, "1", Duration.ofMinutes(silentPeriod));

        // 构建报警原因
        String alertReason = buildAlertReason(rule, duration);

        // ========== 保存报警数据：解析通知人员，批量插入 ==========
        List<Long> recipientIds = resolveAlertRecipients(deviceData, rule);
        if (recipientIds.isEmpty()) {
            log.warn("报警规则未找到通知人员，rule_id={}，alert_data_type={}",
                    rule.getId(), rule.getAlertDataType());
            return;
        }

        List<AlertData> alertDataList = new ArrayList<>();
        for (Long userId : recipientIds) {
            AlertData alertData = new AlertData();
            alertData.setIotId(iotId);
            alertData.setDeviceName(deviceData.getDeviceName());
            alertData.setProductKey(deviceData.getProductKey());
            alertData.setProductName(deviceData.getProductName());
            alertData.setFunctionId(functionId);
            alertData.setAccessLocation(deviceData.getAccessLocation());
            alertData.setLocationType(deviceData.getLocationType());
            alertData.setPhysicalLocationType(deviceData.getPhysicalLocationType());
            alertData.setDeviceDescription(deviceData.getDeviceDescription());
            alertData.setDataValue(deviceData.getDataValue());
            alertData.setAlertRuleId(rule.getId());
            alertData.setAlertReason(alertReason);
            alertData.setType(rule.getAlertDataType() != null ? rule.getAlertDataType() : 0);
            alertData.setStatus(0); // 待处理
            alertData.setUserId(userId);
            alertDataList.add(alertData);
        }

        alertDataService.saveBatch(alertDataList);
        log.info("报警数据已批量保存{}条，rule_id={}，iot_id={}，function_id={}，data_value={}，reason={}，recipient_count={}",
                alertDataList.size(), rule.getId(), iotId, functionId, deviceData.getDataValue(), alertReason, recipientIds.size());
    }



    /**
     * 校验数值是否达到阈值
     *
     * @param dataValue 设备上报数据值
     * @param operator  运算符（<、<=、>、>=、==、!=）
     * @param threshold 阈值
     * @return true-达到阈值
     */
    private boolean checkThreshold(double dataValue, String operator, Double threshold) {
        if (operator == null || threshold == null) {
            return false;
        }
        switch (operator.trim()) {
            case "<":
                return dataValue < threshold;
            case "<=":
                return dataValue <= threshold;
            case ">":
                return dataValue > threshold;
            case ">=":
                return dataValue >= threshold;
            case "==":
                return dataValue == threshold;
            case "!=":
                return dataValue != threshold;
            default:
                log.warn("不支持的运算符: {}", operator);
                return false;
        }
    }

    /**
     * 判断当前时间是否在报警生效时段内
     * <p>
     * 格式: "HH:mm:ss~HH:mm:ss"，如 "00:00:00~23:59:59"
     *
     * @param effectivePeriod 报警生效时段字符串
     * @return true-在生效时段内
     */
    private boolean isInEffectivePeriod(String effectivePeriod) {
        if (effectivePeriod == null || effectivePeriod.isEmpty()) {
            return true; // 未配置则默认全天生效
        }
        try {
            String[] parts = effectivePeriod.split("~");
            if (parts.length != 2) {
                return true;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalTime startTime = LocalTime.parse(parts[0].trim(), formatter);
            LocalTime endTime = LocalTime.parse(parts[1].trim(), formatter);
            LocalTime now = LocalTime.now();

            if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
                // 正常时段，如 08:00~20:00
                return !now.isBefore(startTime) && !now.isAfter(endTime);
            } else {
                // 跨天时段，如 22:00~06:00
                return !now.isBefore(startTime) || !now.isAfter(endTime);
            }
        } catch (Exception e) {
            log.warn("解析报警生效时段失败: {}", effectivePeriod, e);
            return true; // 解析失败默认全天生效
        }
    }

    /**
     * 构建报警原因描述
     *
     * @param rule     报警规则
     * @param duration 持续周期
     * @return 报警原因字符串
     */
    private String buildAlertReason(AlertRule rule, int duration) {
        StringBuilder sb = new StringBuilder();
        if (rule.getFunctionName() != null) {
            sb.append(rule.getFunctionName());
        } else {
            sb.append(rule.getFunctionId());
        }
        sb.append(rule.getOperator());
        sb.append(rule.getValue());
        sb.append(" 持续");
        sb.append(duration);
        sb.append("个周期");
        return sb.toString();
    }

    /**
     * 解析报警通知人员ID集合
     * <p>
     * 根据报警数据类型和设备类型，按以下流程筛选通知人员：
     * 1. 设备异常数据(alertDataType=1)：查询"行政"角色人员 + "超级管理员"
     * 2. 老人异常数据(alertDataType=0)：
     *    - 随身设备(locationType=0)：accessLocation=老人ID → 护理员
     *    - 固定设备(physicalLocationType=2)：accessLocation=床位ID → 老人 → 护理员
     *    + "超级管理员"
     *
     * @param deviceData 设备上报数据
     * @param rule       报警规则
     * @return 去重后的通知人员ID列表
     */
    private List<Long> resolveAlertRecipients(DeviceData deviceData, AlertRule rule) {
        Set<Long> userIds = new LinkedHashSet<>();

        Integer alertDataType = rule.getAlertDataType();
        Integer locationType = deviceData.getLocationType();
        Integer physicalLocationType = deviceData.getPhysicalLocationType();
        String accessLocation = deviceData.getAccessLocation();

        if (alertDataType == null || alertDataType == 1) {
            // 设备异常数据：设备不关联老人，查询"行政"角色人员
            userIds.addAll(queryUserIdsByRoleName("行政"));
        } else {
            // 老人异常数据(alertDataType=0)：设备关联老人
            Long elderId = null;

            if (locationType != null && locationType == 0) {
                // 随身设备：accessLocation的值就是老人ID
                if (accessLocation != null && !accessLocation.isEmpty()) {
                    try {
                        elderId = Long.valueOf(accessLocation);
                    } catch (NumberFormatException e) {
                        log.warn("随身设备accessLocation解析失败，iot_id={}，access_location={}",
                                deviceData.getIotId(), accessLocation);
                    }
                }
            } else {
                // 固定设备：physicalLocationType=2(床位)时，accessLocation=床位ID
                if (physicalLocationType != null && physicalLocationType == 2
                        && accessLocation != null && !accessLocation.isEmpty()) {
                    try {
                        Long bedId = Long.valueOf(accessLocation);
                        elderId = queryElderIdByBedId(bedId);
                    } catch (NumberFormatException e) {
                        log.warn("固定设备accessLocation解析失败，iot_id={}，access_location={}",
                                deviceData.getIotId(), accessLocation);
                    }
                }
            }

            // 根据老人ID查询护理员
            if (elderId != null) {
                List<Long> nursingIds = elderMapper.selectNursingIdsByElderId(elderId);
                userIds.addAll(nursingIds);
                log.debug("老人ID={}关联护理员{}人", elderId, nursingIds.size());
            }
        }

        // 所有场景：查询"超级管理员"角色人员
        userIds.addAll(queryUserIdsByRoleName("超级管理员"));

        log.debug("报警通知人员解析完成，rule_id={}，alert_data_type={}，recipient_ids={}",
                rule.getId(), alertDataType, userIds);
        return new ArrayList<>(userIds);
    }

    /**
     * 根据角色名称查询关联的用户ID列表
     *
     * @param roleName 角色名称
     * @return 用户ID列表
     */
    private List<Long> queryUserIdsByRoleName(String roleName) {
        return sysUserRoleMapper.selectUserIdsByRoleName(roleName);
    }

    /**
     * 根据床位ID查询关联的老人ID
     *
     * @param bedId 床位ID
     * @return 老人ID，未找到返回null
     */
    private Long queryElderIdByBedId(Long bedId) {
        Elder elder = elderService.getOne(
                Wrappers.<Elder>lambdaQuery().eq(Elder::getBedId, bedId));
        if (elder != null) {
            return elder.getId();
        }
        log.warn("未找到床位{}关联的老人", bedId);
        return null;
    }
}
