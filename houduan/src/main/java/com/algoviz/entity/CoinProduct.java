package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "硬币商品实体")
@Data
public class CoinProduct {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "商品编号")
    private String productId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "商品描述")
    private String description;
    @Schema(description = "硬币价格")
    private Integer coinPrice;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
