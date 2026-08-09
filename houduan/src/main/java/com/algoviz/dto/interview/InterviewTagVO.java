package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 前台标签返回结构（按热度排序） */
@Data
@Schema(description = "前台标签返回")
public class InterviewTagVO {
    @Schema(description = "标签名")
    private String tagName;
    @Schema(description = "使用次数（热度）")
    private Integer useCount;

    public static InterviewTagVO of(String name, Integer useCount) {
        InterviewTagVO v = new InterviewTagVO();
        v.tagName = name;
        v.useCount = useCount;
        return v;
    }
}
