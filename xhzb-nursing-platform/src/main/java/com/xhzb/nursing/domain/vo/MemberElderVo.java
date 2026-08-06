package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小程序端家属绑定的老人列表返回对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "家属绑定老人信息")
public class MemberElderVo {

    @Schema(title = "关联记录ID（解绑用）")
    private Long mid;

    @Schema(title = "老人ID")
    private Long elderId;

    @Schema(title = "老人姓名")
    private String name;

    @Schema(title = "老人头像")
    private String image;

    @Schema(title = "备注（家属与老人关系等）")
    private String mremark;

    @Schema(title = "床位号")
    private String bedNumber;

    @Schema(title = "房间类型名称")
    private String typeName;

    @Schema(title = "设备物联网ID")
    private String iotId;

    @Schema(title = "设备名称")
    private String deviceName;

    @Schema(title = "产品Key")
    private String productKey;
}
