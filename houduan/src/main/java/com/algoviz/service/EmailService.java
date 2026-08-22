package com.algoviz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final long CODE_EXPIRY_MINUTES = 5;
    private static final String CODE_KEY_PREFIX = "algoviz:email:code:";
    private static final String SEND_LIMIT_KEY_PREFIX = "algoviz:email:send:limit:";
    private static final long SEND_LIMIT_SECONDS = 60;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public boolean sendVerificationCode(String toEmail) {
        // 检查发送频率限制
        String sendLimitKey = SEND_LIMIT_KEY_PREFIX + toEmail;
        Boolean canSend = redisTemplate.opsForValue().setIfAbsent(sendLimitKey, "1", SEND_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (canSend == null || !canSend) {
            logger.warn("邮箱验证码发送过于频繁: {}", toEmail);
            return false;
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【AlgoViz】邮箱验证码");
            message.setText("您的验证码是：" + code + "，5分钟内有效。请勿泄露给他人。");

            mailSender.send(message);

            // 存储验证码到Redis，5分钟过期（再次获取会自动覆盖旧验证码）
            String codeKey = CODE_KEY_PREFIX + toEmail;
            redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);

            logger.info("验证码已发送到: {}", toEmail);
            return true;

        } catch (Exception e) {
            logger.error("发送邮件失败: {}", toEmail, e);
            // 发送失败时移除频率限制，允许重试
            redisTemplate.delete(sendLimitKey);
            return false;
        }
    }

    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;

        String codeKey = CODE_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            logger.warn("邮箱验证码不存在或已过期: {}", email);
            return false;
        }

        boolean valid = code.equals(storedCode);
        if (valid) {
            redisTemplate.delete(codeKey);
            logger.info("邮箱验证码验证成功: {}", email);
        } else {
            logger.warn("邮箱验证码错误: {}", email);
        }
        return valid;
    }

    public long getSendIntervalRemaining(String email) {
        String sendLimitKey = SEND_LIMIT_KEY_PREFIX + email;
        Long remaining = redisTemplate.getExpire(sendLimitKey, TimeUnit.SECONDS);
        return remaining != null && remaining > 0 ? remaining : 0;
    }

    public boolean sendNotificationEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【AlgoViz】" + subject);
            message.setText(content);

            mailSender.send(message);
            logger.info("通知邮件已发送到: {}, 主题: {}", toEmail, subject);
            return true;
        } catch (Exception e) {
            logger.error("发送通知邮件失败: {}", toEmail, e);
            return false;
        }
    }
}
