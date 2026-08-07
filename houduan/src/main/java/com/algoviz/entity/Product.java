package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "商品实体")
public class Product {

    @Schema(description = "商品ID")
    private String productId;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "商品描述")
    private String description;
    @Schema(description = "价格")
    private int price;
    @Schema(description = "分类")
    private String category;
    @Schema(description = "图标")
    private String icon;

    public Product() {
    }

    public Product(String productId, String productName, String description,
                   int price, String category, String icon) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.category = category;
        this.icon = icon;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}