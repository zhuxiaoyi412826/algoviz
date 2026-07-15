package com.algoviz.service.impl;

import com.algoviz.config.WechatPayConfig;
import com.algoviz.dto.CreateOrderResponse;
import com.algoviz.dto.QueryOrderResponse;
import com.algoviz.entity.Order;
import com.algoviz.entity.Product;
import com.algoviz.mapper.OrderMapper;
import com.algoviz.mapper.ProductMapper;
import com.algoviz.service.PaymentService;
import com.algoviz.util.WechatPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private WechatPayConfig wechatPayConfig;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    private PrivateKey privateKey;

    /**
     * 初始化时加载私钥
     */
    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            try {
                String privateKeyPath = wechatPayConfig.getPrivateKeyPath();
                logger.info("尝试加载私钥，路径：{}", privateKeyPath);
                String privateKeyStr;

                // 从文件系统或类路径加载私钥
                if (privateKeyPath.startsWith("classpath:")) {
                    logger.info("使用类路径加载私钥");
                    Resource resource = new ClassPathResource(privateKeyPath.substring(10));
                    if (!resource.exists()) {
                        logger.error("私钥文件不存在于类路径: {}", privateKeyPath);
                        return null;
                    }
                    logger.info("私钥文件存在，开始读取");
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                        privateKeyStr = reader.lines().collect(Collectors.joining("\n"));
                    }
                    logger.info("私钥文件读取完成，长度: {} 字符", privateKeyStr.length());
                } else {
                    logger.info("使用文件系统路径加载私钥");
                    java.nio.file.Path path = java.nio.file.Paths.get(privateKeyPath);
                    if (!java.nio.file.Files.exists(path)) {
                        logger.error("私钥文件不存在: {}", path.toAbsolutePath());
                        return null;
                    }
                    privateKeyStr = java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
                }

                if (privateKeyStr == null || privateKeyStr.isEmpty()) {
                    logger.error("私钥内容为空");
                    return null;
                }

                // 检查私钥格式
                if (!privateKeyStr.contains("-----BEGIN PRIVATE KEY-----")) {
                    logger.warn("私钥格式可能不正确，缺少 BEGIN PRIVATE KEY 标记");
                }

                privateKey = WechatPayUtil.loadPrivateKey(privateKeyStr);
                logger.info("微信支付私钥加载成功");
            } catch (Exception e) {
                logger.error("加载微信支付私钥失败: {}", e.getMessage(), e);
                return null;
            }
        }
        return privateKey;
    }

    /**
     * 检查微信支付配置是否完整
     */
    private boolean isWechatPayConfigValid() {
        logger.info("========== 检查微信支付配置 ==========");
        
        boolean valid = true;
        
        // 检查商户号
        if (wechatPayConfig.getMchId() == null || wechatPayConfig.getMchId().isEmpty()) {
            logger.error("微信支付配置缺失：商户号(mch-id)为空");
            valid = false;
        } else {
            logger.info("商户号(mch-id): {}", wechatPayConfig.getMchId());
        }
        
        // 检查证书序列号
        if (wechatPayConfig.getCertSerialNo() == null || wechatPayConfig.getCertSerialNo().isEmpty()) {
            logger.error("微信支付配置缺失：证书序列号(cert-serial-no)为空");
            valid = false;
        } else {
            logger.info("证书序列号(cert-serial-no): {}", wechatPayConfig.getCertSerialNo());
        }
        
        // 检查API V3密钥
        if (wechatPayConfig.getApiV3Key() == null || wechatPayConfig.getApiV3Key().isEmpty()) {
            logger.error("微信支付配置缺失：API V3密钥(api-v3-key)为空");
            valid = false;
        } else {
            logger.info("API V3密钥(api-v3-key): {}", "******");
        }
        
        // 检查私钥路径
        if (wechatPayConfig.getPrivateKeyPath() == null || wechatPayConfig.getPrivateKeyPath().isEmpty()) {
            logger.error("微信支付配置缺失：私钥路径(private-key-path)为空");
            valid = false;
        } else {
            logger.info("私钥路径(private-key-path): {}", wechatPayConfig.getPrivateKeyPath());
        }
        
        // 检查私钥是否能正常加载
        PrivateKey key = getPrivateKey();
        if (key == null) {
            logger.error("微信支付配置无效：私钥加载失败");
            valid = false;
        } else {
            logger.info("私钥加载状态: 成功");
        }
        
        // 检查证书路径
        if (wechatPayConfig.getCertPath() == null || wechatPayConfig.getCertPath().isEmpty()) {
            logger.warn("微信支付配置警告：证书路径(cert-path)为空（Native支付不需要证书文件）");
        } else {
            logger.info("证书路径(cert-path): {}", wechatPayConfig.getCertPath());
        }
        
        // 检查回调地址
        if (wechatPayConfig.getNotifyUrl() == null || wechatPayConfig.getNotifyUrl().isEmpty()) {
            logger.error("微信支付配置缺失：回调地址(notify-url)为空");
            valid = false;
        } else {
            logger.info("回调地址(notify-url): {}", wechatPayConfig.getNotifyUrl());
        }
        
        logger.info("微信支付配置校验结果: {}", valid ? "有效" : "无效");
        logger.info("======================================");
        
        return valid;
    }

    @Override
    public List<Product> getProductList() {
        logger.info("获取商品列表");
        return productMapper.getAllProducts();
    }

    @Override
    public Product getProduct(String productId) {
        logger.info("获取商品详情：{}", productId);
        return productMapper.getProductById(productId);
    }

    @Override
    public CreateOrderResponse createOrder(String productId, Long userId) {
        logger.info("========== 创建订单开始 ==========");
        logger.info("商品ID：{}，用户ID：{}", productId, userId);

        Product product = productMapper.getProductById(productId);
        if (product == null) {
            logger.error("商品不存在：{}", productId);
            logger.info("========== 创建订单结束(失败) ==========");
            return CreateOrderResponse.fail("商品不存在");
        }
        
        logger.info("商品信息：ID={}, 名称={}, 价格={}分", 
                product.getProductId(), product.getProductName(), product.getPrice());

        // 生成订单号
        String orderId = WechatPayUtil.generateOutTradeNo();
        logger.info("生成订单号：{}", orderId);

        // 创建订单
        Order order = new Order();
        order.setOrderId(orderId);
        order.setProductId(productId);
        order.setProductName(product.getProductName());
        order.setAmount(product.getPrice());
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 插入数据库
        orderMapper.insertOrder(order);
        logger.info("订单已保存到数据库");

        String qrCodeText;

        // 检查是否可以使用真实微信支付
        if (isWechatPayConfigValid()) {
            logger.info("微信支付配置有效，尝试调用微信支付Native API");
            logger.info("请求参数：");
            logger.info("  - 商户号(mchId): {}", wechatPayConfig.getMchId());
            logger.info("  - 证书序列号(serialNo): {}", wechatPayConfig.getCertSerialNo());
            logger.info("  - 回调地址(notifyUrl): {}", wechatPayConfig.getNotifyUrl());
            logger.info("  - 订单号(outTradeNo): {}", orderId);
            logger.info("  - 商品描述(description): {}", product.getProductName());
            logger.info("  - 金额(amount): {}分", product.getPrice());
            
            try {
                // 使用真实微信支付Native支付
                qrCodeText = WechatPayUtil.createNativeOrder(
                        wechatPayConfig.getAppid(),
                        wechatPayConfig.getMchId(),
                        wechatPayConfig.getCertSerialNo(),
                        getPrivateKey(),
                        wechatPayConfig.getNotifyUrl(),
                        orderId,
                        product.getProductName(),
                        product.getPrice() // 金额单位：分
                );
                logger.info("微信支付Native API调用成功");
                logger.info("返回的二维码内容(code_url): {}", qrCodeText);
                logger.info("二维码类型: {}", qrCodeText.startsWith("weixin://") ? "微信支付链接" : "未知类型");
            } catch (Exception e) {
                logger.error("调用微信支付Native API失败，降级使用模拟支付");
                logger.error("错误信息: {}", e.getMessage());
                logger.error("错误堆栈:", e);
                // 降级使用模拟支付
                qrCodeText = WechatPayUtil.generateQRCodeText(
                        productId, product.getProductName(), product.getPrice());
                logger.info("已降级到模拟支付，生成的模拟二维码: {}", qrCodeText);
            }
        } else {
            logger.warn("微信支付配置无效，使用模拟支付");
            // 使用模拟支付
            qrCodeText = WechatPayUtil.generateQRCodeText(
                    productId, product.getProductName(), product.getPrice());
            logger.info("生成模拟二维码: {}", qrCodeText);
        }

        return CreateOrderResponse.success(orderId, qrCodeText,
                product.getProductName(), product.getPrice());
    }

    @Override
    public QueryOrderResponse queryOrder(String orderId) {
        logger.info("查询订单状态：{}", orderId);

        Order order = orderMapper.getOrderById(orderId);
        if (order == null) {
            logger.error("订单不存在：{}", orderId);
            return QueryOrderResponse.fail("订单不存在");
        }

        return QueryOrderResponse.success(order.getStatus(), orderId,
                order.getProductName(), order.getAmount());
    }

    @Override
    public void handlePaymentCallback(String jsonData) {
        logger.info("收到支付回调：{}", jsonData);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonData);

            // 解密回调数据
            com.fasterxml.jackson.databind.JsonNode resource = root.get("resource");
            if (resource != null) {
                String ciphertext = resource.get("ciphertext").asText();
                String nonce = resource.get("nonce").asText();
                String associatedData = resource.has("associated_data") ?
                        resource.get("associated_data").asText() : "";

                String decryptedData = WechatPayUtil.decryptNotifyData(
                        ciphertext, nonce, associatedData, wechatPayConfig.getApiV3Key());

                logger.info("解密后的回调数据：{}", decryptedData);

                com.fasterxml.jackson.databind.JsonNode notifyData = mapper.readTree(decryptedData);
                String outTradeNo = notifyData.get("out_trade_no").asText();
                String transactionId = notifyData.get("transaction_id").asText();
                String tradeState = notifyData.get("trade_state").asText();

                if ("SUCCESS".equals(tradeState)) {
                    // 更新订单状态
                    orderMapper.updateOrderPaymentInfo(outTradeNo, transactionId, transactionId);
                    logger.info("订单支付成功：{}", outTradeNo);
                }
            }
        } catch (Exception e) {
            logger.error("处理支付回调失败", e);
        }
    }

    @Override
    public void mockPayment(String orderId) {
        logger.info("模拟支付成功：{}", orderId);

        Order order = orderMapper.getOrderById(orderId);
        if (order == null) {
            logger.error("订单不存在：{}", orderId);
            return;
        }

        if ("SUCCESS".equals(order.getStatus())) {
            logger.info("订单已支付：{}", orderId);
            return;
        }

        // 更新订单支付信息
        String wechatTradeNo = "WX" + System.currentTimeMillis();
        String wechatTransactionId = "TRX" + System.currentTimeMillis();

        orderMapper.updateOrderPaymentInfo(orderId, wechatTradeNo, wechatTransactionId);
        logger.info("订单支付成功：{}", orderId);
    }
}
