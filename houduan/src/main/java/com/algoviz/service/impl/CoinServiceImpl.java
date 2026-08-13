package com.algoviz.service.impl;

import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.CoinPurchase;
import com.algoviz.entity.User;
import com.algoviz.mapper.CoinMapper;
import com.algoviz.mapper.UserMapper;
import com.algoviz.service.CoinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CoinServiceImpl implements CoinService {

    private static final Logger logger = LoggerFactory.getLogger(CoinServiceImpl.class);

    @Autowired
    private CoinMapper coinMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<CoinProduct> getActiveProducts() {
        return coinMapper.getAllActiveProducts();
    }

    @Override
    public CoinProduct getProduct(String productId) {
        return coinMapper.getProductById(productId);
    }

    /**
     * 购买商品：事务内扣币 + 写记录
     */
    @Override
    @Transactional
    public Map<String, Object> purchase(Long userId, String productId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查商品
        CoinProduct product = coinMapper.getProductById(productId);
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            result.put("success", false);
            result.put("message", "商品不存在或已下架");
            return result;
        }

        // 2. 查用户
        User user = userMapper.findById(userId.intValue());
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        int currentCoins = user.getCoins() != null ? user.getCoins() : 1000;
        int price = product.getCoinPrice();

        // 3. 校验余额
        if (currentCoins < price) {
            result.put("success", false);
            result.put("message", "硬币不足，当前余额 " + currentCoins + "，需要 " + price);
            result.put("currentCoins", currentCoins);
            return result;
        }

        // 4. 扣币
        int coinAfter = currentCoins - price;
        userMapper.setCoins(userId.intValue(), coinAfter);
        logger.info("用户 {} 购买商品 {}，扣除 {} 硬币，余额 {} -> {}",
                userId, productId, price, currentCoins, coinAfter);

        // 5. 写购买记录
        CoinPurchase purchase = new CoinPurchase();
        purchase.setUserId(userId);
        purchase.setUsername(user.getUsername());
        purchase.setProductId(productId);
        purchase.setProductName(product.getProductName());
        purchase.setCoinPrice(price);
        purchase.setCoinBefore(currentCoins);
        purchase.setCoinAfter(coinAfter);
        purchase.setStatus("SUCCESS");
        coinMapper.insertPurchase(purchase);

        // 6. cmd 控制台输出
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────┐");
        System.out.println("│            🪙  硬币购买成功                               │");
        System.out.println("├──────────────────────────────────────────────────────────┤");
        System.out.println("│  用户       : " + pad(user.getUsername(), 42) + "│");
        System.out.println("│  商品       : " + pad(product.getProductName(), 42) + "│");
        System.out.println("│  消耗硬币   : " + pad(price + " 🪙", 42) + "│");
        System.out.println("│  余额变化   : " + pad(currentCoins + " -> " + coinAfter, 42) + "│");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();

        result.put("success", true);
        result.put("message", "购买成功");
        result.put("productName", product.getProductName());
        result.put("coinPrice", price);
        result.put("coinBefore", currentCoins);
        result.put("coinAfter", coinAfter);
        return result;
    }

    @Override
    public List<CoinPurchase> getUserPurchases(Long userId) {
        return coinMapper.getPurchasesByUserId(userId);
    }

    @Override
    public int getUserCoins(Long userId) {
        User user = userMapper.findById(userId.intValue());
        if (user == null) return 0;
        return user.getCoins() != null ? user.getCoins() : 1000;
    }

    @Override
    public int getUserTotalSpent(Long userId) {
        return coinMapper.sumUserSpent(userId);
    }

    // ===== 管理端 =====

    @Override
    public List<CoinProduct> getAllProducts() {
        return coinMapper.getAllProducts();
    }

    @Override
    public CoinProduct createProduct(CoinProduct product) {
        if (product.getStatus() == null) product.setStatus("ACTIVE");
        if (product.getCategory() == null) product.setCategory("coin");
        coinMapper.insertProduct(product);
        return coinMapper.getProductById(product.getProductId());
    }

    @Override
    public CoinProduct updateProduct(CoinProduct product) {
        coinMapper.updateProduct(product);
        return coinMapper.getProductById(product.getProductId());
    }

    @Override
    public boolean deleteProduct(String productId) {
        return coinMapper.deleteProduct(productId) > 0;
    }

    @Override
    public List<CoinPurchase> getAllPurchases() {
        return coinMapper.getAllPurchases();
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpent", coinMapper.sumAllSpent());
        stats.put("totalPurchases", coinMapper.countAllPurchases());
        stats.put("productCount", coinMapper.getAllProducts().size());
        return stats;
    }

    private static String pad(String str, int len) {
        if (str == null) str = "";
        if (str.length() >= len) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }
}
