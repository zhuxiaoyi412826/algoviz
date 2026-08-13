package com.algoviz.mapper;

import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.CoinPurchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CoinMapper {

    // ===== 硬币商品 =====

    /** 获取所有上架的硬币商品 */
    List<CoinProduct> getAllActiveProducts();

    /** 获取所有硬币商品（含下架，管理端用） */
    List<CoinProduct> getAllProducts();

    /** 根据商品编号获取商品 */
    CoinProduct getProductById(@Param("productId") String productId);

    /** 新增商品 */
    int insertProduct(CoinProduct product);

    /** 更新商品 */
    int updateProduct(CoinProduct product);

    /** 删除商品 */
    int deleteProduct(@Param("productId") String productId);

    // ===== 购买记录 =====

    /** 插入购买记录 */
    int insertPurchase(CoinPurchase purchase);

    /** 根据用户ID获取购买记录 */
    List<CoinPurchase> getPurchasesByUserId(@Param("userId") Long userId);

    /** 获取所有购买记录（管理端用，含用户名） */
    List<CoinPurchase> getAllPurchases();

    /** 分页查询购买记录（支持关键词搜索用户名/商品名） */
    List<CoinPurchase> getPurchasesByPage(@Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /** 分页查询购买记录总数（支持关键词） */
    int countPurchasesForPage(@Param("keyword") String keyword);

    /** 统计用户总消耗硬币 */
    int sumUserSpent(@Param("userId") Long userId);

    /** 统计平台总硬币消耗 */
    int sumAllSpent();

    /** 统计购买次数 */
    int countAllPurchases();
}
