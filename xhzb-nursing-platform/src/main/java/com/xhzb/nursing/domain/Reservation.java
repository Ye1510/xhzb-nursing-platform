package com.xhzb.nursing.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xhzb.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.xhzb.common.core.domain.BaseEntity;

/**
 * 预约信息对象 reservation
 *
 * @author ruoyi
 * @date 2026-08-28
 */
@Data
@Schema(description = "预约信息对象")
public class Reservation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(title = "主键")
    private Long id;

    /** 预约人姓名 */
    @Schema(title = "预约人姓名")
    @Excel(name = "预约人姓名")
    private String name;

    /** 预约人手机号 */
    @Schema(title = "预约人手机号")
    @Excel(name = "预约人手机号")
    private String mobile;

    /** 预约时间 */
    @Schema(title = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    /** 到院时间 */
    @Schema(title = "到院时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "到院时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date arriveTime;

    /** 老人姓名 */
    @Schema(title = "老人姓名")
    @Excel(name = "老人姓名")
    private String visitor;

    /** 预约类型，0：参观预约，1：探访预约 */
    @Schema(title = "预约类型，0：参观预约，1：探访预约")
    @Excel(name = "预约类型，0：参观预约，1：探访预约")
    private Integer type;

    /** 预约状态，0：待上门，1：已完成，2：已取消，3：已过期 */
    @Schema(title = "预约状态，0：待上门，1：已完成，2：已取消，3：已过期")
    @Excel(name = "预约状态，0：待上门，1：已完成，2：已取消，3：已过期")
    private Integer status;

    /** 查询条件-预约时间范围-开始 */
    @Schema(title = "查询条件-开始时间", hidden = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 查询条件-预约时间范围-结束 */
    @Schema(title = "查询条件-结束时间", hidden = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
