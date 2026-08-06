package com.xhzb.nursing.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小程序绑定老人请求DTO
 *
 * @author ruoyi
 */
@Data
@Schema(description = "绑定老人请求")
public class MemberElderBindDto {

    @Schema(title = "老人姓名", description = "老人姓名")
    private String name;

    @Schema(title = "身份证号", description = "老人身份证号")
    private String idCard;

    @Schema(title = "备注", description = "与老人关系等备注")
    private String remark;
}
