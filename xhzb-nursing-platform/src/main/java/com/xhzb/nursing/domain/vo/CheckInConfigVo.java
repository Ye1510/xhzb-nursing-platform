package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入住详情-入住配置VO
 */
@Data
@Schema(description = "入住详情-入住配置")
public class CheckInConfigVo {

    @Schema(title = "创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "入住表ID")
    private Long checkInId;

    @Schema(title = "护理等级ID")
    private Long nursingLevelId;

    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用开始时间")
    private LocalDateTime feeStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用结束时间")
    private LocalDateTime feeEndDate;

    @Schema(title = "押金")
    private BigDecimal deposit;

    @Schema(title = "护理费用")
    private BigDecimal nursingFee;

    @Schema(title = "床位费用")
    private BigDecimal bedFee;

    @Schema(title = "医保支付")
    private BigDecimal insurancePayment;

    @Schema(title = "政府补贴")
    private BigDecimal governmentSubsidy;

    @Schema(title = "其他费用")
    private BigDecimal otherFees;

    @Schema(title = "排序编号")
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住开始时间")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住结束时间")
    private LocalDateTime endDate;

    @Schema(title = "床位编号")
    private String bedNumber;
}
