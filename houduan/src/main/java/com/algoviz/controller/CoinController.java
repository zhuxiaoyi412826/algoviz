package com.algoviz.controller;

import com.algoviz.config.AuthInterceptor;
import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.CoinPurchase;
import com.algoviz.entity.User;
import com.algoviz.service.CoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coin")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "硬币系统(前端)", description = "硬币商品浏览与购买")
public class CoinController {

    private static final Logger logger = LoggerFactory.getLogger(CoinController.class);

    @Autowired
    private CoinService coinService;

    /**
     * 获取当前登录用户ID（从 Session 或 Cookie）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(AuthInterceptor.SESSION_USER);
            if (obj instanceof User) {
                User user = (User) obj;
                if (user.getId() != null) return user.getId().longValue();
            }
        }
        String uid = AuthInterceptor.getCookieValue(request, AuthInterceptor.COOKIE_USER_ID);
        if (uid != null) {
            try { return Long.parseLong(uid); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @GetMapping("/products")
    @Operation(summary = "获取硬币商品列表", description = "获取所有上架的硬币商品")
    public Map<String, Object> getProducts() {
        Map<String, Object> result = new HashMap<>();
        List<CoinProduct> products = coinService.getActiveProducts();
        result.put("success", true);
        result.put("products", products);
        result.put("count", products.size());
        return result;
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "获取商品详情")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        CoinProduct product = coinService.getProduct(productId);
        if (product != null) {
            result.put("success", true);
            result.put("product", product);
        } else {
            result.put("success", false);
            result.put("message", "商品不存在");
        }
        return result;
    }

    @PostMapping("/purchase")
    @Operation(summary = "购买商品", description = "用硬币购买商品，扣币并记录")
    public Map<String, Object> purchase(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        String productId = body.get("productId");
        if (productId == null || productId.isEmpty()) {
            result.put("success", false);
            result.put("message", "缺少商品ID");
            return result;
        }
        logger.info("用户 {} 购买硬币商品 {}", userId, productId);
        return coinService.purchase(userId, productId);
    }

    @GetMapping("/balance")
    @Operation(summary = "获取硬币余额", description = "获取当前用户硬币余额")
    public Map<String, Object> getBalance(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        result.put("success", true);
        result.put("coins", coinService.getUserCoins(userId));
        result.put("totalSpent", coinService.getUserTotalSpent(userId));
        return result;
    }

    @GetMapping("/purchases")
    @Operation(summary = "获取购买记录", description = "获取当前用户的硬币购买记录")
    public Map<String, Object> getPurchases(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<CoinPurchase> purchases = coinService.getUserPurchases(userId);
        result.put("success", true);
        result.put("purchases", purchases);
        result.put("count", purchases.size());
        return result;
    }
}
