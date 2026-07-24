package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 健康数据统计返回（按天/按周）
 *
 * @author ruoyi
 */
@Data
@Schema(description = "数据统计点")
public class TrendDataVo {

    @Schema(title = "日期时间：按天(HH:00)，按周(MM.dd)")
    private String dateTime;

    @Schema(title = "数值")
    private Double dataValue;
}
