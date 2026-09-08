package com.algoviz.mapper;

import com.algoviz.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品 Mapper（product 表）
 * 分页/筛选/排序全部在 SQL 内完成；orderBy/orderDir 由服务层白名单校验后传入
 */
@Mapper
public interface ProductMapper {

    @Select("<script>" +
            "SELECT * FROM product WHERE 1=1" +
            "<if test='keyword != null and keyword != \"\"'> AND product_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "<if test='startDate != null and startDate != \"\"'> AND created_at &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            " ORDER BY " +
            "<choose>" +
            "  <when test='orderBy == \"productName\"'>product_name</when>" +
            "  <when test='orderBy == \"productId\"'>product_id</when>" +
            "  <when test='orderBy == \"price\"'>price</when>" +
            "  <otherwise>created_at</otherwise>" +
            "</choose> " +
            "<choose><when test='orderDir == \"asc\"'>ASC</when><otherwise>DESC</otherwise></choose>" +
            " LIMIT #{offset}, #{size}" +
            "</script>")
    List<Product> selectByConditions(@Param("keyword") String keyword,
                                     @Param("category") String category,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate,
                                     @Param("orderBy") String orderBy,
                                     @Param("orderDir") String orderDir,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM product WHERE 1=1" +
            "<if test='keyword != null and keyword != \"\"'> AND product_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "<if test='startDate != null and startDate != \"\"'> AND created_at &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "</script>")
    long countByConditions(@Param("keyword") String keyword,
                           @Param("category") String category,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product findById(@Param("id") Long id);

    /** 全部商品（XML: mappers/ProductMapper.xml → getAllProducts，支付/前台使用） */
    List<Product> getAllProducts();

    /** 按商品编号查（XML: mappers/ProductMapper.xml → getProductById，支付下单使用） */
    Product getProductById(@Param("productId") String productId);

    @Select("SELECT * FROM product WHERE product_id = #{productId}")
    Product findByProductId(@Param("productId") String productId);

    @Select("SELECT COUNT(*) FROM product WHERE category = #{category}")
    long countByCategory(@Param("category") String category);

    @Insert("INSERT INTO product (product_id, product_name, description, price, category, icon, material_url, created_at, updated_at) " +
            "VALUES (#{productId}, #{productName}, #{description}, #{price}, #{category}, #{icon}, #{materialUrl}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("UPDATE product SET product_id = #{productId}, product_name = #{productName}, description = #{description}, " +
            "price = #{price}, category = #{category}, icon = #{icon}, material_url = #{materialUrl}, updated_at = NOW() WHERE id = #{id}")
    int update(Product product);

    @Delete("DELETE FROM product WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
