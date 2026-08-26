package com.algoviz.common.util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 三层密码加密工具
 *  - 前台普通用户：MD5（32位hex）
 *  - 后台普通管理员（sys_user.role_level >= 2）：BCrypt（$2a$...，VARCHAR(60)）
 *  - 超级管理员（algovize，id=1 / role_code=SUPER_ADMIN）：Argon2id（VARCHAR(128)）
 */
@Component
public class PasswordEncoderUtil {

    // ======================= 前台用户：MD5 =======================

    public static String md5Encode(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 加密失败", e);
        }
    }

    public static boolean md5Matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null) return false;
        return encoded.equalsIgnoreCase(md5Encode(rawPassword));
    }

    // ======================= 后台普通管理员：BCrypt =======================

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    public static String bcryptEncode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    public static boolean bcryptMatches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null) return false;
        try {
            return BCRYPT.matches(rawPassword, encoded);
        } catch (Exception e) {
            return false;
        }
    }

    // ======================= 超级管理员：Argon2id =======================

    // Argon2id 参数：内存 16MB、并行度 1、迭代 2、输出 32 字节 + salt 16 字节
    // 实现：argon2-jvm（JNA 绑定 C 参考实现），比原 BouncyCastle 纯 Java 快 3~5 倍，
    //       哈希字符串格式与旧存储完全兼容：$argon2id$v=19$m=16384,t=2,p=1$salt$hash
    private static final Argon2Factory.Argon2Types ARGON2_TYPE = Argon2Factory.Argon2Types.ARGON2id;
    private static final int ARGON2_ITERATIONS = 2;
    private static final int ARGON2_MEMORY = 16384;      // 16 MB（KiB）
    private static final int ARGON2_PARALLELISM = 1;

    /**
     * Argon2id 加密，返回标准编码字符串：
     * 格式：$argon2id$v=19$m=16384,t=2,p=1${saltBase64}${hashBase64}
     */
    public static String argon2Encode(String rawPassword) {
        Argon2 argon2 = Argon2Factory.create(ARGON2_TYPE);
        char[] pwd = rawPassword.toCharArray();
        try {
            return argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM, pwd);
        } finally {
            argon2.wipeArray(pwd);
        }
    }

    public static boolean argon2Matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null || !encoded.startsWith("$argon2id$")) {
            return false;
        }
        Argon2 argon2 = null;
        try {
            argon2 = Argon2Factory.create(ARGON2_TYPE);
            return argon2.verify(encoded, rawPassword.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }

    // ======================= 统一入口（按角色类型自动选择） =======================

    public enum UserType {
        FRONT_USER,       // 前台普通用户 → MD5
        ADMIN_NORMAL,     // 后台普通管理员 → BCrypt
        ADMIN_SUPER       // 超级管理员 → Argon2id
    }

    public static String encode(String rawPassword, UserType type) {
        return switch (type) {
            case FRONT_USER -> md5Encode(rawPassword);
            case ADMIN_NORMAL -> bcryptEncode(rawPassword);
            case ADMIN_SUPER -> argon2Encode(rawPassword);
        };
    }

    public static boolean matches(String rawPassword, String encoded, UserType type) {
        return switch (type) {
            case FRONT_USER -> md5Matches(rawPassword, encoded);
            case ADMIN_NORMAL -> bcryptMatches(rawPassword, encoded);
            case ADMIN_SUPER -> argon2Matches(rawPassword, encoded);
        };
    }

    /**
     * 自动探测加密类型并校验（用于数据库存储的密码回读）
     *  - $argon2id$ 开头 → Argon2id
     *  - $2a$ / $2b$ 开头 → BCrypt
     *  - 32 位 hex → MD5
     */
    public static boolean autoMatches(String rawPassword, String encoded) {
        if (encoded == null) return false;
        if (encoded.startsWith("$argon2id$")) return argon2Matches(rawPassword, encoded);
        if (encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$")) {
            return bcryptMatches(rawPassword, encoded);
        }
        if (encoded.length() == 32 && encoded.matches("[0-9a-fA-F]{32}")) {
            return md5Matches(rawPassword, encoded);
        }
        return false;
    }
}
