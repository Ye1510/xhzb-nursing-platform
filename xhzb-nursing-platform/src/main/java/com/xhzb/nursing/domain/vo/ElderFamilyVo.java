package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入住详情-家属信息VO
 */
@Data
@Schema(description = "入住详情-家属信息")
public class ElderFamilyVo {

    @Schema(title = "家属姓名")
    private String name;

    @Schema(title = "家属电话")
    private String phone;

    @Schema(title = "与老人关系")
    private String kinship;
}
