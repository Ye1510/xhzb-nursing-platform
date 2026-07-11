package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入住配置DTO
 */
@Data
@Schema(description = "入住配置")
public class CheckInConfigDto {

    /** 床位费用 */
    @JsonProperty("bedFee")
    @Schema(title = "床位费用")
    private String bedFee;

    /** 床位ID */
    @JsonProperty("bedId")
    @Schema(title = "床位ID")
    private Long bedId;

    /** 房间编号 */
    @JsonProperty("code")
    @Schema(title = "房间编号")
    private String code;

    /** 押金 */
    @JsonProperty("deposit")
    @Schema(title = "押金")
    private Integer deposit;

    /** 入住结束时间 */
    @JsonProperty("endDate")
    @Schema(title = "入住结束时间")
    private String endDate;

    /** 费用结束时间 */
    @JsonProperty("feeEndDate")
    @Schema(title = "费用结束时间")
    private String feeEndDate;

    /** 费用开始时间 */
    @JsonProperty("feeStartDate")
    @Schema(title = "费用开始时间")
    private String feeStartDate;

    /** 楼层ID */
    @JsonProperty("floorId")
    @Schema(title = "楼层ID")
    private Long floorId;

    /** 楼层名称 */
    @JsonProperty("floorName")
    @Schema(title = "楼层名称")
    private String floorName;

    /** 政府补贴 */
    @JsonProperty("governmentSubsidy")
    @Schema(title = "政府补贴")
    private Integer governmentSubsidy;

    /** 医保支付 */
    @JsonProperty("insurancePayment")
    @Schema(title = "医保支付")
    private Integer insurancePayment;

    /** 护理费用 */
    @JsonProperty("nursingFee")
    @Schema(title = "护理费用")
    private Integer nursingFee;

    /** 护理等级ID */
    @JsonProperty("nursingLevelId")
    @Schema(title = "护理等级ID")
    private Long nursingLevelId;

    /** 护理等级名称 */
    @JsonProperty("nursingLevelName")
    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    /** 其他费用 */
    @JsonProperty("otherFees")
    @Schema(title = "其他费用")
    private Integer otherFees;

    /** 房间ID */
    @JsonProperty("roomId")
    @Schema(title = "房间ID")
    private Long roomId;

    /** 入住开始时间 */
    @JsonProperty("startDate")
    @Schema(title = "入住开始时间")
    private String startDate;
}
