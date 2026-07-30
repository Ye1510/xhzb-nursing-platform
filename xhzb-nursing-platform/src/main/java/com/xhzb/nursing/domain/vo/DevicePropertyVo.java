package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备上报属性数据响应模型
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Data
@Schema(description = "设备上报属性数据响应模型")
public class DevicePropertyVo {

    /** 功能标识符 */
    @Schema(title = "功能标识符")
    private String functionId;

    /** 数据上报时间（北京时间） */
    @Schema(title = "数据上报时间")
    private String eventTime;

    /** 数据值 */
    @Schema(title = "数据值")
    private Object value;
}
