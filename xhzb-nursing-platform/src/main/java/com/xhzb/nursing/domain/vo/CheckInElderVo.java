package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入住详情-老人信息VO
 */
@Data
@Schema(description = "入住详情-老人信息")
public class CheckInElderVo {

    @Schema(title = "老人ID")
    private Long id;

    @Schema(title = "老人姓名")
    private String name;

    @Schema(title = "身份证号")
    private String idCardNo;

    @Schema(title = "出生日期")
    private String birthday;

    @Schema(title = "性别")
    private Integer sex;

    @Schema(title = "手机号")
    private String phone;

    @Schema(title = "家庭住址")
    private String address;

    @Schema(title = "头像")
    private String image;

    @Schema(title = "身份证国徽面")
    private String idCardNationalEmblemImg;

    @Schema(title = "身份证人像面")
    private String idCardPortraitImg;

    @Schema(title = "年龄")
    private Integer age;

    @Schema(title = "民族")
    private String nation;

    @Schema(title = "文化程度")
    private String educationLevel;

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

    @Schema(title = "医疗费用支付方式")
    private String medicalPaymentMethod;

    @Schema(title = "核心建议")
    private String coreSuggestion;
}
