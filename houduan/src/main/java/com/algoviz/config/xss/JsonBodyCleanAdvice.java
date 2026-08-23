package com.algoviz.config.xss;

import com.algoviz.common.constant.XssConstants;
import com.algoviz.config.XssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @RequestBody JSON / Map 体 XSS 清洗 AOP：
 *  在 MappingJackson2HttpMessageConverter 把 JSON 反序列化为对象后、进入 Controller 方法之前，
 *  递归遍历对象图：
 *    - String 类型字段 → Jsoup clean（标签内容用 relaxed+ 白名单，纯文本内容 none + 实体反转义）
 *    - Collection / Map / 数组 → 递归进入
 *    - 自定义实体 DTO → 反射遍历字段
 *  对 excludeUrls 的请求路径 + GET 方法跳过。
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "xss", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JsonBodyCleanAdvice extends RequestBodyAdviceAdapter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final XssProperties properties;

    /** 只对 application/json 类请求生效（所有 Object.class 请求体，supports 直接返回 true） */
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
                                MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body == null) return null;
        try {
            // 按请求 URI 判断是否排除
            String uri = currentRequestUri();
            if (uri != null && isExcluded(uri)) return body;
            cleanObject(body, new IdentityHashMap<>(128));
        } catch (Exception e) {
            log.warn("[XSS] JSON 请求体清洗异常，跳过：{}", e.getMessage());
        }
        return body;
    }

    // ==================== 核心递归清洗 ====================

    /**
     * 递归清洗对象；visited 用于处理循环引用
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void cleanObject(Object obj, Map<Object, Object> visited) {
        if (obj == null) return;
        Class<?> clazz = obj.getClass();

        // 1) 已访问过 → 直接返回，防循环
        if (visited.containsKey(obj)) return;

        // 2) 终端类型
        if (obj instanceof String s) {
            // String 不可变，此处无法原地修改，返回值由调用方写入容器 / 字段
            return;
        }
        visited.put(obj, obj);

        // 3) Collection / Iterable（List / Set / Vector ...）
        if (obj instanceof Iterable<?> iterable) {
            List<Object> copy = new ArrayList<>();
            for (Object e : iterable) copy.add(e);
            if (obj instanceof List list) {
                for (int i = 0; i < copy.size(); i++) {
                    Object e = copy.get(i);
                    if (e instanceof String s) list.set(i, cleanString(s));
                    else cleanObject(e, visited);
                }
            } else if (obj instanceof Set) {
                // Set 无法按索引修改，先清空再加回（过滤后仍可能重复，但概率低）
                Set set = (Set) obj;
                List<Object> replaced = new ArrayList<>();
                for (Object e : copy) {
                    if (e instanceof String s) replaced.add(cleanString(s));
                    else { cleanObject(e, visited); replaced.add(e); }
                }
                set.clear();
                set.addAll(replaced);
            } else {
                // 其它 Iterable：普通递归进入（不尝试修改容器元素）
                for (Object e : copy) cleanObject(e, visited);
            }
            return;
        }

        // 4) 数组
        if (clazz.isArray()) {
            if (obj instanceof String[] arr) {
                for (int i = 0; i < arr.length; i++) arr[i] = cleanString(arr[i]);
                return;
            }
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) cleanObject(Array.get(obj, i), visited);
            return;
        }

        // 5) Map
        if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> m = (Map<Object, Object>) map;
            List<Map.Entry<Object, Object>> entries = new ArrayList<>(m.entrySet());
            for (Map.Entry<Object, Object> e : entries) {
                Object v = e.getValue();
                if (v instanceof String s) {
                    m.put(e.getKey(), cleanString(s));
                } else {
                    cleanObject(v, visited);
                }
            }
            return;
        }

        // 6) 基础类型 / 枚举 / 包装类型 → 不处理
        if (isSimpleType(clazz)) return;

        // 7) 自定义 DTO / VO / Entity → 反射遍历所有字段（含父类）
        Field[] fields = getAllFields(clazz);
        for (Field f : fields) {
            if (f.getType().isPrimitive() || f.getType().isEnum()) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val == null) continue;
                if (val instanceof String s) {
                    // 区分字段类型：description/solution/content 等富文本字段用 relaxed 白名单
                    // 其余字符串（name/title/tag/username 等）用最严格 Safelist.none 清洗
                    String cleaned = isRichTextField(f.getName()) ? cleanRich(s) : cleanText(s);
                    if (!Objects.equals(s, cleaned)) f.set(obj, cleaned);
                } else {
                    cleanObject(val, visited);
                }
            } catch (IllegalAccessException ignored) {
                // 不可访问字段跳过
            }
        }
    }

    /** 自定义 DTO 字段名命中以下关键字视为富文本 → 用宽松白名单（保留 h1/code/table 等标签） */
    private static final Set<String> RICH_TEXT_HINTS = Set.of(
            "description", "solution", "content", "body", "detail", "details",
            "markdown", "comment", "reply", "article", "review", "feedback",
            "note", "answer", "explanation", "analyze", "analysis"
    );

    private static boolean isRichTextField(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String key : RICH_TEXT_HINTS) if (lower.contains(key)) return true;
        return false;
    }

    /** 富文本内容：使用统一 relaxed+ 白名单 */
    public static String cleanRich(String s) {
        if (s == null || s.isEmpty()) return s;
        Document.OutputSettings os = new Document.OutputSettings()
                .outline(false).prettyPrint(false);
        return Jsoup.clean(s, "", XssConstants.SAFELIST, os);
    }

    /** 纯文本内容：严格去所有标签 + 反转义 HTML 实体 */
    public static String cleanText(String s) {
        if (s == null || s.isEmpty()) return s;
        Document.OutputSettings os = new Document.OutputSettings()
                .outline(false).prettyPrint(false);
        String r = Jsoup.clean(s, "", org.jsoup.safety.Safelist.none(), os);
        return Parser.unescapeEntities(r, false);
    }

    /** 兜底对外入口（Filter 对表单/Query 调用），此处按纯文本处理 */
    public static String cleanString(String s) {
        return cleanText(s);
    }

    // ==================== 工具方法 ====================

    private static boolean isSimpleType(Class<?> c) {
        return c.isPrimitive()
                || c == String.class
                || c == Integer.class || c == Long.class || c == Short.class || c == Byte.class
                || c == Double.class || c == Float.class
                || c == Boolean.class || c == Character.class
                || c == java.math.BigDecimal.class || c == java.math.BigInteger.class
                || c == java.time.LocalDateTime.class || c == java.time.LocalDate.class
                || c == java.time.LocalTime.class || c == java.util.Date.class
                || c == java.util.UUID.class
                || c.isEnum();
    }

    private static Field[] getAllFields(Class<?> c) {
        List<Field> list = new ArrayList<>();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                // 跳过静态字段 / 序列化字段 / 特殊 Spring/Lombok 合成字段
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if ("serialVersionUID".equals(f.getName())) continue;
                list.add(f);
            }
            c = c.getSuperclass();
        }
        return list.toArray(new Field[0]);
    }

    // ==================== Path 排除 & 当前请求获取 ====================

    private boolean isExcluded(String uri) {
        for (String pattern : properties.getExcludeUrls()) {
            if (PATH_MATCHER.match(pattern, uri)) return true;
        }
        return false;
    }

    private static String currentRequestUri() {
        try {
            jakarta.servlet.http.HttpServletRequest req =
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                            .getRequest();
            String ctx = req.getContextPath();
            String uri = req.getRequestURI();
            if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
                return uri.substring(ctx.length());
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }
}
