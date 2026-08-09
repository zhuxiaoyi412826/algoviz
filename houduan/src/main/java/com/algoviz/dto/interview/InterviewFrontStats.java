package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 前台全站统计 */
@Data
@Schema(description = "前台题目统计")
public class InterviewFrontStats {
    @Schema(description = "总阅读量")
    private long totalView;
    @Schema(description = "总收藏数")
    private long totalCollect;
    @Schema(description = "总点赞数")
    private long totalLike;
    @Schema(description = "总点踩数")
    private long totalDislike;
}
