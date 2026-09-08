package com.algoviz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 本地自托管 GraphiQL 调试面板。
 * <p>
 * Spring Boot 自带的 GraphiQL 页面会从 unpkg.com 外链加载
 * react / react-dom / graphiql / @graphiql/plugin-explorer 等 UMD，
 * 国内网络环境下要么被 CORS 拦截、要么 CDN 版本路径 404，
 * 最终页面停在 "Loading..." 白屏。
 * <p>
 * 因此改为完全本地化：资源放在 static/graphiql/（index.html + vendor/*），
 * 不依赖任何外部 CDN；页面内置带 satoken 请求头的 fetcher，直连 /graphql。
 * 鉴权：页面本身不拦截，真正发往 /graphql 的查询由 GraphqlSaAuthInterceptor 校验。
 */
@Controller
public class GraphiqlUiController {

    @GetMapping("/graphiql")
    public String index() {
        return "forward:/graphiql/index.html";
    }
}
