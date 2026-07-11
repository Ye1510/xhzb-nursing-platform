package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 入住详情VO
 */
@Data
@Schema(description = "入住详情")
public class CheckInDetailVo {

    @Schema(title = "老人信息")
    private CheckInElderVo checkInElderVo;

    @Schema(title = "家属信息列表")
    private List<ElderFamilyVo> elderFamilyVoList;

    @Schema(title = "入住配置信息")
    private CheckInConfigVo checkInConfigVo;

    @Schema(title = "合同信息")
    private CheckInContractVo contract;
}
