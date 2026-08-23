package com.algoviz.common.constant;

import org.jsoup.safety.Safelist;

/**
 * XSS 过滤常量 & Jsoup Safelist 配置
 */
public final class XssConstants {

    private XssConstants() {}

    /**
     * 全局统一的 XSS 白名单策略：
     * - 基于 relaxed（常见富文本安全标签）
     * - 额外允许 Markdown 渲染常用标签（h1~h6 / pre / code / blockquote / table 系列）
     * - <a> 允许 target="_blank"
     * - <code> 允许 class 属性（Prism / highlight.js 语法高亮类名）
     * - <img> 仅允许 http/https/data 三种协议（防止 javascript: / file: 注入）
     */
    public static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "pre", "code", "blockquote",
                    "table", "thead", "tbody", "tr", "th", "td", "caption", "hr", "details", "summary")
            .addAttributes("code", "class")
            .addAttributes("a", "target", "rel")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("a", "href", "http", "https", "mailto");
}
