package com.algoviz.dto.vector;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "ChromaDB Collection 实时信息")
public class VectorCollectionInfo {

    @Schema(description = "Collection 名称")
    private String name;

    @Schema(description = "向量总数")
    private int vectorCount;

    @Schema(description = "向量维度（如 512 = bge-small-zh-v1.5）")
    private int dimension;

    @Schema(description = "距离度量（cosine / l2 / ip）")
    private String distanceMetric;

    @Schema(description = "Collection metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Chroma 持久化目录")
    private String chromaPath;

    @Schema(description = "嵌入模型名称")
    private String modelName;
}
