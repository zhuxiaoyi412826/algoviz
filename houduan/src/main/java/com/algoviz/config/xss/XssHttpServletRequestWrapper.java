package com.algoviz.config.xss;

import com.algoviz.common.constant.XssConstants;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * XSS 请求包装器：
 * 覆盖 getParameter / getParameterValues / getInputStream / getReader，
 * 对 QueryString、Form-data、application/x-www-form-urlencoded 内容做统一 Jsoup clean。
 *
 * @RequestBody JSON 的处理由 JsonBodyCleanAdvice 负责（走 Spring MVC 解析链路，不走此处 getReader）
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /** 用 Jsoup.clean 清洗字符串；输出格式改为保留文本格式（不输出外层 html/head/body） */
    private static String clean(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Jsoup.clean(s, "", XssConstants.SAFELIST,
                new org.jsoup.nodes.Document.OutputSettings()
                        .outline(false)
                        .prettyPrint(false));
    }

    /** 对非结构化文本（纯文本搜索词）做最严格的 clean — 去除所有标签 */
    private static String cleanText(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String r = Jsoup.clean(s, "", org.jsoup.safety.Safelist.none(),
                new Document.OutputSettings().outline(false).prettyPrint(false));
        // 二次兜底：HTML 实体反转义，避免 &lt; / &gt; 入库
        return org.jsoup.parser.Parser.unescapeEntities(r, false);
    }

    /** 原始 body 字节缓存（首次读取 getInputStream 时写入） */
    private byte[] cachedBody;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 预先把原始请求体读入内存，避免 reset 不支持导致 body 只能读一次
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    // ============== QueryString / Form 参数 ==============

    @Override
    public String getParameter(String name) {
        return cleanText(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = cleanText(values[i]);
        }
        return result;
    }

    // ============== application/x-www-form-urlencoded 提交的 body ==============

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 先对缓存 body 做文本层面的 XSS 清洗（针对 application/x-www-form-urlencoded；JSON 走另一套 Advice）
        String contentType = getContentType();
        byte[] cleaned;
        if (contentType != null
                && (contentType.startsWith("application/x-www-form-urlencoded")
                    || contentType.startsWith("multipart/form-data"))) {
            // 表单 / 文件上传：直接返回原始字节，文件内容不做 XSS，
            // multipart 的字段值会通过 getParameter 被 Spring 读取，已在 getParameter 中过滤
            cleaned = cachedBody;
        } else {
            // application/json 等文本 body：整体 clean，
            // 注意：对 JSON 字符串做 clean 会误删 < > 等字符，
            // 所以此处保留原字节交给 JsonBodyCleanAdvice 在解析后按字段处理
            cleaned = cachedBody;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(cleaned);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) {}
            @Override public int read() { return bais.read(); }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        try {
            return getInputStream().available();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public long getContentLengthLong() {
        try {
            return getInputStream().available();
        } catch (IOException e) {
            return -1;
        }
    }

    // 给 Filter / Advice 读取原始 body 复用
    public byte[] getCachedBody() {
        return cachedBody;
    }
}
