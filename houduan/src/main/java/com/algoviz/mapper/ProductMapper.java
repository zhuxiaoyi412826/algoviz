package com.algoviz.mapper;

import com.algoviz.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    
    /**
     * 获取所有商品
     */
    List<Product> getAllProducts();
    
    /**
     * 根据商品ID获取商品
     */
    Product getProductById(@Param("productId") String productId);
    
    /**
     * 根据分类获取商品
     */
    List<Product> getProductsByCategory(@Param("category") String category);
    
    /**
     * 插入商品
     */
    int insertProduct(Product product);
    
    /**
     * 更新商品
     */
    int updateProduct(Product product);
    
    /**
     * 删除商品
     */
    int deleteProduct(@Param("productId") String productId);
}
