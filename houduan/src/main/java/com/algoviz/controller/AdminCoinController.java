package com.algoviz.controller;

import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.CoinPurchase;
import com.algoviz.service.CoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coin/admin")
@Tag(name = "硬币系统(后台)", description = "硬币商品管理与购买记录查询")
public class AdminCoinController {

    @Autowired
    private CoinService coinService;

    // ===== 商品管理 =====

    @GetMapping("/products")
    @Operation(summary = "获取所有硬币商品", description = "管理端获取全部商品（含下架）")
    public Map<String, Object> getAllProducts() {
        Map<String, Object> result = new HashMap<>();
        List<CoinProduct> products = coinService.getAllProducts();
        result.put("success", true);
        result.put("products", products);
        result.put("count", products.size());
        return result;
    }

    @PostMapping("/products")
    @Operation(summary = "新增硬币商品")
    public Map<String, Object> createProduct(@RequestBody CoinProduct product) {
        Map<String, Object> result = new HashMap<>();
        CoinProduct created = coinService.createProduct(product);
        result.put("success", true);
        result.put("product", created);
        return result;
    }

    @PutMapping("/products/{productId}")
    @Operation(summary = "编辑硬币商品")
    public Map<String, Object> updateProduct(@PathVariable String productId, @RequestBody CoinProduct product) {
        product.setProductId(productId);
        Map<String, Object> result = new HashMap<>();
        CoinProduct updated = coinService.updateProduct(product);
        result.put("success", true);
        result.put("product", updated);
        return result;
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "删除硬币商品")
    public Map<String, Object> deleteProduct(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        boolean ok = coinService.deleteProduct(productId);
        result.put("success", ok);
        result.put("message", ok ? "删除成功" : "删除失败");
        return result;
    }

    // ===== 购买记录查询 =====

    @GetMapping("/purchases")
    @Operation(summary = "分页查询购买记录", description = "分页查询所有用户的硬币购买记录，支持关键词搜索，默认每页100条")
    public Map<String, Object> getAllPurchases(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        List<CoinPurchase> purchases = coinService.getPurchasesByPage(keyword, page, pageSize);
        int total = coinService.countPurchasesForPage(keyword);
        result.put("success", true);
        result.put("purchases", purchases);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @GetMapping("/stats")
    @Operation(summary = "硬币系统统计", description = "总消耗/购买次数/商品数")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.putAll(coinService.getStats());
        return result;
    }
}
