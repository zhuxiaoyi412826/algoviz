package com.algoviz.service;

import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.PageResult;
import com.algoviz.entity.Product;
import com.algoviz.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 商品管理业务（product 表，供 GraphQL 商品管理 CRUD 使用）
 */
@Service
public class ProductManageService {

    /** 排序字段白名单：防止 GraphQL 参数直接拼接 SQL */
    private static final Set<String> ORDER_FIELDS = Set.of("id", "productName", "productId", "price", "category");

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private com.algoviz.mapper.OrderMapper orderMapper;

    /**
     * 分页查询：名称模糊 / 分类精确 / 创建时间范围 / 白名单排序
     */
    public PageResult<Product> page(String keyword, String category, String startDate, String endDate,
                                    String orderBy, String orderDir, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        String safeOrderBy = (orderBy != null && ORDER_FIELDS.contains(orderBy)) ? orderBy : "created_at";
        // 默认新建时间倒序
        String safeOrderDir = "asc".equalsIgnoreCase(orderDir) ? "asc" : "desc";
        long total = productMapper.countByConditions(keyword, category, startDate, endDate);
        List<Product> list = total == 0 ? List.of()
                : productMapper.selectByConditions(keyword, category, startDate, endDate,
                        safeOrderBy, safeOrderDir, (page - 1) * size, size);
        return new PageResult<>(list, (int) total, page, size);
    }

    public Product get(Long id) {
        Product p = productMapper.findById(id);
        if (p == null) throw new BusinessException("商品不存在");
        return p;
    }

    /** 新增商品（product_id 唯一校验） */
    public Product create(Product input) {
        validate(input);
        if (productMapper.findByProductId(input.getProductId()) != null) {
            throw new BusinessException("商品编号已存在: " + input.getProductId());
        }
        productMapper.insert(input);
        return get(input.getId());
    }

    /** 修改商品（product_id 唯一校验需排除自身） */
    @Transactional
    public Product update(Long id, Product input) {
        Product exist = get(id);
        validate(input);
        Product dup = productMapper.findByProductId(input.getProductId());
        if (dup != null && !dup.getId().equals(id)) {
            throw new BusinessException("商品编号已存在: " + input.getProductId());
        }
        input.setId(id);
        input.setCreatedAt(exist.getCreatedAt());
        productMapper.update(input);
        return get(id);
    }

    /** 删除商品：被订单引用时拒绝物理删除，避免破坏历史订单的商品快照 */
    @Transactional
    public boolean delete(Long id) {
        Product exist = get(id);
        long orders = orderMapper.countByProduct(exist.getProductId(), exist.getId());
        if (orders > 0) {
            throw new BusinessException("该商品已被 " + orders + " 笔订单引用，为保留订单历史请勿删除（可停售处理）");
        }
        return productMapper.deleteById(id) > 0;
    }

    private void validate(Product p) {
        if (p == null) throw new BusinessException("商品信息不能为空");
        if (p.getProductName() == null || p.getProductName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (p.getProductId() == null || p.getProductId().trim().isEmpty()) {
            throw new BusinessException("商品编号不能为空");
        }
        if (p.getPrice() == null || p.getPrice() < 0) {
            throw new BusinessException("价格必须 >= 0（单位：分）");
        }
        p.setProductName(p.getProductName().trim());
        p.setProductId(p.getProductId().trim());
        p.setCategory(p.getCategory() == null ? "" : p.getCategory().trim());
        String url = p.getMaterialUrl();
        p.setMaterialUrl(url == null || url.isBlank() ? null : url.trim());
    }
}
