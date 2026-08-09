package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 后台统计 */
@Data
@Schema(description = "面试模块后台统计")
public class InterviewAdminStats {
    @Schema(description = "题库总数")
    private long totalNum;
    @Schema(description = "上线数")
    private long activeNum;
    @Schema(description = "下线数")
    private long inactiveNum;
    @Schema(description = "高频数")
    private long frequentNum;
    @Schema(description = "简单题数")
    private long easyNum;
    @Schema(description = "中等题数")
    private long mediumNum;
    @Schema(description = "困难题数")
    private long hardNum;
}
