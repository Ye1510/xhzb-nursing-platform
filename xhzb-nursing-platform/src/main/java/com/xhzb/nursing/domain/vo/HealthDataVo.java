package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 健康数据返回（当前最新值）
 *
 * @author ruoyi
 */
@Data
@Schema(description = "健康数据当前值")
public class HealthDataVo {

    @Schema(title = "物模型功能ID")
    private String functionId;

    @Schema(title = "上报时间(ISO8601)")
    private String eventTime;

    @Schema(title = "数值")
    private Double value;
}
