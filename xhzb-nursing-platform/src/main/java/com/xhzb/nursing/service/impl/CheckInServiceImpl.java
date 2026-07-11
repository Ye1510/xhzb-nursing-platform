package com.xhzb.nursing.service.impl;

import java.math.BigDecimal;
import com.xhzb.common.utils.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.nursing.domain.*;
import com.xhzb.nursing.domain.dto.checkin.*;
import com.xhzb.nursing.domain.vo.*;
import com.xhzb.nursing.mapper.CheckInMapper;
import com.xhzb.nursing.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 入住表Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService
{
    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private IElderService elderService;

    @Autowired
    private IBedService bedService;

    @Autowired
    private IContractService contractService;

    @Autowired
    private ICheckInConfigService checkInConfigService;

    @Autowired
    private IHealthAssessmentService healthAssessmentService;

    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    /**
     * 查询入住表
     */
    @Override
    public CheckIn selectCheckInById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询入住表列表
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn)
    {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住表
     */
    @Override
    public int insertCheckIn(CheckIn checkIn)
    {
        return save(checkIn)? 1 : 0;
    }

    /**
     * 修改入住表
     */
    @Override
    public int updateCheckIn(CheckIn checkIn)
    {
        return updateById(checkIn)? 1 : 0;
    }

    /**
     * 批量删除入住表
     */
    @Override
    public int deleteCheckInByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除入住表信息
     */
    @Override
    public int deleteCheckInById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    // ==================== 申请入住核心流程 ====================

    /**
     * 申请入住（完整业务流程）
     * 1) 判断评估是否完成
     * 2) 校验老人是否已入住
     * 3) 更新床位状态为已入住
     * 4) 新增或更新老人信息
     * 5) 新增签约合同（生成合同号、判断合同状态）
     * 6) 新增入住记录（家属信息转JSON保存到Remark）
     * 7) 新增入住配置
     * 8) 修改入住评估表，做老人关联
     * 9) 修改入住评估结果表，修改为已入住
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void applyCheckIn(CheckInApplyDto dto) {
        Long assessmentId = dto.getHealthAssessmentId();
        CheckInElderDto elderDto = dto.getCheckInElderDto();
        CheckInConfigDto configDto = dto.getCheckInConfigDto();
        CheckInContractDto contractDto = dto.getCheckInContractDto();
        List<ElderFamilyDto> familyList = dto.getElderFamilyDtoList();

        // 1. 判断评估是否完成，未完成不能入住
        HealthAssessment assessment = healthAssessmentService.getById(assessmentId);
        if (assessment == null || !Integer.valueOf(1).equals(assessment.getEvaluationProgress())) {
            throw new BaseException("未完成评估不能入住");
        }

        // 2. 校验老人是否已入住，如果已入住抛异常
        Elder existingElder = elderService.lambdaQuery()
                .eq(Elder::getName, elderDto.getName())
                .eq(Elder::getIdCardNo, elderDto.getIdCardNo())
                .one();
        if (existingElder != null && Integer.valueOf(1).equals(existingElder.getStatus())) {
            throw new BaseException("老人已入住");
        }

        // 3. 更新床位状态为已入住
        Bed bed = bedService.getById(configDto.getBedId());
        if (bed == null) {
            throw new BaseException("床位不存在");
        }
        bed.setBedStatus(1); // 已入住
        bedService.updateById(bed);

        // 4. 新增或更新老人信息（老人可能以前入住过，存在则更新）
        Elder elder;
        if (existingElder != null) {
            elder = existingElder;
        } else {
            elder = new Elder();
        }
        elder.setName(elderDto.getName());
        elder.setIdCardNo(elderDto.getIdCardNo());
        elder.setSex(elderDto.getSex());
        elder.setPhone(elderDto.getPhone());
        elder.setBirthday(elderDto.getBirthday());
        elder.setAddress(elderDto.getAddress());
        elder.setImage(elderDto.getImage());
        elder.setIdCardNationalEmblemImg(elderDto.getIdCardNationalEmblemImg());
        elder.setIdCardPortraitImg(elderDto.getIdCardPortraitImg());
        elder.setBedNumber(bed.getBedNumber());
        elder.setBedId(configDto.getBedId().intValue());
        elder.setNation(elderDto.getNation());
        elder.setEducationLevel(elderDto.getEducationLevel());
        elder.setSocialSecurityCard(elderDto.getSocialSecurityCard());
        elder.setLivingSituation(elderDto.getLivingSituation());
        elder.setReligiousBelief(elderDto.getReligiousBelief());
        elder.setEconomicSource(elderDto.getEconomicSource());
        elder.setMaritalStatus(elderDto.getMaritalStatus());
        elder.setMedicalPaymentMethod(elderDto.getMedicalPaymentMethod());
        elder.setCoreSuggestion(elderDto.getCoreSuggestion());
        elder.setStatus(1); // 已入住
        elderService.saveOrUpdate(elder);

        // 5. 新增签约合同
        Contract contract = new Contract();
        contract.setElderId(elder.getId());
        contract.setContractName(contractDto.getContractName());
        // 合同号：HT + yyyyMMddHHmmss + 4位递增值
        contract.setContractNumber(generateContractNumber());
        contract.setAgreementPath(contractDto.getAgreementPath());
        contract.setThirdPartyPhone(contractDto.getThirdPartyPhone());
        contract.setThirdPartyName(contractDto.getThirdPartyName());
        contract.setElderName(elderDto.getName());

        LocalDateTime startDate = parseDateTime(configDto.getStartDate());
        LocalDateTime endDate = parseDateTime(configDto.getEndDate());
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);

        // 判断合同状态：签约日期<=当前时间则为已生效，否则未生效
        LocalDate signDate = parseDate(contractDto.getSignDate());
        contract.setSignDate(signDate.atStartOfDay());
        if (!signDate.isAfter(LocalDate.now())) {
            contract.setStatus(1); // 已生效
        } else {
            contract.setStatus(0); // 未生效
        }
        contractService.save(contract);

        // 6. 新增入住记录，家属信息转成JSON保存到Remark字段
        CheckIn checkIn = new CheckIn();
        checkIn.setElderName(elderDto.getName());
        checkIn.setElderId(elder.getId());
        checkIn.setIdCardNo(elderDto.getIdCardNo());
        checkIn.setStartDate(startDate);
        checkIn.setEndDate(endDate);
        checkIn.setNursingLevelName(configDto.getNursingLevelName());
        checkIn.setBedNumber(bed.getBedNumber());
        checkIn.setStatus(0); // 已入住
        if (familyList != null && !familyList.isEmpty()) {
            checkIn.setRemark(JSONUtil.toJsonStr(familyList));
        }
        save(checkIn);

        // 7. 新增入住配置
        CheckInConfig checkInConfig = new CheckInConfig();
        checkInConfig.setCheckInId(checkIn.getId());
        checkInConfig.setNursingLevelId(configDto.getNursingLevelId());
        checkInConfig.setNursingLevelName(configDto.getNursingLevelName());
        checkInConfig.setFeeStartDate(parseDateTime(configDto.getFeeStartDate()));
        checkInConfig.setFeeEndDate(parseDateTime(configDto.getFeeEndDate()));
        checkInConfig.setDeposit(configDto.getDeposit() != null ? new BigDecimal(configDto.getDeposit()) : BigDecimal.ZERO);
        checkInConfig.setNursingFee(configDto.getNursingFee() != null ? new BigDecimal(configDto.getNursingFee()) : BigDecimal.ZERO);
        checkInConfig.setBedFee(StringUtils.isNotEmpty(configDto.getBedFee()) ? new BigDecimal(configDto.getBedFee()) : BigDecimal.ZERO);
        checkInConfig.setInsurancePayment(configDto.getInsurancePayment() != null ? new BigDecimal(configDto.getInsurancePayment()) : BigDecimal.ZERO);
        checkInConfig.setGovernmentSubsidy(configDto.getGovernmentSubsidy() != null ? new BigDecimal(configDto.getGovernmentSubsidy()) : BigDecimal.ZERO);
        checkInConfig.setOtherFees(configDto.getOtherFees() != null ? new BigDecimal(configDto.getOtherFees()) : BigDecimal.ZERO);
        checkInConfigService.save(checkInConfig);

        // 8. 修改入住评估表，做老人关联
        assessment.setElderId(elder.getId());
        assessment.setCheckInStatus(1); // 已入住
        healthAssessmentService.updateById(assessment);

        // 9. 修改入住评估结果表，修改为已入住
        HealthAssessmentReport report = healthAssessmentReportService.getByHealthAssessmentId(assessmentId);
        if (report != null) {
            report.setCheckInStatus(1); // 已入住
            healthAssessmentReportService.updateById(report);
        }
    }

    // ==================== 查询入住详情 ====================

    /**
     * 查询入住详情（从多个表获取数据组装DTO）
     */
    @Override
    public CheckInDetailVo getCheckInDetail(Long checkInId) {
        CheckInDetailVo detailVo = new CheckInDetailVo();

        CheckIn checkIn = getById(checkInId);
        if (checkIn == null) {
            throw new BaseException("入住记录不存在");
        }

        // 查询老人信息
        Elder elder = elderService.getById(checkIn.getElderId());
        if (elder != null) {
            CheckInElderVo elderVo = new CheckInElderVo();
            elderVo.setId(elder.getId());
            elderVo.setName(elder.getName());
            elderVo.setIdCardNo(elder.getIdCardNo());
            elderVo.setBirthday(elder.getBirthday());
            elderVo.setSex(elder.getSex());
            elderVo.setPhone(elder.getPhone());
            elderVo.setAddress(elder.getAddress());
            elderVo.setImage(elder.getImage());
            elderVo.setIdCardNationalEmblemImg(elder.getIdCardNationalEmblemImg());
            elderVo.setIdCardPortraitImg(elder.getIdCardPortraitImg());
            // 计算年龄
            if (elder.getBirthday() != null && elder.getBirthday().length() >= 4) {
                try {
                    int birthYear = Integer.parseInt(elder.getBirthday().substring(0, 4));
                    elderVo.setAge(LocalDate.now().getYear() - birthYear);
                } catch (Exception e) {
                    elderVo.setAge(null);
                }
            }
            elderVo.setNation(elder.getNation());
            elderVo.setEducationLevel(elder.getEducationLevel());
            elderVo.setSocialSecurityCard(elder.getSocialSecurityCard());
            elderVo.setLivingSituation(elder.getLivingSituation());
            elderVo.setReligiousBelief(elder.getReligiousBelief());
            elderVo.setEconomicSource(elder.getEconomicSource());
            elderVo.setMaritalStatus(elder.getMaritalStatus());
            elderVo.setMedicalPaymentMethod(elder.getMedicalPaymentMethod());
            elderVo.setCoreSuggestion(elder.getCoreSuggestion());
            detailVo.setCheckInElderVo(elderVo);
        }

        // 解析家属信息（从入住表的remark字段JSON中获取）
        if (checkIn.getRemark() != null && !checkIn.getRemark().isBlank()) {
            try {
                List<ElderFamilyDto> familyDtos = JSONUtil.toList(checkIn.getRemark(), ElderFamilyDto.class);
                List<ElderFamilyVo> familyVos = familyDtos.stream().map(f -> {
                    ElderFamilyVo vo = new ElderFamilyVo();
                    vo.setName(f.getName());
                    vo.setPhone(f.getPhone());
                    vo.setKinship(f.getKinship());
                    return vo;
                }).collect(Collectors.toList());
                detailVo.setElderFamilyVoList(familyVos);
            } catch (Exception e) {
                detailVo.setElderFamilyVoList(new ArrayList<>());
            }
        } else {
            detailVo.setElderFamilyVoList(new ArrayList<>());
        }

        // 查询合同信息
        List<Contract> contracts = contractService.lambdaQuery()
                .eq(Contract::getElderId, checkIn.getElderId())
                .orderByDesc(Contract::getCreateTime)
                .list();
        if (contracts != null && !contracts.isEmpty()) {
            Contract con = contracts.get(0);
            CheckInContractVo contractVo = new CheckInContractVo();
            contractVo.setCreateBy(con.getCreateBy());
            contractVo.setId(con.getId());
            contractVo.setElderId(con.getElderId());
            contractVo.setContractName(con.getContractName());
            contractVo.setContractNumber(con.getContractNumber());
            contractVo.setAgreementPath(con.getAgreementPath());
            contractVo.setThirdPartyPhone(con.getThirdPartyPhone());
            contractVo.setThirdPartyName(con.getThirdPartyName());
            contractVo.setElderName(con.getElderName());
            contractVo.setStartDate(con.getStartDate());
            contractVo.setEndDate(con.getEndDate());
            contractVo.setStatus(con.getStatus());
            contractVo.setSignDate(con.getSignDate());
            contractVo.setSortOrder(con.getSortOrder());
            detailVo.setContract(contractVo);
        }

        // 查询入住配置
        List<CheckInConfig> configs = checkInConfigService.lambdaQuery()
                .eq(CheckInConfig::getCheckInId, checkInId)
                .list();
        if (configs != null && !configs.isEmpty()) {
            CheckInConfig cfg = configs.get(0);
            CheckInConfigVo configVo = new CheckInConfigVo();
            configVo.setCreateBy(cfg.getCreateBy());
            configVo.setId(cfg.getId());
            configVo.setCheckInId(cfg.getCheckInId());
            configVo.setNursingLevelId(cfg.getNursingLevelId());
            configVo.setNursingLevelName(cfg.getNursingLevelName());
            configVo.setFeeStartDate(cfg.getFeeStartDate());
            configVo.setFeeEndDate(cfg.getFeeEndDate());
            configVo.setDeposit(cfg.getDeposit());
            configVo.setNursingFee(cfg.getNursingFee());
            configVo.setBedFee(cfg.getBedFee());
            configVo.setInsurancePayment(cfg.getInsurancePayment());
            configVo.setGovernmentSubsidy(cfg.getGovernmentSubsidy());
            configVo.setOtherFees(cfg.getOtherFees());
            configVo.setSortOrder(cfg.getSortOrder());
            configVo.setStartDate(checkIn.getStartDate());
            configVo.setEndDate(checkIn.getEndDate());
            configVo.setBedNumber(checkIn.getBedNumber());
            detailVo.setCheckInConfigVo(configVo);
        }

        return detailVo;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成合同编号：HT + yyyyMMddHHmmss + 4位递增值
     */
    private String generateContractNumber() {
        String prefix = "HT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 查询当天已有合同数，生成4位递增序号
        long count = contractService.lambdaQuery()
                .likeRight(Contract::getContractNumber,
                        "HT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .count();
        return prefix + String.format("%04d", count + 1);
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            }
        } catch (Exception e) {
            return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        }
    }

    /**
     * 解析日期字符串
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            if (dateStr.contains(" ")) {
                return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
