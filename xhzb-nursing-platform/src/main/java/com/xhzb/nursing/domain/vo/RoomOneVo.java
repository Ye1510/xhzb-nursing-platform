package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房间详情VO（含楼层、房间、价格）
 */
@Data
@Schema(description = "房间详情VO")
public class RoomOneVo {

    @Schema(title = "楼层名称")
    private String floorName;

    @Schema(title = "楼层ID")
    private Long floorId;

    @Schema(title = "房间ID")
    private Long roomId;

    @Schema(title = "房间编号")
    private String code;

    @Schema(title = "床位价格")
    private BigDecimal price;
}
