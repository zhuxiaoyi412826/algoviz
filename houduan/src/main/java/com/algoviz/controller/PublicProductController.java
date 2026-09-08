package com.algoviz.controller;

import com.algoviz.entity.Product;
import com.algoviz.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开商品浏览接口（前台商品中心用；product 表人民币商品）
 * <p>无需登录：WebConfig 中已将该路径从 AuthInterceptor 排除；
 * 数据与后台 GraphQL「商品管理 → 商品列表」同源（同一张 product 表），
 * 后台新增/修改后前台刷新即生效。
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "商品中心(前台公开)", description = "人民币商品浏览（product 表）")
public class PublicProductController {

    @Autowired
    private ProductMapper productMapper;

    @GetMapping
    @Operation(summary = "获取全部人民币商品")
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        List<Product> products = productMapper.getAllProducts();
        // 资料下载链接仅随订单邮件发放，公开接口不下发，避免资料被任意抓取
        products.forEach(p -> p.setMaterialUrl(null));
        result.put("success", true);
        result.put("products", products);
        result.put("count", products.size());
        return result;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "获取人民币商品详情")
    public Map<String, Object> detail(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        Product product = productMapper.getProductById(productId);
        if (product != null) {
            product.setMaterialUrl(null);
            result.put("success", true);
            result.put("product", product);
        } else {
            result.put("success", false);
            result.put("message", "商品不存在");
        }
        return result;
    }
}
