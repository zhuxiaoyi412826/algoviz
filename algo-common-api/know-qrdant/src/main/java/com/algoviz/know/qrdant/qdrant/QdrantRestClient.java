package com.algoviz.know.qrdant.qdrant;

import com.algoviz.know.qrdant.config.QdrantProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Qdrant REST 客户端（直连 6333，轻量、零第三方向量依赖）。
 * 文档：https://qdrant.tech/documentation/concepts/points/
 */
@Component
public class QdrantRestClient {

    private static final Logger log = LoggerFactory.getLogger(QdrantRestClient.class);

    private final QdrantProperties props;
    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();

    /** 待写入点：稳定 UUID 点 id（由业务 id 派生）+ 向量 + payload */
    public record PointInput(String pointId, float[] vector, Map<String, String> payload) {
    }

    /** 命中结果 */
    public record Hit(String id, double score, Map<String, Object> payload) {
    }

    public QdrantRestClient(QdrantProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    // ==================== 底层 HTTP ====================

    private JsonNode request(String method, String path, Object body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(props.baseUrl() + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json");
            if (body != null) {
                b.method(method, HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), java.nio.charset.StandardCharsets.UTF_8));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("Qdrant " + method + " " + path + " -> " + resp.statusCode()
                        + ": " + truncate(resp.body(), 300));
            }
            if (resp.body() == null || resp.body().isBlank()) {
                return om.createObjectNode();
            }
            return om.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Qdrant 请求失败 " + method + " " + path + ": " + e.getMessage(), e);
        }
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    // ==================== 集合管理 ====================

    /** 集合是否存在 */
    public boolean collectionExists() {
        try {
            return request("GET", "/collections/" + props.getCollection(), null).path("result").isMissingNode() == false;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** 幂等创建集合 */
    public void ensureCollection() {
        if (collectionExists()) {
            return;
        }
        ObjectNode cfg = om.createObjectNode();
        cfg.put("size", props.getDim());
        cfg.put("distance", props.getDistance());
        ObjectNode hnsw = cfg.putObject("hnsw_config");
        hnsw.put("m", props.getHnswM());
        hnsw.put("ef_construct", props.getHnswEfConstruct());
        ObjectNode body = om.createObjectNode();
        body.set("vectors", cfg);
        request("PUT", "/collections/" + props.getCollection(), body);
        log.info("Qdrant 集合已创建: {} (dim={}, distance={})", props.getCollection(), props.getDim(), props.getDistance());
    }

    /** 删除并重建集合（清空） */
    public long recreateCollection() {
        long before = count();
        request("DELETE", "/collections/" + props.getCollection(), null);
        ensureCollection();
        return before;
    }

    // ==================== 点操作 ====================

    /** 批量 upsert（id 为稳定 UUID 字符串） */
    public void upsert(List<PointInput> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ArrayNode arr = om.createArrayNode();
        for (PointInput p : points) {
            ObjectNode node = om.createObjectNode();
            node.put("id", p.pointId());
            ArrayNode vec = node.putArray("vector");
            for (float v : p.vector()) {
                vec.add((double) v);
            }
            ObjectNode pl = node.putObject("payload");
            if (p.payload() != null) {
                p.payload().forEach((k, v) -> pl.put(k, v == null ? "" : v));
            }
            arr.add(node);
        }
        ObjectNode body = om.createObjectNode();
        body.set("points", arr);
        request("PUT", "/collections/" + props.getCollection() + "/points?wait=true", body);
    }

    /** 语义检索；category 非空时按 payload.category 精确过滤（必须 with_payload=true 才能拿到 algorithmId） */
    public List<Hit> search(float[] vector, int limit, String category) {
        ObjectNode body = om.createObjectNode();
        ArrayNode vec = body.putArray("vector");
        for (float v : vector) {
            vec.add((double) v);
        }
        body.put("limit", limit);
        body.put("with_payload", true);
        if (category != null && !category.isBlank()) {
            body.set("filter", matchFilter("category", category));
        }
        JsonNode resp = request("POST", "/collections/" + props.getCollection() + "/points/search", body);
        List<Hit> hits = new ArrayList<>();
        for (JsonNode h : resp.path("result")) {
            hits.add(new Hit(h.path("id").asText(),
                    h.path("score").asDouble(),
                    toMap(h.path("payload"))));
        }
        return hits;
    }

    /** 向量总数 */
    public long count() {
        ObjectNode body = om.createObjectNode();
        body.put("exact", true);
        JsonNode resp = request("POST", "/collections/" + props.getCollection() + "/points/count", body);
        return resp.path("result").path("count").asLong(0);
    }

    /** 按 algorithmId payload 删除 */
    public boolean deleteByAlgorithmId(String algorithmId) {
        ObjectNode body = om.createObjectNode();
        body.set("filter", matchFilter("algorithmId", algorithmId));
        JsonNode resp = request("POST", "/collections/" + props.getCollection() + "/points/delete", body);
        return resp.path("result").path("status").asText().equals("ok");
    }

    /** 分页滚动（keyword 在 Java 侧按 name/title 子串过滤）；带完整向量 */
    public List<Map<String, Object>> scroll(int limit, Object offset) {
        ObjectNode body = om.createObjectNode();
        body.put("limit", limit);
        body.put("with_payload", true);
        body.put("with_vector", true);
        if (offset != null) {
            body.set("offset", om.valueToTree(offset));
        }
        JsonNode resp = request("POST", "/collections/" + props.getCollection() + "/points/scroll", body);
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode p : resp.path("result").path("points")) {
            Map<String, Object> item = toMap(p.path("payload"));
            item.put("_id", p.path("id").asText());
            JsonNode vecNode = p.path("vector");
            if (vecNode.isArray()) {
                List<Double> vec = new ArrayList<>();
                for (JsonNode v : vecNode) {
                    vec.add(v.asDouble());
                }
                item.put("vector", vec);
            }
            out.add(item);
        }
        return out;
    }

    /** 集合信息（配置 + 状态） */
    public JsonNode collectionInfo() {
        return request("GET", "/collections/" + props.getCollection(), null).path("result");
    }

    /** 生成稳定 UUID 点 id */
    public static String stableUuid(String key) {
        return UUID.nameUUIDFromBytes((key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    // ==================== 工具 ====================

    private ObjectNode matchFilter(String key, String value) {
        ObjectNode match = om.createObjectNode();
        match.put("value", value);
        ObjectNode cond = om.createObjectNode();
        cond.put("key", key);
        cond.set("match", match);
        ObjectNode filter = om.createObjectNode();
        ArrayNode must = filter.putArray("must");
        must.add(cond);
        return filter;
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                map.put(e.getKey(), v.isValueNode() ? v.asText() : v.toString());
            });
        }
        return map;
    }
}
