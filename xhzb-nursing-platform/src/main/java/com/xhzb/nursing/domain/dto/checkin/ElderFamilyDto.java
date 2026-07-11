package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家属信息DTO
 */
@Data
@Schema(description = "家属信息")
public class ElderFamilyDto {

    /** 与老人关系 */
    @JsonProperty("kinship")
    @Schema(title = "与老人关系")
    private String kinship;

    /** 家属姓名 */
    @JsonProperty("name")
    @Schema(title = "家属姓名")
    private String name;

    /** 家属电话 */
    @JsonProperty("phone")
    @Schema(title = "家属电话")
    private String phone;
}
