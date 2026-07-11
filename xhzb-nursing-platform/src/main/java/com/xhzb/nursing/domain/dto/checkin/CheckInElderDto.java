package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入住老人信息DTO
 */
@Data
@Schema(description = "入住老人信息")
public class CheckInElderDto {

    /** 家庭住址 */
    @JsonProperty("address")
    @Schema(title = "家庭住址")
    private String address;

    /** 年龄 */
    @JsonProperty("age")
    @Schema(title = "年龄")
    private String age;

    /** 出生日期 */
    @JsonProperty("birthday")
    @Schema(title = "出生日期")
    private String birthday;

    /** 身份证国徽面 */
    @JsonProperty("idCardNationalEmblemImg")
    @Schema(title = "身份证国徽面")
    private String idCardNationalEmblemImg;

    /** 身份证号 */
    @JsonProperty("idCardNo")
    @Schema(title = "身份证号")
    private String idCardNo;

    /** 身份证人像面 */
    @JsonProperty("idCardPortraitImg")
    @Schema(title = "身份证人像面")
    private String idCardPortraitImg;

    /** 头像 */
    @JsonProperty("image")
    @Schema(title = "头像")
    private String image;

    /** 老人姓名 */
    @JsonProperty("name")
    @Schema(title = "老人姓名")
    private String name;

    /** 手机号 */
    @JsonProperty("phone")
    @Schema(title = "手机号")
    private String phone;

    /** 性别（0:女 1:男） */
    @JsonProperty("sex")
    @Schema(title = "性别（0:女 1:男）")
    private Integer sex;

    /** 民族 */
    @JsonProperty("nation")
    @Schema(title = "民族")
    private String nation;

    /** 文化程度 */
    @JsonProperty("educationLevel")
    @Schema(title = "文化程度")
    private String educationLevel;

    /** 社保卡号 */
    @JsonProperty("socialSecurityCard")
    @Schema(title = "社保卡号")
    private String socialSecurityCard;

    /** 居住情况 */
    @JsonProperty("livingSituation")
    @Schema(title = "居住情况")
    private String livingSituation;

    /** 宗教信仰 */
    @JsonProperty("religiousBelief")
    @Schema(title = "宗教信仰")
    private String religiousBelief;

    /** 经济来源 */
    @JsonProperty("economicSource")
    @Schema(title = "经济来源")
    private String economicSource;

    /** 婚姻状况 */
    @JsonProperty("maritalStatus")
    @Schema(title = "婚姻状况")
    private String maritalStatus;

    /** 医疗费用支付方式 */
    @JsonProperty("medicalPaymentMethod")
    @Schema(title = "医疗费用支付方式")
    private String medicalPaymentMethod;

    /** 核心建议 */
    @JsonProperty("coreSuggestion")
    @Schema(title = "核心建议")
    private String coreSuggestion;
}
