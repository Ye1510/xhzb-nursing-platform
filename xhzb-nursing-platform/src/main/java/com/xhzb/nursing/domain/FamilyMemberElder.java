package com.xhzb.nursing.domain;

import com.xhzb.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.xhzb.common.core.domain.BaseEntity;

/**
 * 客户老人关联对象 family_member_elder
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Data
@Schema(description = "客户老人关联对象")
public class FamilyMemberElder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Schema(title = "id")
    private Long id;

    /** 家属id */
    @Schema(title = "家属id")
    @Excel(name = "家属id")
    private Long familyMemberId;

    /** 老人id */
    @Schema(title = "老人id")
    @Excel(name = "老人id")
    private Long elderId;

}
