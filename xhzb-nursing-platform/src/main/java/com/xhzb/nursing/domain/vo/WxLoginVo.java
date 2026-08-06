package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录响应VO
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信小程序登录响应")
public class WxLoginVo {

    /** 令牌 */
    @Schema(title = "令牌")
    private String token;

    /** 昵称 */
    @Schema(title = "昵称")
    private String nickName;

}
