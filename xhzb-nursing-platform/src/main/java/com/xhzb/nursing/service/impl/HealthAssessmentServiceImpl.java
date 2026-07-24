package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.*;
import com.xhzb.nursing.domain.vo.ElderInfoVo;
import com.xhzb.nursing.mapper.HealthAssessmentReportMapper;
import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 健康评估记录Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService
{
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;

    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;

    @Autowired
    @Qualifier("chatClientByAssessment")
    private ChatClient chatClientByAssessment;

    // ==================== 第一阶段AI分析：老人能力评估Prompt模板 ====================

    private static final String ELDER_ABILITY_PROMPT_TEMPLATE = """
            ## 老人评估的信息：
            - 日常生活活动分级：%s
            - 精神状态分级：%s
            - 感知觉与沟通分级：%s
            - 社会参与分级：%s
            - 跌倒次数：%d
            - 噎食次数：%d
            - 自杀次数：%d
            - 走失次数：%d
            - 昏迷次数：%d
            - 痴呆疾病：%s
            - 精神疾病：%s
            - 是否确诊为认知障碍：%d

            ## 评估规则1：
            - 能力完好：
                日常生活活动、精神状态、感知觉与沟通分级均为0，社会参与分级为0或1
            - 轻度失能：
                日常生活活动分级为0，但精神状态、感知觉与沟通中至少一项分级为1及以上，或社会参与的分级为2；
                或日常生活活动分级为1，精神状态、感知觉与沟通、社会参与中至少有一项的分级为0或1
            - 中度失能：
                日常生活活动分级为1，但精神状态、感知觉与沟通、社会参与均为2，或有一项为3；
                或日常生活活动分级为2，且精神状态、感知觉与沟通、社会参与中有1-2项的分级为1或2
            - 重度失能：
                日常生活活动的分级为3；
                或日常生活活动、精神状态、感知觉与沟通、社会参与分级均为2；
                或日常生活活动分级为2，且精神状态、感知觉与沟通、社会参与中至少有一项分级为3

            ## 评估原则2：
            1.有认知障碍/痴呆、精神疾病者，在原有能力级别上提高一个等级；
            2.近30天内发生过2次及以上跌倒、噎食、自杀、走失者，在原有能力级别上提高一个等级；
            3.处于昏迷状态者，直接评定为重度失能；
            4.若初步等级确定为"3重度失能"，则不考虑上述1-3中各情况对最终等级的影响，等级不再提高

            ## 匹配规则
            1. 请根据老人的评估信息与规则1逐条进行比对，判断老人属于哪一种能力
            2. 然后拿老人的评估信息逐条与规则2进行比对，再次判断老人属于哪一种能力。
            3. 如果两次评级不一样，升级的理由是什么，理由只需要填写评估原则2的一条或多条内容，把内容输出到reason中，不需要说明分析理由
            4. 结合评估的原则，给出老人的两次评级，不需要输出分析过程，只需要输出json格式，不要出现markdown语法
            格式为：
            {
                "preLevel": "能力完好|轻度失能|中度失能|重度失能",
                "finalLevel": "能力完好|轻度失能|中度失能|重度失能",
                "reason":"评估原则2中一条或多条"
            }
            """;

    // ==================== 第二阶段AI分析：体检报告评估Prompt模板 ====================

    private static final String HEALTH_ASSESSMENT_PROMPT_TEMPLATE = """
            请以一个专业医生的视角来分析这份体检报告，报告中包含了一些异常数据，我需要您对这些数据进行解读，并给出相应的健康建议。
            体检内容如下：

            %s

            要求：
            1. 提取体检报告中的"总检日期"；
            2. 通过临床医学、疾病风险评估模型和数据智能分析，给该用户的风险等级和健康指数给出结果。风险等级分为：健康、提示、风险、危险、严重危险。健康指数范围为0至100分；
            3. 对于体检报告有异常数据，请列出（异常数据的结论、体检项目名称、检查结果、参考值、单位、异常解读、建议）这8字段。解读异常数据，解决这些数据可能代表的健康问题或风险。分析可能的原因，包括但不限于生活习惯、饮食习惯、遗传因素等。基于这些异常数据和可能的原因，请给出具体的健康建议，包括饮食调整、运动建议、生活方式改变以及是否需要进一步检查或治疗等。
            结论格式：异常数据的结论：肥胖，体检项目名称：体重指数BMI，检查结果：29.2，参考值>24，单位：-。异常解读：体重超标包括超重与肥胖。体重指数（BMI）=体重（kg）/身高（m）的平方，BMI≥24为超重，BMI≥28为肥胖；男性腰围≥90cm和女性腰围≥85cm为腹型肥胖。体重超标是一种由多因素（如遗传、进食油脂较多、运动少、疾病等）引起的慢性代谢性疾病，尤其是肥胖，已经被世界卫生组织列为导致疾病负担的十大危险因素之一。AI建议：采取综合措施预防和控制体重，积极改变生活方式，宜低脂、低糖、高纤维素膳食，多食果蔬及菌藻类食物，增加有氧运动。若有相关疾病（如血脂异常、高血压、糖尿病等）应积极治疗。
            4. 根据这个体检报告的内容，分别是给人体的8大系统打分，每项满分为100分，8大系统分别为：呼吸系统、消化系统、内分泌系统、免疫系统、循环系统、泌尿系统、运动系统、感官系统
            5. 给体检报告做一个总结，总结格式：体检报告中尿蛋白、癌胚抗原、血沉、空腹血糖、总胆固醇、甘油三酯、低密度脂蛋白胆固醇、血清载脂蛋白B、动脉硬化指数、白细胞、平均红细胞体积、平均血红蛋白共12项指标提示异常，尿液常规共1项指标处于临界值，血脂、血液常规、尿液常规、糖类抗原、血清酶类等共43项指标提示正常，综合这些临床指标和数据分析：肾脏、肝胆、心脑血管存在隐患，其中心脑血管有"高危"风险；肾脏部位有"中危"风险；肝胆部位有"低危"风险。

            # 输出要求：
            最后，将以上结果输出为纯JSON格式，不要包含其他的文字说明，也不要出现Markdown语法相关的文字，所有的返回结果都是json，注意双引号的单引号的配合使用，详细格式如下：

            {
              "healthScore": XX.XX,
              "riskLevel": "健康|提示|风险|危险|严重危险",
              "abnormalData": [
                {
                  "conclusion": "异常数据的结论",
                  "examinationItem": "体检项目名称",
                  "result": "检查结果",
                  "referenceValue": "参考值",
                  "unit": "单位",
                  "interpret":"对于异常的结论进一步详细的说明",
                  "advice":"针对于这一项的异常，给出一些健康的建议"
                }
              ],
              "systemScore": {
                "breathingSystem": XX,
                "digestiveSystem": XX,
                "endocrineSystem": XX,
                "immuneSystem": XX,
                "circulatorySystem": XX,
                "urinarySystem": XX,
                "motionSystem": XX,
                "senseSystem": XX
              },
              "summarize": "体检报告的总结"
            }
            """;

    /**
     * 查询健康评估记录
     *
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    @Override
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id)
    {
        return healthAssessmentDataCollectionService.getById(id);
    }

    /**
     * 查询健康评估记录列表
     *
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment)
    {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }

    /**
     * 新增健康评估记录
     * @param dto 健康评估记录
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long insertHealthAssessment(ElderAssessmentDto dto)
    {
        return saveOrUpdateHealthAssessment(dto);
    }

    /**
     * 修改健康评估记录
     *
     * @param dto 健康评估记录
     * @return 结果
     */
    @Override
    public Long updateHealthAssessment(ElderAssessmentDto dto)
    {
        return saveOrUpdateHealthAssessment(dto);
    }

    /**
     * 取消健康评估（仅评估中的记录可取消）
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    public int cancelHealthAssessment(Long id)
    {
        HealthAssessment ha = getById(id);
        if (ha == null)
        {
            throw new BaseException("评估记录不存在");
        }
        // 仅评估中的记录可取消
        if (!ha.getEvaluationProgress().equals(0))
        {
            throw new BaseException("评估已完成或已取消，不能再次取消");
        }
        // 评估进度置为2-已取消
        ha.setEvaluationProgress(2);
        return baseMapper.updateById(ha);
    }

    /**
     * 新增或修改健康评估记录
     * @param dto
     * @return
     */
    private Long saveOrUpdateHealthAssessment(ElderAssessmentDto dto) {
        // 保存两份数据，评估基本信息表，评估详细数据表  注意，这两个表的主键是一样的
        HealthAssessment healthAssessment = new HealthAssessment();
        // 如果id不为空，则根据id查询评估基本信息
        if(dto.getId() != null){
            //查询状态  如果是评估完成，则不能再新增或修改
            HealthAssessment ha = getById(dto.getId());
            if(!ha.getEvaluationProgress().equals(0)){
                throw new BaseException("评估已完成，不能再次修改");
            }
            healthAssessment = ha;
        }else {
            // 入住状态  默认为0  未入住
            healthAssessment.setCheckInStatus(0);
            // 核心建议  默认为null
            healthAssessment.setCoreSuggestion(null);
            // 评估进度  默认为0  评估中
            healthAssessment.setEvaluationProgress(0);
        }
        // 老人姓名  从基本信息中获取
        healthAssessment.setElderName(dto.getBasicInfo().getElderName());
        // 身份证号码  从基本信息中获取
        healthAssessment.setIdCard(dto.getBasicInfo().getIdCard());
        // 保存，会自动主键返回
        saveOrUpdate(healthAssessment);

        //评估数据数据采集表
        HealthAssessmentDataCollection healthAssessmentDataCollection = new HealthAssessmentDataCollection();
        // 设置主键，与评估基本信息表的主键一致
        healthAssessmentDataCollection.setId(healthAssessment.getId());
        // 其余字段主要是为了方便展示使用，全部转换为JSON字符串中，存储到字段中
        // 基本信息
        healthAssessmentDataCollection.setBasicInfo(JSONUtil.toJsonStr(dto.getBasicInfo()));
        // 健康评估
        healthAssessmentDataCollection.setHealthAssessment(JSONUtil.toJsonStr(dto.getHealthAssessmentDto()));
        // 日常生活活动
        healthAssessmentDataCollection.setDailyLivingActivities(JSONUtil.toJsonStr(dto.getDailyLivingActivities()));
        // 精神状态
        healthAssessmentDataCollection.setMentalState(JSONUtil.toJsonStr(dto.getMentalState()));
        // 感知与沟通
        healthAssessmentDataCollection.setPerceptionCommunication(JSONUtil.toJsonStr(dto.getPerceptionAndCommunication()));
        // 社会参与
        healthAssessmentDataCollection.setSocialParticipation(JSONUtil.toJsonStr(dto.getSocialParticipation()));

        healthAssessmentDataCollectionService.saveOrUpdate(healthAssessmentDataCollection);

        //返回主键，方便前端查询或处理
        return healthAssessment.getId();
    }

    // ==================== AI评估核心流程 ====================

    /**
     * AI评估完整流程
     * 1. 保存评估基础数据
     * 2. 提取四大维度能力等级
     * 3. 第一阶段AI分析：多维度能力评估
     * 4. 第二阶段AI分析：体检报告评估
     * 5. 结果存储与状态更新
     * 6. 返回评估ID
     *
     * @param dto 老人评估数据
     * @return 评估ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long processAssessment(ElderAssessmentDto dto) {
        // ========== 1. 保存评估基础数据 ==========
        Long assessmentId = saveOrUpdateHealthAssessment(dto);

        // ========== 2. 提取四大维度能力等级 ==========
        String dailyActivityLevel = dto.getDailyLivingActivities().getAbilityRating();
        String mentalStatusLevel = dto.getMentalState().getAbilityRating();
        String perceptionCommunicationLevel = dto.getPerceptionAndCommunication().getAbilityRating();
        String socialParticipationLevel = dto.getSocialParticipation().getAbilityRating();

        // ========== 3. 第一阶段AI分析：多维度能力评估 ==========
        String initialAbilityLevel;
        String finalAbilityLevel;
        String levelChangeReason;

        try {
            // 构建老人能力评估Prompt
            String abilityPrompt = buildElderAbilityPrompt(dto);
            // 调用AI分析
            String abilityResult = chatClientByAssessment.prompt()
                    .user(abilityPrompt)
                    .call()
                    .content();
            // 解析JSON结果
            String cleanResult = cleanAiJsonResponse(abilityResult);
            JSONObject abilityJson = JSONUtil.parseObj(cleanResult);
            initialAbilityLevel = abilityJson.getStr("preLevel");
            finalAbilityLevel = abilityJson.getStr("finalLevel");
            levelChangeReason = abilityJson.getStr("reason", "");
        } catch (Exception e) {
            throw new BaseException("第一阶段AI分析（能力评估）失败：" + e.getMessage());
        }

        // ========== 4. 第二阶段AI分析：体检报告评估 ==========
        HealthAssessmentReport report = new HealthAssessmentReport();
        report.setHealthAssessmentId(assessmentId);
        report.setAssessmentTime(new Date());
        // 评估员姓名：当前登录用户
        report.setAssessorName(SecurityUtils.getUsername());
        // 四大维度等级
        report.setDailyActivityLevel(dailyActivityLevel);
        report.setMentalStatusLevel(mentalStatusLevel);
        report.setPerceptionCommunicationLevel(perceptionCommunicationLevel);
        report.setSocialParticipationLevel(socialParticipationLevel);
        // 能力评估结果
        report.setInitialAbilityLevel(initialAbilityLevel);
        report.setFinalAbilityLevel(finalAbilityLevel);
        report.setLevelChangeReason(levelChangeReason);

        // 获取体检报告URL
        String medicalReportUrl = dto.getHealthAssessmentDto().getRecent30Days().getMedicalReport();
        if (medicalReportUrl != null && !medicalReportUrl.isBlank()) {
            try {
                // 从OSS下载体检报告PDF
                InputStream pdfInputStream = ossAliyunFileStorageService.download(medicalReportUrl);
                // 提取PDF文本内容
                String pdfContent = extractPdfContent(pdfInputStream);
                // 构建健康评估Prompt
                String healthPrompt = buildHealthAssessmentPrompt(pdfContent);
                // 调用AI分析体检报告
                String healthResult = chatClientByAssessment.prompt()
                        .user(healthPrompt)
                        .call()
                        .content();
                // 解析评估结果
                String cleanHealthResult = cleanAiJsonResponse(healthResult);
                JSONObject healthJson = JSONUtil.parseObj(cleanHealthResult);

                double healthScore = healthJson.getDouble("healthScore", 0.0);
                String riskLevel = healthJson.getStr("riskLevel");
                JSONArray abnormalData = healthJson.getJSONArray("abnormalData");
                JSONObject systemScore = healthJson.getJSONObject("systemScore");
                String summarize = healthJson.getStr("summarize");

                // 填充健康评估结果
                report.setHealthScore(String.valueOf(healthScore));
                report.setRiskLevel(riskLevel);
                report.setAbnormalAnalysis(abnormalData != null ? abnormalData.toString() : null);
                report.setSystemScore(systemScore != null ? systemScore.toString() : null);
                report.setReportSummary(summarize);

                // healthScore超过60才建议入住
                report.setCoreSuggestion(healthScore > 60 ? 1 : 0);
            } catch (Exception e) {
                // 体检报告分析失败时，保留能力评估结果，但健康评估相关字段置空
                report.setHealthScore(null);
                report.setRiskLevel(null);
                report.setAbnormalAnalysis(null);
                report.setSystemScore(null);
                report.setReportSummary(null);
                report.setCoreSuggestion(null);
            }
        }

        // ========== 5. 结果存储与状态更新 ==========
        // 保存评估报告
        healthAssessmentReportService.saveOrUpdate(report);

        // 更新评估信息状态
        HealthAssessment assessment = getById(assessmentId);
        assessment.setEvaluationProgress(1); // 评估完成
        // 根据健康评分设置核心建议（体检报告未分析时保持原有）
        if (report.getCoreSuggestion() != null) {
            assessment.setCoreSuggestion(report.getCoreSuggestion());
        }
        updateById(assessment);

        // ========== 6. 返回评估ID ==========
        return assessmentId;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建老人能力评估Prompt
     */
    private String buildElderAbilityPrompt(ElderAssessmentDto dto) {
        HealthAssessmentDto healthDto = dto.getHealthAssessmentDto();
        HealthAssessmentDto.DiseaseDiagnosis diseaseDiagnosis = healthDto.getDiseaseDiagnosis();
        HealthAssessmentDto.Recent30Days recent30Days = healthDto.getRecent30Days();

        // 痴呆疾病：将中文描述转为数字标识（0-无，1-有）
        String dementia = diseaseDiagnosis.getDementia();
        int dementiaFlag = (dementia != null && !dementia.equals("无") && !dementia.isBlank()) ? 1 : 0;

        // 精神疾病：将中文描述转为数字标识
        String mentalIllness = diseaseDiagnosis.getMentalIllness();
        int mentalIllnessFlag = (mentalIllness != null && !mentalIllness.equals("无") && !mentalIllness.isBlank()) ? 1 : 0;

        // 是否确诊为认知障碍：从画钟测验结果获取（0-正确，1-错误，2-确诊认知障碍）
        MentalState.ClockDrawingTest clockTest = dto.getMentalState().getClockDrawingTest();
        int cognitiveImpairment = clockTest != null ? clockTest.getResult() : 0;

        return String.format(ELDER_ABILITY_PROMPT_TEMPLATE,
                dto.getDailyLivingActivities().getAbilityRating(),
                dto.getMentalState().getAbilityRating(),
                dto.getPerceptionAndCommunication().getAbilityRating(),
                dto.getSocialParticipation().getAbilityRating(),
                recent30Days.getFall() != null ? recent30Days.getFall() : 0,
                recent30Days.getChoking() != null ? recent30Days.getChoking() : 0,
                recent30Days.getSuicideAttempt() != null ? recent30Days.getSuicideAttempt() : 0,
                recent30Days.getLost() != null ? recent30Days.getLost() : 0,
                recent30Days.getComa() != null ? recent30Days.getComa() : 0,
                dementiaFlag,
                mentalIllnessFlag,
                cognitiveImpairment
        );
    }

    /**
     * 构建体检报告健康评估Prompt
     */
    private String buildHealthAssessmentPrompt(String pdfContent) {
        return String.format(HEALTH_ASSESSMENT_PROMPT_TEMPLATE, pdfContent);
    }

    /**
     * 从PDF输入流中提取文本内容
     */
    private String extractPdfContent(InputStream inputStream) {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                new InputStreamResource(inputStream),
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(9999) // 所有页面合并为一个Document
                        .build()
        );
        List<Document> documents = pdfReader.read();
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 清理AI返回的JSON字符串，去除Markdown代码块标记
     */
    private String cleanAiJsonResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            throw new BaseException("AI返回结果为空");
        }
        String cleaned = aiResponse.trim();
        // 去除Markdown代码块标记 ```json ... ``` 或 ``` ... ```
        if (cleaned.startsWith("```")) {
            // 找到第一个换行符，去除 ```json 或 ``` 开头
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            // 去除结尾的 ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }
        return cleaned;
    }

    /**
     * 根据评估ID获取老人基本信息（用于入住字段填充）
     */
    @Override
    public ElderInfoVo getElderInfoByAssessmentId(Long id) {
        // 从评估基本信息表获取核心建议
        HealthAssessment assessment = getById(id);
        // 从数据采集表获取基本信息JSON
        HealthAssessmentDataCollection dataCollection = healthAssessmentDataCollectionService.getById(id);
        if (dataCollection == null || dataCollection.getBasicInfo() == null) {
            throw new BaseException("未找到评估数据");
        }

        BasicInfo basicInfo = JSONUtil.toBean(dataCollection.getBasicInfo(), BasicInfo.class);
        ElderInfoVo vo = new ElderInfoVo();
        vo.setPhone(basicInfo.getElderContact());
        vo.setMedicalPaymentMethod(basicInfo.getMedicalPaymentMethod());
        vo.setCoreSuggestion(assessment != null ? assessment.getCoreSuggestion() : null);
        vo.setNation(basicInfo.getNation());
        vo.setEducationLevel(basicInfo.getEducationLevel());
        vo.setIdCardNo(basicInfo.getIdCard());
        vo.setName(basicInfo.getElderName());
        vo.setSocialSecurityCard(basicInfo.getSocialSecurityCard());
        vo.setLivingSituation(basicInfo.getLivingSituation());
        vo.setReligiousBelief(basicInfo.getReligiousBelief());
        vo.setEconomicSource(basicInfo.getEconomicSource());
        vo.setMaritalStatus(basicInfo.getMaritalStatus());
        return vo;
    }

    /**
     * 批量删除健康评估记录
     *
     * @param ids 需要删除的健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除健康评估记录信息
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
