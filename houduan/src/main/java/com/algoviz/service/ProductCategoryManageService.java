package com.algoviz.service;

import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.ProductCategory;
import com.algoviz.mapper.ProductCategoryMapper;
import com.algoviz.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类字典业务（product_category 表）
 * 删除分类 = 有商品占用时拒绝；无占用时物理删除。历史商品的 category 字符串不受影响。
 */
@Service
public class ProductCategoryManageService {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private com.algoviz.mapper.CoinMapper coinMapper;

    public List<ProductCategory> listAll() {
        return categoryMapper.selectAll();
    }

    public List<ProductCategory> listEnabled() {
        return categoryMapper.selectEnabled();
    }

    public ProductCategory get(Long id) {
        ProductCategory c = categoryMapper.findById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return c;
    }

    public ProductCategory create(String name, Integer sort, Integer status) {
        String n = trimName(name);
        if (categoryMapper.findByName(n) != null) {
            throw new BusinessException("分类名称已存在: " + n);
        }
        ProductCategory c = new ProductCategory();
        c.setName(n);
        c.setSort(sort == null ? 0 : sort);
        c.setStatus(status == null || status != 0 ? 1 : 0);
        categoryMapper.insert(c);
        return get(c.getId());
    }

    public ProductCategory update(Long id, String name, Integer sort, Integer status) {
        ProductCategory exist = get(id);
        if (name != null && !name.trim().isEmpty()) {
            String n = name.trim();
            ProductCategory dup = categoryMapper.findByName(n);
            if (dup != null && !dup.getId().equals(id)) {
                throw new BusinessException("分类名称已存在: " + n);
            }
            exist.setName(n);
        }
        if (sort != null) exist.setSort(sort);
        if (status != null) exist.setStatus(status == 0 ? 0 : 1);
        categoryMapper.update(exist);
        return get(id);
    }

    /** 删除分类：钱商品/金币商品仍在使用时拒绝（先调整商品分类再删字典） */
    @Transactional
    public boolean delete(Long id) {
        ProductCategory exist = get(id);
        long usedByProduct = productMapper.countByCategory(exist.getName());
        if (usedByProduct > 0) {
            throw new BusinessException("该分类仍被 " + usedByProduct + " 个钱商品使用，请先调整商品分类");
        }
        long usedByCoin = coinMapper.countCoinProductsByCategory(exist.getName());
        if (usedByCoin > 0) {
            throw new BusinessException("该分类仍被 " + usedByCoin + " 个金币商品使用，请先调整商品分类");
        }
        return categoryMapper.deleteById(id) > 0;
    }

    private String trimName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        return name.trim();
    }
}
