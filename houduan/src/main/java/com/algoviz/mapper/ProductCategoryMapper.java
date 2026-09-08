package com.algoviz.mapper;

import com.algoviz.entity.ProductCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品分类字典 Mapper（product_category 表）
 */
@Mapper
public interface ProductCategoryMapper {

    /** 全部分类：启用优先 + sort 升序 */
    @Select("SELECT * FROM product_category ORDER BY status DESC, sort ASC, id ASC")
    List<ProductCategory> selectAll();

    /** 仅启用分类（商品页下拉用） */
    @Select("SELECT * FROM product_category WHERE status = 1 ORDER BY sort ASC, id ASC")
    List<ProductCategory> selectEnabled();

    @Select("SELECT * FROM product_category WHERE id = #{id}")
    ProductCategory findById(@Param("id") Long id);

    @Select("SELECT * FROM product_category WHERE name = #{name}")
    ProductCategory findByName(@Param("name") String name);

    @Insert("INSERT INTO product_category (name, sort, status, created_at, updated_at) " +
            "VALUES (#{name}, #{sort}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductCategory category);

    @Update("UPDATE product_category SET name = #{name}, sort = #{sort}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int update(ProductCategory category);

    @Delete("DELETE FROM product_category WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
