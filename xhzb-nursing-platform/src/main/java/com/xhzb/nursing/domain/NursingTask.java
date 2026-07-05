package com.xhzb.nursing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xhzb.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护理任务对象 nursing_task
 */
@Data
@Schema(description = "护理任务对象")
public class NursingTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键")
    private Long id;

    @Schema(title = "护理员id（逗号分隔）")
    private String nursingId;

    @Schema(title = "项目id")
    private Long projectId;

    @Schema(title = "护理项目名称")
    private String projectName;

    @Schema(title = "老人id")
    private Long elderId;

    @Schema(title = "老人姓名")
    private String elderName;

    @Schema(title = "床位编号")
    private String bedNumber;

    @Schema(title = "预计服务时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedServerTime;

    @Schema(title = "实际服务时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime realServerTime;

    @Schema(title = "执行记录")
    private String mark;

    @Schema(title = "取消原因")
    private String cancelReason;

    @Schema(title = "状态  1待执行 2已执行 3已关闭")
    private Integer status;

    @Schema(title = "执行图片")
    private String taskImage;
}
