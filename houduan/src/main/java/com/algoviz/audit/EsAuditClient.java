package com.algoviz.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 直连客户端（JDK17 HttpClient，无第三方依赖）
 * 查询 Fluentd 写入的 algoviz-logs-* 索引中的审计日志
 */
@Slf4j
@Component
public class EsAuditClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${audit.es.url:http://localhost:9200}")
    private String esUrl;

    @Value("${audit.es.index-prefix:algoviz-logs}")
    private String indexPrefix;

    /**
     * 搜索审计日志（按时间倒序）
     */
    public List<AuditLogEntry> searchAuditLogs(int size) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        // logger 精确过滤（Fluentd 解析 logback %logger 输出 "AUDIT"），
        // 避免 match_phrase 分词后误命中含 "audit" 单词的普通日志
        com.fasterxml.jackson.databind.node.ArrayNode filter = body.putObject("query")
                .putObject("bool").putArray("filter");
        filter.addObject().putObject("term").put("logger.keyword", "AUDIT");
        filter.addObject().putObject("match_phrase").put("message", "AUDIT|");
        body.put("size", size);
        body.putArray("sort").addObject().putObject("@timestamp").put("order", "desc");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(esUrl + "/" + indexPrefix + "-*/_search"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("ES 查询失败 HTTP " + resp.statusCode() + ": " +
                    resp.body().substring(0, Math.min(200, resp.body().length())));
        }

        JsonNode hits = objectMapper.readTree(resp.body()).path("hits").path("hits");
        List<AuditLogEntry> result = new ArrayList<>();
        for (JsonNode h : hits) {
            String message = h.path("_source").path("message").asText("");
            int idx = message.indexOf("AUDIT|");
            if (idx < 0) continue;
            String json = message.substring(idx + 6);
            try {
                AuditLogEntry e = objectMapper.readValue(json, AuditLogEntry.class);
                e.setEsIndex(h.path("_index").asText(null));
                e.setEsDocId(h.path("_id").asText(null));
                e.setLogTime(h.path("_source").path("@timestamp").asText(null));
                result.add(e);
            } catch (Exception parseEx) {
                log.debug("[audit] 审计日志 JSON 解析跳过: {}", parseEx.getMessage());
            }
        }
        return result;
    }

    /**
     * 更新 ES 文档的 audit_status 字段（审核闭环回写，失败容忍）
     */
    public void updateAuditStatus(String index, String docId, String status) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode doc = body.putObject("doc");
            doc.put("audit_status", status);
            doc.put("audit_time", java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(esUrl + "/" + index + "/_update/" + docId))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("[audit] ES 状态回写失败(容忍): index={} doc={}: {}", index, docId, e.getMessage());
        }
    }

    /** ES 是否可用 */
    public boolean available() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(esUrl)).timeout(Duration.ofSeconds(3)).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
