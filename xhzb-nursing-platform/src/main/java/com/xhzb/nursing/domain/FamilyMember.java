package com.xhzb.nursing.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xhzb.common.annotation.Excel;
import com.xhzb.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人家属对象 family_member
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Data
@Schema(description = "老人家属对象")
@TableName("family_member")
public class FamilyMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(title = "主键")
    private Long id;

    /** 手机号 */
    @Schema(title = "手机号")
    @Excel(name = "手机号")
    private String phone;

    /** 名称 */
    @Schema(title = "名称")
    @Excel(name = "名称")
    private String name;

    /** 头像 */
    @Schema(title = "头像")
    private String avatar;

    /** OpenID */
    @Schema(title = "OpenID")
    private String openId;

    /** 性别(0:男，1:女) */
    @Schema(title = "性别(0:男，1:女)")
    @Excel(name = "性别", readConverterExp = "0=男,1=女")
    private Integer gender;

}
