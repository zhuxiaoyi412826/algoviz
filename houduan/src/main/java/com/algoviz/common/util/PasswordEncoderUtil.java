package com.algoviz.common.util;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

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

    // Argon2id 参数：内存 64MB、并行度 1、迭代 3、输出 32 字节 + salt 16 字节
    private static final int ARGON2_TYPE = Argon2Parameters.ARGON2_id;
    private static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13;
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY = 65536;     // 64 MB
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_SALT_LEN = 16;
    private static final int ARGON2_HASH_LEN = 32;

    private static final SecureRandom SR = new SecureRandom();

    /**
     * Argon2id 加密，返回 Base64 字符串（约 96~110 字符，VARCHAR(128) 足够）
     * 格式：$argon2id$v=19$m=65536,t=3,p=1${saltBase64}${hashBase64}
     */
    public static String argon2Encode(String rawPassword) {
        byte[] salt = new byte[ARGON2_SALT_LEN];
        SR.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(ARGON2_TYPE)
                .withVersion(ARGON2_VERSION)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);
        byte[] hash = new byte[ARGON2_HASH_LEN];
        gen.generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), hash);

        String saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt);
        String hashB64 = Base64.getEncoder().withoutPadding().encodeToString(hash);

        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
                ARGON2_MEMORY, ARGON2_ITERATIONS, ARGON2_PARALLELISM, saltB64, hashB64);
    }

    public static boolean argon2Matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null || !encoded.startsWith("$argon2id$")) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            // $argon2id$v=19$m=X,t=Y,p=Z$salt$hash  →  split 后长度 6
            if (parts.length < 6) return false;
            String[] params = parts[3].split(",");
            int m = 0, t = 0, p = 0;
            for (String kv : params) {
                if (kv.startsWith("m=")) m = Integer.parseInt(kv.substring(2));
                else if (kv.startsWith("t=")) t = Integer.parseInt(kv.substring(2));
                else if (kv.startsWith("p=")) p = Integer.parseInt(kv.substring(2));
            }
            byte[] salt = Base64.getDecoder().decode(parts[4]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[5]);

            Argon2Parameters argonParams = new Argon2Parameters.Builder(ARGON2_TYPE)
                    .withVersion(ARGON2_VERSION)
                    .withIterations(t)
                    .withMemoryAsKB(m)
                    .withParallelism(p)
                    .withSalt(salt)
                    .build();

            Argon2BytesGenerator gen = new Argon2BytesGenerator();
            gen.init(argonParams);
            byte[] actualHash = new byte[expectedHash.length];
            gen.generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), actualHash);

            return MessageDigest.isEqual(expectedHash, actualHash);
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
