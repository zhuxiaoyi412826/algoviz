package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 批量导入结果 */
@Data
@Schema(description = "批量导入结果")
public class BatchImportResult {
    @Schema(description = "总条数")
    private int total;
    @Schema(description = "成功数")
    private int successNum;
    @Schema(description = "失败数")
    private int failNum;
    @Schema(description = "失败原因列表")
    private List<String> failList;
    @Schema(description = "文件名（文件导入时填充）")
    private String fileName;

    public static BatchImportResult of(int total, int successNum, int failNum, List<String> failList) {
        BatchImportResult r = new BatchImportResult();
        r.total = total;
        r.successNum = successNum;
        r.failNum = failNum;
        r.failList = failList;
        return r;
    }
}
