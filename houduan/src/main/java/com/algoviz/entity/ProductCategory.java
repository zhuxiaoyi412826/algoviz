package com.algoviz.entity;

import lombok.Data;

/**
 * 商品分类字典（product_category 表，product.category 存显示名与之对应；
 * 只增改停用字典，不修改历史商品字符串，避免影响前台与订单）
 */
@Data
public class ProductCategory {
    private Long id;
    private String name;
    /** 排序值，越小越前 */
    private Integer sort;
    /** 1 启用 0 停用 */
    private Integer status;
    private String createdAt;
    private String updatedAt;
}
