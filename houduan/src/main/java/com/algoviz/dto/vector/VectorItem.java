package com.algoviz.dto.vector;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "ChromaDB 中单个向量对应的题目信息")
public class VectorItem {

    @Schema(description = "Chroma 向量 ID（题目 ID 字符串）")
    private String id;

    @Schema(description = "题目 ID")
    private Long problemId;

    @Schema(description = "题目编号（如 MS001）")
    private String problemNo;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "难度：easy/medium/hard")
    private String difficulty;

    @Schema(description = "向量化文本预览（前 200 字）")
    private String documentPreview;

    @Schema(description = "向量数值（完整的浮点数组，如 512 维）")
    private List<Double> vectorValues;

    @Schema(description = "向量数值预览（前 10 个值的字符串）")
    private String vectorPreview;

    @Schema(description = "向量维度")
    private Integer vectorDimension;
}
