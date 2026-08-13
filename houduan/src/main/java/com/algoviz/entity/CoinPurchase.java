package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "硬币购买记录实体")
@Data
public class CoinPurchase {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "商品编号")
    private String productId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "消耗硬币数")
    private Integer coinPrice;
    @Schema(description = "购买前余额")
    private Integer coinBefore;
    @Schema(description = "购买后余额")
    private Integer coinAfter;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
