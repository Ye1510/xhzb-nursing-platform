package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人信息VO（入住字段填充用）
 */
@Data
@Schema(description = "老人信息VO（入住字段填充）")
public class ElderInfoVo {

    @Schema(title = "联系电话")
    private String phone;

    @Schema(title = "医疗费用支付方式")
    private String medicalPaymentMethod;

    @Schema(title = "核心建议")
    private Integer coreSuggestion;

    @Schema(title = "民族")
    private String nation;

    @Schema(title = "文化程度")
    private String educationLevel;

    @Schema(title = "身份证号")
    private String idCardNo;

    @Schema(title = "老人姓名")
    private String name;

    @Schema(title = "社保卡号")
    private String socialSecurityCard;

    @Schema(title = "居住情况")
    private String livingSituation;

    @Schema(title = "宗教信仰")
    private String religiousBelief;

    @Schema(title = "经济来源")
    private String economicSource;

    @Schema(title = "婚姻状况")
    private String maritalStatus;
}
