package com.algoviz.controller;

import com.algoviz.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/wx")
@Tag(name = "微信接口", description = "微信公众号相关接口")
public class WechatController {

    @Value("${wechat.token:algoviz_token}")
    private String token;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.appsecret}")
    private String appsecret;

    @Autowired
    private LoginService loginService;

    /**
     * 微信公众号服务器验证接口
     * 微信服务器会发送GET请求验证服务器地址
     * 在微信公众平台配置时，需要填写此URL作为服务器地址
     */
    @GetMapping("/callback")
    @Operation(summary = "微信回调验证", description = "用于微信公众平台验证服务器地址")
    public String verify(
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {

        System.out.println("收到微信验证请求: ");
        System.out.println("signature: " + signature);
        System.out.println("timestamp: " + timestamp);
        System.out.println("nonce: " + nonce);
        System.out.println("echostr: " + echostr);
        System.out.println("token value: " + token);

        if (checkSignature(signature, timestamp, nonce)) {
            System.out.println("验证成功!");
            return echostr;
        }
        System.out.println("验证失败!");
        return "验证失败";
    }

    /**
     * 接收微信公众号推送的消息
     * 微信服务器会发送POST请求推送用户消息
     */
    @PostMapping(value = "/callback", produces = "application/xml;charset=UTF-8")
    @Operation(summary = "接收微信消息", description = "接收微信公众号推送的用户消息")
    public String handleMessage(
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestBody String requestBody) {

        // 验证请求来源
        if (!checkSignature(signature, timestamp, nonce)) {
            return "error";
        }

        // 处理消息并返回响应
        return processMessage(requestBody);
    }

    /**
     * 验证微信签名的方法
     */
    private boolean checkSignature(String signature, String timestamp, String nonce) {
        if (signature == null || timestamp == null || nonce == null) {
            return false;
        }

        System.out.println("token value: " + token);
        
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);

        StringBuilder content = new StringBuilder();
        for (String s : arr) {
            content.append(s);
        }
        System.out.println("checkSignature str: " + content.toString());

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            String tmpStr = byteToStr(digest);
            System.out.println("checkSignature calculated: " + tmpStr);
            System.out.println("checkSignature expected: " + (signature != null ? signature.toUpperCase() : "null"));
            return tmpStr != null && tmpStr.equals(signature.toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String byteToStr(byte[] byteArray) {
        StringBuilder strDigest = new StringBuilder();
        for (byte b : byteArray) {
            strDigest.append(byteToHex(b));
        }
        return strDigest.toString();
    }

    /**
     * 将字节转换为十六进制字符串
     */
    private String byteToHex(byte b) {
        char[] digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] tempChar = {digit[(b >>> 4) & 0x0F], digit[b & 0x0F]};
        return new String(tempChar);
    }

    /**
     * 处理接收到的消息
     */
    private String processMessage(String xml) {
        // 简单正则解析XML，实际项目中建议使用 dom4j 等库
        String toUserName = extractXmlNode(xml, "ToUserName");
        String fromUserName = extractXmlNode(xml, "FromUserName");
        String msgType = extractXmlNode(xml, "MsgType");
        String content = extractXmlNode(xml, "Content");

        if ("text".equals(msgType)) {
            if (content != null) {
                content = content.trim();
                // 如果是6位数字验证码，尝试验证登录
                if (content.matches("\\d{6}")) {
                    boolean success = loginService.verifyCodeFromWechat(content, fromUserName);
                    if (success) {
                        return buildTextMessage(fromUserName, toUserName, "登录成功！欢迎来到AlgoViz数据结构与算法可视化学习平台。");
                    } else {
                        return buildTextMessage(fromUserName, toUserName, "验证码无效或已过期，请在网页上刷新验证码后重试。");
                    }
                }
            }
            return buildTextMessage(fromUserName, toUserName, "您可以输入网页上显示的6位数字验证码进行登录。");
        }

        if ("event".equals(msgType)) {
            String event = extractXmlNode(xml, "Event");
            if ("subscribe".equals(event)) {
                return buildTextMessage(fromUserName, toUserName, "感谢关注AlgoViz！\n请输入网页上的6位数字验证码完成登录。");
            }
        }

        return "success";
    }

    private String extractXmlNode(String xml, String nodeName) {
        Pattern pattern = Pattern.compile("<" + nodeName + "><!\\[CDATA\\[(.*?)\\]\\]></" + nodeName + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        pattern = Pattern.compile("<" + nodeName + ">(.*?)</" + nodeName + ">");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 构建文本消息XML
     */
    private String buildTextMessage(String toUserName, String fromUserName, String content) {
        long createTime = System.currentTimeMillis() / 1000;
        return "<xml>\n" +
                "<ToUserName><![CDATA[" + toUserName + "]]></ToUserName>\n" +
                "<FromUserName><![CDATA[" + fromUserName + "]]></FromUserName>\n" +
                "<CreateTime>" + createTime + "</CreateTime>\n" +
                "<MsgType><![CDATA[text]]></MsgType>\n" +
                "<Content><![CDATA[" + content + "]]></Content>\n" +
                "</xml>";
    }

    /**
     * 获取微信配置信息（用于前端）
     */
    @GetMapping("/config")
    @Operation(summary = "获取微信配置", description = "获取微信公众号配置信息")
    public Map<String, String> getWechatConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("appid", appid);
        config.put("appsecret", appsecret);
        return config;
    }
}
