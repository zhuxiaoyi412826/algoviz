package com.algoviz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/captcha")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Tag(name = "验证码管理", description = "图形验证码接口")
public class CaptchaController {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaController.class);
    private static final String SESSION_CAPTCHA_KEY = "LOGIN_CAPTCHA";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    /** 验证码有效期：5 分钟（存于 HttpSession，校验成功后立即失效，一次一用） */
    private static final long EXPIRY_MS = 5 * 60 * 1000L;

    /**
     * 按用途区分 Session 槽位。
     *
     * <p>登录页同时存在「账号密码」和「邮箱」两个 Tab，各有独立的图形验证码输入框；
     * 若共用同一个 key，后生成的一次会覆盖前一次 —— 页面上显示的是 A 的图片，
     * Session 里存的却是 B 的码，用户照着图输也必然报「验证码错误或已过期」。</p>
     *
     * @param type 用途标识（如 account / email）；为空表示单验证码页面的默认槽位
     */
    private static String captchaKey(String type) {
        return (type == null || type.isEmpty()) ? SESSION_CAPTCHA_KEY : SESSION_CAPTCHA_KEY + "_" + type;
    }

    private static final String[] FONTS = {
            "Arial", "Times New Roman", "Courier New",
            "Verdana", "Georgia", "Tahoma",
            "SimHei", "Microsoft YaHei"
    };

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Random RANDOM = new Random();

    @GetMapping("/generate")
    @Operation(summary = "生成图形验证码", description = "返回Base64编码的验证码图片，验证码存入Session；type 用于同一页面多验证码场景（account/email）")
    public Map<String, Object> generateCaptcha(@RequestParam(value = "type", required = false) String type,
                                               HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String code = generateRandomCode(CODE_LENGTH);
            BufferedImage image = generateCaptchaImage(code);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            Map<String, Object> captchaData = new HashMap<>();
            captchaData.put("code", code);
            captchaData.put("expiry", System.currentTimeMillis() + EXPIRY_MS);
            request.getSession().setAttribute(captchaKey(type), captchaData);

            result.put("success", true);
            result.put("image", "data:image/png;base64," + base64Image);

        } catch (IOException e) {
            logger.error("生成验证码失败", e);
            result.put("success", false);
            result.put("message", "生成验证码失败");
        }

        return result;
    }

    @PostMapping("/verify")
    @Operation(summary = "校验验证码", description = "校验用户输入的验证码是否正确；type 与生成时保持一致")
    public Map<String, Object> verifyCaptcha(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String userInput = body.get("captcha");
        String captchaKey = captchaKey(body.get("type"));

        if (userInput == null || userInput.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入验证码");
            return result;
        }

        Object captchaObj = request.getSession().getAttribute(captchaKey);
        if (captchaObj == null) {
            result.put("success", false);
            result.put("message", "验证码已过期，请刷新");
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> captchaData = (Map<String, Object>) captchaObj;
        String correctCode = (String) captchaData.get("code");
        Long expiry = (Long) captchaData.get("expiry");

        if (System.currentTimeMillis() > expiry) {
            request.getSession().removeAttribute(captchaKey);
            result.put("success", false);
            result.put("message", "验证码已过期，请刷新");
            return result;
        }

        if (userInput.trim().equalsIgnoreCase(correctCode)) {
            request.getSession().removeAttribute(captchaKey);
            result.put("success", true);
        } else {
            result.put("success", false);
            result.put("message", "验证码错误");
        }

        return result;
    }

    /**
     * 校验默认槽位的验证码（单验证码页面：注册、找回密码、邮箱密码登录等）
     */
    public static boolean verifyCaptchaInSession(String userInput, HttpServletRequest request) {
        return verifyCaptchaInSession(userInput, request, null);
    }

    /**
     * 校验指定用途槽位的验证码（登录页账号/邮箱两个 Tab 各用各的 key）
     *
     * @param type 与 /api/captcha/generate 的 type 保持一致
     */
    @SuppressWarnings("unchecked")
    public static boolean verifyCaptchaInSession(String userInput, HttpServletRequest request, String type) {
        String key = captchaKey(type);
        Object captchaObj = request.getSession().getAttribute(key);
        // 兼容未带 type 的调用（旧版页面 JS 生成时未区分槽位）：
        // 专用槽位为空则回退到默认槽位，避免「新版后端 + 旧版页面」时验证码必然校验失败
        if (captchaObj == null && type != null && !type.isEmpty()) {
            key = SESSION_CAPTCHA_KEY;
            captchaObj = request.getSession().getAttribute(key);
        }
        if (captchaObj == null) return false;

        Map<String, Object> captchaData = (Map<String, Object>) captchaObj;
        String correctCode = (String) captchaData.get("code");
        Long expiry = (Long) captchaData.get("expiry");

        if (System.currentTimeMillis() > expiry) {
            request.getSession().removeAttribute(key);
            return false;
        }

        boolean valid = userInput != null && userInput.trim().equalsIgnoreCase(correctCode);
        if (valid) {
            request.getSession().removeAttribute(key);
        }
        return valid;
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private BufferedImage generateCaptchaImage(String code) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(240, 243, 248));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawNoiseDots(g);
        drawNoiseLines(g);

        g.setFont(new Font(FONTS[RANDOM.nextInt(FONTS.length)], Font.BOLD, 26));

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int x = 18 + i * 24;
            int y = 30 + RANDOM.nextInt(4) - 2;

            g.setColor(new Color(RANDOM.nextInt(80), RANDOM.nextInt(80), RANDOM.nextInt(80)));

            double theta = (RANDOM.nextDouble() - 0.5) * 0.6;
            g.rotate(theta, x, y);
            g.drawString(String.valueOf(c), x, y);
            g.rotate(-theta, x, y);
        }

        g.dispose();
        return image;
    }

    private void drawNoiseDots(Graphics2D g) {
        for (int i = 0; i < 30; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200), 120));
            g.fillOval(x, y, 2, 2);
        }
    }

    private void drawNoiseLines(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(WIDTH);
            int y2 = RANDOM.nextInt(HEIGHT);
            g.setColor(new Color(RANDOM.nextInt(180), RANDOM.nextInt(180), RANDOM.nextInt(180), 80));
            g.drawLine(x1, y1, x2, y2);
        }
    }
}
