package com.algoviz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class WechatPayUtil {

    private static final Logger logger = LoggerFactory.getLogger(WechatPayUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 微信支付Native支付接口地址
     */
    private static final String NATIVE_PAY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/native";

    /**
     * 生成随机字符串
     */
    public static String generateNonceStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成商户订单号
     */
    public static String generateOutTradeNo() {
        return "ALG" + System.currentTimeMillis();
    }

    /**
     * 构造V3签名
     */
    public static String buildSignature(String method, String url, String body,
                                        String nonceStr, long timestamp,
                                        PrivateKey privateKey) throws Exception {
        String signStr = method + "\n" +
                        url + "\n" +
                        timestamp + "\n" +
                        nonceStr + "\n" +
                        (body != null ? body : "") + "\n";

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(signStr.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 构造Authorization头部
     */
    public static String buildAuthorization(String mchId, String serialNo,
                                           String nonceStr, long timestamp,
                                           String signature) {
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + mchId +
                "\",nonce_str=\"" + nonceStr +
                "\",timestamp=\"" + timestamp +
                "\",serial_no=\"" + serialNo +
                "\",signature=\"" + signature + "\"";
    }

    /**
     * 从私钥字符串加载PrivateKey
     */
    public static PrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
        // 去除PEM格式的头部和尾部
        privateKeyStr = privateKeyStr
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 解密回调通知数据
     */
    public static String decryptNotifyData(String ciphertext, String nonce,
                                            String associatedData,
                                            String apiV3Key) throws Exception {
        byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);
        byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
        byte[] aadBytes = associatedData != null ?
                            associatedData.getBytes(StandardCharsets.UTF_8) : new byte[0];

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonceBytes);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
        cipher.updateAAD(aadBytes);

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 创建微信支付Native订单，获取支付二维码链接
     *
     * @param appid 公众号ID/小程序ID
     * @param mchId 商户号
     * @param serialNo 证书序列号
     * @param privateKey 商户私钥
     * @param notifyUrl 回调地址
     * @param outTradeNo 商户订单号
     * @param description 商品描述
     * @param amount 金额（单位：分）
     * @return 支付二维码链接 code_url
     */
    public static String createNativeOrder(String appid, String mchId, String serialNo, PrivateKey privateKey,
                                          String notifyUrl, String outTradeNo,
                                          String description, int amount) throws Exception {
        logger.info("========== 调用微信支付Native API开始 ==========");
        logger.info("API地址: {}", NATIVE_PAY_URL);
        logger.info("公众号ID(appid): {}", appid);
        logger.info("商户号(mchId): {}", mchId);
        logger.info("证书序列号(serialNo): {}", serialNo);
        logger.info("回调地址(notifyUrl): {}", notifyUrl);
        logger.info("订单号(outTradeNo): {}", outTradeNo);
        logger.info("商品描述(description): {}", description);
        logger.info("金额(amount): {}分", amount);

        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("appid", appid);
        requestBody.put("mchid", mchId);
        requestBody.put("out_trade_no", outTradeNo);
        requestBody.put("description", description);
        requestBody.put("notify_url", notifyUrl);

        ObjectNode amountNode = objectMapper.createObjectNode();
        amountNode.put("total", amount);
        requestBody.set("amount", amountNode);

        String body = requestBody.toString();
        logger.info("请求体(body): {}", body);

        String nonceStr = generateNonceStr();
        long timestamp = System.currentTimeMillis() / 1000;
        logger.info("随机字符串(nonce_str): {}", nonceStr);
        logger.info("时间戳(timestamp): {}", timestamp);

        // 构建签名
        logger.info("开始构建签名...");
        String signature = buildSignature("POST", "/v3/pay/transactions/native", 
                                         body, nonceStr, timestamp, privateKey);
        logger.info("签名成功，签名长度: {} 字符", signature.length());

        // 构建Authorization头
        String authorization = buildAuthorization(mchId, serialNo, nonceStr, timestamp, signature);
        logger.info("Authorization头构建完成");

        // 发送请求
        logger.info("开始发送HTTP请求...");
        URL url = new URL(NATIVE_PAY_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        logger.info("请求已发送");

        int responseCode = conn.getResponseCode();
        logger.info("HTTP响应码: {}", responseCode);

        BufferedReader reader;
        if (responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        logger.info("响应内容: {}", response.toString());

        if (responseCode == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            String codeUrl = jsonResponse.get("code_url").asText();
            logger.info("获取到支付二维码链接(code_url): {}", codeUrl);
            logger.info("二维码链接长度: {} 字符", codeUrl.length());
            logger.info("二维码类型: {}", codeUrl.startsWith("weixin://") ? "微信支付链接(有效)" : "未知类型(可能无效)");
            logger.info("========== 调用微信支付Native API成功 ==========");
            return codeUrl;
        } else {
            logger.error("微信支付Native API调用失败");
            logger.error("HTTP状态码: {}", responseCode);
            logger.error("错误响应: {}", response.toString());
            logger.info("========== 调用微信支付Native API失败 ==========");
            throw new RuntimeException("微信支付请求失败, HTTP状态码: " + responseCode + ", 响应: " + response.toString());
        }
    }

    /**
     * 生成二维码内容（模拟支付）
     * 用于测试环境，当微信支付配置不完整时使用
     */
    public static String generateQRCodeText(String productId, String productName,
                                            int amount) {
        return "wechat://pay/product/" + productId + "/amount/" + amount;
    }

    /**
     * 生成H5支付链接
     * 用于微信外浏览器支付
     */
    public static String createH5Order(String mchId, String serialNo, PrivateKey privateKey,
                                      String notifyUrl, String outTradeNo,
                                      String description, int amount, String clientIp) throws Exception {
        String h5PayUrl = "https://api.mch.weixin.qq.com/v3/pay/transactions/h5";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("mchid", mchId);
        requestBody.put("out_trade_no", outTradeNo);
        requestBody.put("description", description);
        requestBody.put("notify_url", notifyUrl);

        ObjectNode amountNode = objectMapper.createObjectNode();
        amountNode.put("total", amount);
        requestBody.set("amount", amountNode);

        ObjectNode sceneNode = objectMapper.createObjectNode();
        ObjectNode h5InfoNode = objectMapper.createObjectNode();
        h5InfoNode.put("type", "Wap");
        h5InfoNode.put("app_name", "AlgoViz");
        h5InfoNode.put("app_url", "https://dsaol.asia");
        sceneNode.set("h5_info", h5InfoNode);
        requestBody.set("scene_info", sceneNode);

        String body = requestBody.toString();
        String nonceStr = generateNonceStr();
        long timestamp = System.currentTimeMillis() / 1000;

        String signature = buildSignature("POST", "/v3/pay/transactions/h5",
                                         body, nonceStr, timestamp, privateKey);

        String authorization = buildAuthorization(mchId, serialNo, nonceStr, timestamp, signature);

        URL url = new URL(h5PayUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            return jsonResponse.get("h5_url").asText();
        } else {
            throw new RuntimeException("微信支付H5请求失败: " + response.toString());
        }
    }
}
