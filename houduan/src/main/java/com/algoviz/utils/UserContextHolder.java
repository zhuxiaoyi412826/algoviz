package com.algoviz.utils;

/**
 * 简易用户上下文占位（兼容：无登录态时返回 null）
 * 若后续接入鉴权，可在此处接入 ThreadLocal。
 */
public final class UserContextHolder {
    private static final ThreadLocal<Object> HOLDER = new ThreadLocal<>();

    public static void set(Object user) {
        if (user == null) HOLDER.remove();
        else HOLDER.set(user);
    }

    public static Object get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
