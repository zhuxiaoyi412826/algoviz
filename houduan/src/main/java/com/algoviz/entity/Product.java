package com.algoviz.entity;

import lombok.Data;

/**
 * 商品实体（product 表：真实商品，price 单位「分」，category 为分类名称字符串，
 * 与订单 orders 通过 product_id/product_id_ref 关联，此处不改表结构保持兼容）
 */
@Data
public class Product {
    private Long id;
    private String productId;
    private String productName;
    private String description;
    /** 价格（单位：分） */
    private Integer price;
    private String category;
    private String icon;
    /** 购买后资料下载链接（仅成功订单邮件内展示，不对外公开列表返回） */
    private String materialUrl;
    private String createdAt;
    private String updatedAt;
}
