package com.xhzb.nursing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护理员老人关联对象 nursing_elder
 */
@Data
@TableName("nursing_elder")
public class NursingElder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nursingId;

    private Long elderId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
