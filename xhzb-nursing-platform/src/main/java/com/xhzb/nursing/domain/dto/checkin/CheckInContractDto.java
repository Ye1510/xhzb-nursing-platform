package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 合同DTO
 */
@Data
@Schema(description = "合同信息")
public class CheckInContractDto {

    /** 协议地址 */
    @JsonProperty("agreementPath")
    @Schema(title = "协议地址")
    private String agreementPath;

    /** 合同名称 */
    @JsonProperty("contractName")
    @Schema(title = "合同名称")
    private String contractName;

    /** 签约日期 */
    @JsonProperty("signDate")
    @Schema(title = "签约日期")
    private String signDate;

    /** 丙方姓名 */
    @JsonProperty("thirdPartyName")
    @Schema(title = "丙方姓名")
    private String thirdPartyName;

    /** 丙方电话 */
    @JsonProperty("thirdPartyPhone")
    @Schema(title = "丙方电话")
    private String thirdPartyPhone;
}
