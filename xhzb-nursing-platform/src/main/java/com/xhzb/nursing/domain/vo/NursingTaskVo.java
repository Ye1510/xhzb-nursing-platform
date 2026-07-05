package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 护理任务VO（详情查询返回）
 */
@Data
@Schema(description = "护理任务详情")
public class NursingTaskVo {

    @Schema(title = "主键")
    private Long id;

    @Schema(title = "护理员id")
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

    @Schema(title = "状态 1待执行 2已执行 3已关闭")
    private Integer status;

    @Schema(title = "执行图片")
    private String taskImage;

    @Schema(title = "护理员姓名列表")
    private List<String> nursingName;

    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    @Schema(title = "年龄")
    private Integer age;

    @Schema(title = "性别")
    private String sex;

    @Schema(title = "执行人")
    private String updater;

    @Schema(title = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(title = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(title = "备注")
    private String remark;
}
