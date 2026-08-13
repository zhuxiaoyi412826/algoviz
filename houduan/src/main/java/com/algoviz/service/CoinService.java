package com.algoviz.service;

import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.CoinPurchase;

import java.util.List;
import java.util.Map;

public interface CoinService {

    /** 获取所有上架硬币商品 */
    List<CoinProduct> getActiveProducts();

    /** 获取商品详情 */
    CoinProduct getProduct(String productId);

    /** 购买商品（扣币 + 记录） */
    Map<String, Object> purchase(Long userId, String productId);

    /** 获取用户购买记录 */
    List<CoinPurchase> getUserPurchases(Long userId);

    /** 获取用户硬币余额 */
    int getUserCoins(Long userId);

    /** 获取用户总消耗 */
    int getUserTotalSpent(Long userId);

    // ===== 管理端 =====

    /** 管理端：获取所有商品 */
    List<CoinProduct> getAllProducts();

    /** 管理端：新增/编辑/删除商品 */
    CoinProduct createProduct(CoinProduct product);
    CoinProduct updateProduct(CoinProduct product);
    boolean deleteProduct(String productId);

    /** 管理端：获取所有购买记录 */
    List<CoinPurchase> getAllPurchases();

    /** 管理端：统计 */
    Map<String, Object> getStats();
}
