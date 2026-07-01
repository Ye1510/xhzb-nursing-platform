package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 楼层树VO（用于楼层→房间→床位级联选择）
 */
@Data
@Schema(description = "楼层树VO")
public class FloorTreeVo {

    @Schema(title = "值")
    private String value;

    @Schema(title = "标签")
    private String label;

    @Schema(title = "子节点")
    private List<FloorTreeVo> children;

    public FloorTreeVo() {
        this.children = new ArrayList<>();
    }

    public FloorTreeVo(String value, String label) {
        this.value = value;
        this.label = label;
        this.children = new ArrayList<>();
    }

    public void addChild(FloorTreeVo child) {
        this.children.add(child);
    }
}
