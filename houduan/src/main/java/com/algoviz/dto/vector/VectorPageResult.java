package com.algoviz.dto.vector;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "ChromaDB 向量分页结果")
public class VectorPageResult {

    @Schema(description = "向量总数")
    private int total;

    @Schema(description = "当前页码")
    private int page;

    @Schema(description = "每页条数")
    private int pageSize;

    @Schema(description = "总页数")
    private int totalPages;

    @Schema(description = "向量列表")
    private List<VectorItem> vectors;
}
