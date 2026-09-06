package com.algoviz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口响应水印注解
 *
 * <p><b>用途：</b>标记在敏感接口的 Controller 方法上。被标记的接口返回 JSON 时，
 * 由 {@code ResponseWatermarkAdvice}（ResponseBodyAdvice）在根节点追加
 * {@code _watermark} 水印对象，用于数据泄露后的事后溯源取证，
 * 同时可作为前端页面水印的数据源。</p>
 *
 * <p><b>水印内容：</b>userId、username、tenantId、clientIp、traceId、accessTime
 * （登录用户信息从 Sa-Token 登录上下文获取）。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @GetMapping("/system/admin")
 * @ResponseWatermark
 * public ApiResponse<Map<String, Object>> getAdminList(...) { ... }
 * }</pre>
 *
 * <p><b>边界：</b>
 * 1. 只有标注本注解的接口才输出水印，普通接口不输出；
 * 2. {@link #enable()} 可临时关闭单个接口的水印（如高频低敏接口）；
 * 3. 注解只放在方法上，如需整个 Controller 生效请逐个方法标注。</p>
 *
 * <p><b>安全备注：</b>接口水印属于事后溯源取证，客户端可自行删除/篡改
 * {@code _watermark} 字段，无法作为权限校验或防篡改手段；
 * 需搭配安全审计日志（本系统已有 OperationLogAspect 记录操作留痕）使用。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseWatermark {

    /**
     * 是否启用该接口的水印输出，默认启用。
     * 置 false 时切面对本接口直接放行（等效于未标注）。
     */
    boolean enable() default true;
}
