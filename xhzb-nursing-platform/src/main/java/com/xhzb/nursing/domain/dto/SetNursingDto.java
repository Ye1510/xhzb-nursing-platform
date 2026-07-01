package com.xhzb.nursing.domain.dto;

import lombok.Data;
import java.util.List;

/**
 * 设置护理员请求DTO
 */
@Data
public class SetNursingDto {

    private Long elderId;

    private List<Long> nursingIds;
}
