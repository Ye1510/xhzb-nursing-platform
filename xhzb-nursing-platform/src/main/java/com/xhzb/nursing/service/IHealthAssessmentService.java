package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.HealthAssessment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.nursing.domain.vo.ElderInfoVo;

/**
 * 健康评估记录Service接口
 *
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IHealthAssessmentService extends IService<HealthAssessment>
{
    /**
     * 查询健康评估记录
     *
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id);

    /**
     * 查询健康评估记录列表
     *
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录集合
     */
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment);

    /**
     * 新增健康评估记录
     *
     * @param healthAssessment 健康评估记录
     * @return 结果
     */
    /**
     * 新增健康评估记录
     *
     * @param dto 健康评估记录
     * @return 结果
     */
    public Long insertHealthAssessment(ElderAssessmentDto dto);

    /**
     * 修改健康评估记录
     *
     * @param dto 健康评估记录
     * @return 结果
     */
    public Long updateHealthAssessment(ElderAssessmentDto dto);

    /**
     * 批量删除健康评估记录
     *
     * @param ids 需要删除的健康评估记录主键集合
     * @return 结果
     */
    public int deleteHealthAssessmentByIds(Long[] ids);

    /**
     * 删除健康评估记录信息
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    public int deleteHealthAssessmentById(Long id);

    /**
     * AI评估流程：保存评估数据 → 第一阶段AI分析（能力评估）→ 第二阶段AI分析（体检报告评估）→ 保存结果
     *
     * @param dto 老人评估数据
     * @return 评估ID
     */
    public Long processAssessment(ElderAssessmentDto dto);

    /**
     * 取消健康评估（仅评估中的记录可取消）
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    public int cancelHealthAssessment(Long id);

    /**
     * 根据评估ID获取老人基本信息（用于入住字段填充）
     *
     * @param id 评估ID
     * @return 老人基本信息
     */
    public com.xhzb.nursing.domain.vo.ElderInfoVo getElderInfoByAssessmentId(Long id);
}
