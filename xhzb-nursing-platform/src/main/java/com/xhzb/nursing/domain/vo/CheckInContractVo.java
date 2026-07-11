package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入住详情-合同VO
 */
@Data
@Schema(description = "入住详情-合同")
public class CheckInContractVo {

    @Schema(title = "创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @Schema(title = "合同ID")
    private Long id;

    @Schema(title = "老人ID")
    private Long elderId;

    @Schema(title = "合同名称")
    private String contractName;

    @Schema(title = "合同编号")
    private String contractNumber;

    @Schema(title = "协议地址")
    private String agreementPath;

    @Schema(title = "丙方手机号")
    private String thirdPartyPhone;

    @Schema(title = "丙方姓名")
    private String thirdPartyName;

    @Schema(title = "老人姓名")
    private String elderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "开始时间")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "结束时间")
    private LocalDateTime endDate;

    @Schema(title = "状态")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "签约日期")
    private LocalDateTime signDate;

    @Schema(title = "排序编号")
    private Integer sortOrder;
}
