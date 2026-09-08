package com.algoviz.controller;

import com.algoviz.config.GraphqlSaAuthInterceptor;
import com.algoviz.entity.PageResult;
import com.algoviz.entity.Product;
import com.algoviz.entity.ProductCategory;
import com.algoviz.service.ProductCategoryManageService;
import com.algoviz.service.ProductManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * 商品管理 GraphQL 接口（schema: resources/graphql/product.graphqls）
 * 鉴权：/graphql 统一由 {@link GraphqlSaAuthInterceptor} 校验 Sa-Token；
 * 查询/变更标注 @QueryMapping/@MutationMapping，不进入 Knife4j。
 */
@Controller
public class ProductGraphqlController {

    @Autowired
    private ProductManageService productService;

    @Autowired
    private ProductCategoryManageService categoryService;

    @Autowired
    private com.algoviz.service.CoinProductGraphqlService coinProductService;

    @QueryMapping
    public PageResult<Product> products(@Argument String keyword,
                                        @Argument String category,
                                        @Argument String startDate,
                                        @Argument String endDate,
                                        @Argument String orderBy,
                                        @Argument String orderDir,
                                        @Argument int page,
                                        @Argument int size) {
        return productService.page(keyword, category, startDate, endDate, orderBy, orderDir, page, size);
    }

    @QueryMapping
    public Product product(@Argument String id) {
        return productService.get(idOf(id));
    }

    @QueryMapping
    public java.util.List<ProductCategory> categories() {
        return categoryService.listAll();
    }

    @QueryMapping
    public java.util.List<ProductCategory> categoriesEnabled() {
        return categoryService.listEnabled();
    }

    @MutationMapping
    public Product createProduct(@Argument Product input) {
        return productService.create(input);
    }

    @MutationMapping
    public Product updateProduct(@Argument String id, @Argument Product input) {
        return productService.update(idOf(id), input);
    }

    @MutationMapping
    public boolean deleteProduct(@Argument String id) {
        return productService.delete(idOf(id));
    }

    @MutationMapping
    public ProductCategory createCategory(@Argument String name, @Argument Integer sort, @Argument Integer status) {
        return categoryService.create(name, sort, status);
    }

    @MutationMapping
    public ProductCategory updateCategory(@Argument String id, @Argument String name,
                                          @Argument Integer sort, @Argument Integer status) {
        return categoryService.update(idOf(id), name, sort, status);
    }

    @MutationMapping
    public boolean deleteCategory(@Argument String id) {
        return categoryService.delete(idOf(id));
    }

    @QueryMapping
    public PageResult<com.algoviz.service.CoinProductGraphqlService.View> coinProducts(@Argument String keyword,
                                                                                       @Argument String category,
                                                                                       @Argument String status,
                                                                                       @Argument String startDate,
                                                                                       @Argument String endDate,
                                                                                       @Argument int page,
                                                                                       @Argument int size) {
        return coinProductService.page(keyword, category, status, startDate, endDate, page, size);
    }

    @QueryMapping
    public com.algoviz.service.CoinProductGraphqlService.View coinProduct(@Argument String productId) {
        return coinProductService.get(productId);
    }

    @MutationMapping
    public com.algoviz.service.CoinProductGraphqlService.View createCoinProduct(@Argument com.algoviz.entity.CoinProduct input) {
        return coinProductService.create(input);
    }

    @MutationMapping
    public com.algoviz.service.CoinProductGraphqlService.View updateCoinProduct(@Argument String productId,
                                                                                @Argument com.algoviz.entity.CoinProduct input) {
        return coinProductService.update(productId, input);
    }

    @MutationMapping
    public boolean deleteCoinProduct(@Argument String productId) {
        return coinProductService.delete(productId);
    }

    private Long idOf(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new com.algoviz.common.exception.BusinessException("无效的 ID: " + id);
        }
    }
}
