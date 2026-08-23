package com.algoviz.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，表示该操作需要记录到 operation_log 表
 * 如果不标注，AOP 会根据 URL 自动推断模块和操作类型
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 模块名，如 "用户管理"、"内容审核"
     * 留空时自动根据 URL 路径推断
     */
    String module() default "";

    /**
     * 操作类型，如 "新增"、"删除"、"编辑"
     * 留空时自动根据 HTTP 方法推断
     */
    String action() default "";

    /**
     * 操作详情模板
     * 留空时自动根据 URL 路径和方法名生成
     */
    String detail() default "";
}
