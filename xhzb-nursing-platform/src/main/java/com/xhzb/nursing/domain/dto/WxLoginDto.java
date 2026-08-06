package com.xhzb.nursing.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求DTO
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Data
@Schema(description = "微信小程序登录请求")
public class WxLoginDto {

    /** 临时登录凭证code */
    @NotBlank(message = "临时登录凭证code不能为空")
    @Schema(title = "临时登录凭证code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /** 微信用户昵称 */
    @Schema(title = "微信用户昵称")
    private String nickName;

    /** 获取手机的临时code */
    @NotBlank(message = "获取手机的临时code不能为空")
    @Schema(title = "获取手机的临时code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneCode;

}
