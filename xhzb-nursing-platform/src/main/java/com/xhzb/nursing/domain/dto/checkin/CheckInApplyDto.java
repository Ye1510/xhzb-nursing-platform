package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 申请入住请求DTO
 */
@Data
@Schema(description = "申请入住请求")
public class CheckInApplyDto {

    /** 评估ID */
    @JsonProperty("healthAssessmentId")
    @Schema(title = "评估ID")
    private Long healthAssessmentId;

    /** 入住老人信息 */
    @JsonProperty("checkInElderDto")
    @Schema(title = "入住老人信息")
    private CheckInElderDto checkInElderDto;

    /** 家属信息列表 */
    @JsonProperty("elderFamilyDtoList")
    @Schema(title = "家属信息列表")
    private List<ElderFamilyDto> elderFamilyDtoList;

    /** 入住配置 */
    @JsonProperty("checkInConfigDto")
    @Schema(title = "入住配置")
    private CheckInConfigDto checkInConfigDto;

    /** 合同信息 */
    @JsonProperty("checkInContractDto")
    @Schema(title = "合同信息")
    private CheckInContractDto checkInContractDto;
}
