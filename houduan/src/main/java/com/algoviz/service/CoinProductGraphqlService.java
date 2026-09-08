package com.algoviz.service;

import com.algoviz.common.exception.BusinessException;
import com.algoviz.entity.CoinProduct;
import com.algoviz.entity.PageResult;
import com.algoviz.mapper.CoinMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 金币商品 GraphQL 业务（coin_product 表；写入复用 CoinMapper XML，与旧 REST 管理页同源）
 * 与“钱商品”(product 表) 并列，页面上以「商品类型」区分。
 */
@Service
public class CoinProductGraphqlService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CoinMapper coinMapper;

    /** GraphQL 输出视图（时间转字符串，避免 LocalDateTime 直接序列化） */
    @Data
    public static class View {
        private Long id;
        private String productId;
        private String productName;
        private String description;
        private Integer coinPrice;
        private String category;
        private String icon;
        private String status;
        private String createdAt;
        private String updatedAt;
    }

    public PageResult<View> page(String keyword, String category, String status, String startDate,
                                 String endDate, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        long total = coinMapper.countSearchCoinProducts(keyword, category, status, startDate, endDate);
        List<CoinProduct> rows = total == 0 ? List.of()
                : coinMapper.searchCoinProducts(keyword, category, status, startDate, endDate,
                        (page - 1) * size, size);
        List<View> list = new ArrayList<>(rows.size());
        for (CoinProduct p : rows) {
            list.add(toView(p));
        }
        return new PageResult<>(list, (int) total, page, size);
    }

    public View get(String productId) {
        CoinProduct p = coinMapper.getProductById(productId);
        if (p == null) throw new BusinessException("金币商品不存在");
        return toView(p);
    }

    /** 新增金币商品（product_id 唯一） */
    public View create(CoinProduct input) {
        validate(input);
        if (coinMapper.getProductById(input.getProductId()) != null) {
            throw new BusinessException("商品编号已存在: " + input.getProductId());
        }
        if (input.getStatus() == null || input.getStatus().isBlank()) {
            input.setStatus("ACTIVE");
        }
        coinMapper.insertProduct(input);
        return get(input.getProductId());
    }

    /** 修改金币商品（编号不可改，按 product_id 更新） */
    @Transactional
    public View update(String productId, CoinProduct input) {
        if (coinMapper.getProductById(productId) == null) {
            throw new BusinessException("金币商品不存在");
        }
        validate(input);
        input.setProductId(productId);
        if (input.getStatus() == null || input.getStatus().isBlank()) {
            input.setStatus("ACTIVE");
        }
        coinMapper.updateProduct(input);
        return get(productId);
    }

    /** 删除金币商品：存在购买记录时拒绝（建议改为下架，保留历史） */
    @Transactional
    public boolean delete(String productId) {
        if (coinMapper.getProductById(productId) == null) {
            throw new BusinessException("金币商品不存在");
        }
        long bought = coinMapper.countPurchasesByProductId(productId);
        if (bought > 0) {
            throw new BusinessException("该金币商品已有 " + bought + " 条购买记录，为保留历史请改为「下架」而非删除");
        }
        return coinMapper.deleteProduct(productId) > 0;
    }

    private void validate(CoinProduct p) {
        if (p.getProductName() == null || p.getProductName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (p.getProductId() == null || p.getProductId().trim().isEmpty()) {
            throw new BusinessException("商品编号不能为空");
        }
        if (p.getCoinPrice() == null || p.getCoinPrice() < 0) {
            throw new BusinessException("硬币价格必须 >= 0");
        }
        String s = p.getStatus();
        if (s != null && !Set.of("ACTIVE", "INACTIVE").contains(s)) {
            throw new BusinessException("状态仅支持 ACTIVE(上架) / INACTIVE(下架)");
        }
        p.setProductName(p.getProductName().trim());
        p.setProductId(p.getProductId().trim());
    }

    private View toView(CoinProduct p) {
        View v = new View();
        v.setId(p.getId());
        v.setProductId(p.getProductId());
        v.setProductName(p.getProductName());
        v.setDescription(p.getDescription());
        v.setCoinPrice(p.getCoinPrice());
        v.setCategory(p.getCategory());
        v.setIcon(p.getIcon());
        v.setStatus(p.getStatus());
        v.setCreatedAt(fmt(p.getCreatedAt()));
        v.setUpdatedAt(fmt(p.getUpdatedAt()));
        return v;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }
}
