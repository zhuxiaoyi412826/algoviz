package com.algoviz.controller;

import com.algoviz.know.api.dto.*;
import com.algoviz.service.AlgorithmVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 算法题目向量管理（后台）：
 * 调独立服务 know-qrdant（Dubbo 直连）。服务未启动时：
 *  - health / stats 返回 offline 状态
 *  - sync / clear / search / vectors 等操作类接口返回 HTTP 404「向量检索服务未启动」
 * 数据由页面"手动全量同步"触发入库（本服务不自动拉取）。
 */
@RestController
@RequestMapping("/api/algorithm-vector/admin")
@Tag(name = "算法题目向量管理", description = "算法题知识库向量管理（Qdrant + bge-large-zh-v1.5）")
public class AlgorithmVectorController {

    private final AlgorithmVectorService service;

    public AlgorithmVectorController(AlgorithmVectorService service) {
        this.service = service;
    }

    private ResponseEntity<Map<String, Object>> unavailable() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "向量检索服务未启动");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Map<String, Object> health() {
        KnowServiceStatus s = service.ping();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", s.isModelReady() && s.isQdrantConnected() ? "ok" : "offline");
        m.put("modelReady", s.isModelReady());
        m.put("qdrantConnected", s.isQdrantConnected());
        m.put("dim", s.getDim());
        m.put("collection", s.getCollectionName());
        m.put("total", s.getTotal());
        m.put("message", s.getMessage());
        return m;
    }

    @GetMapping("/stats")
    @Operation(summary = "向量库统计")
    public Map<String, Object> stats() {
        try {
            KnowStats s = service.stats();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("collectionName", s.getCollectionName());
            m.put("vectorCount", s.getTotal());
            m.put("modelName", "bge-large-zh-v1.5");
            m.put("dim", s.getDim());
            m.put("distance", s.getDistance());
            m.put("status", "offline".equals(s.getStatus()) ? "offline" : "running");
            return m;
        } catch (Exception e) {
            // 探活类：服务不可用时返回离线状态而非报错
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("collectionName", "algorithm_knowledge");
            m.put("vectorCount", 0);
            m.put("modelName", "bge-large-zh-v1.5");
            m.put("dim", 1024);
            m.put("distance", "Cosine");
            m.put("status", "offline");
            return m;
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "全量同步算法题到向量库（手动触发）")
    public ResponseEntity<Map<String, Object>> syncAll() {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            boolean submitted = service.startSync();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", true);
            m.put("submitted", submitted);
            m.put("message", submitted ? "已提交全量同步，请查看进度" : "已有同步任务进行中");
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }

    @GetMapping("/sync/progress")
    @Operation(summary = "同步进度")
    public Map<String, Object> syncProgress() {
        return service.progressSnapshot();
    }

    @PostMapping("/sync/cancel")
    @Operation(summary = "取消同步")
    public Map<String, Object> cancelSync() {
        service.cancelSync();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", "已请求取消同步");
        return m;
    }

    @PostMapping("/clear")
    @Operation(summary = "清空向量库")
    public ResponseEntity<Map<String, Object>> clear() {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            long deleted = service.clear();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", deleted >= 0);
            m.put("deleted", deleted);
            m.put("message", deleted >= 0 ? "向量库已清空" : "清空失败");
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }

    @GetMapping("/collection-info")
    @Operation(summary = "集合信息")
    public ResponseEntity<Map<String, Object>> collectionInfo() {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            KnowCollectionInfo info = service.collectionInfo();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("collectionName", info.getCollectionName());
            m.put("dim", info.getDim());
            m.put("distance", info.getDistance());
            m.put("config", info.getConfig());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }

    @GetMapping("/vectors")
    @Operation(summary = "向量分页列表")
    public ResponseEntity<Map<String, Object>> vectors(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(defaultValue = "") String keyword,
                                                       @RequestParam(defaultValue = "") String algorithmId,
                                                       @RequestParam(defaultValue = "") String category,
                                                       @RequestParam(defaultValue = "") String tags) {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            KnowPageResult r = service.list(page, pageSize, keyword, algorithmId, category, tags);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("list", r.getList());
            m.put("total", r.getTotal());
            m.put("page", r.getPage());
            m.put("pageSize", r.getPageSize());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }

    @PostMapping("/search")
    @Operation(summary = "语义检索测试")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, Object> body) {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            String query = body.get("query") == null ? "" : String.valueOf(body.get("query"));
            int topK = body.get("topK") == null ? 10 : Integer.parseInt(String.valueOf(body.get("topK")));
            String category = body.get("category") == null ? null : String.valueOf(body.get("category"));
            KnowSearchResult r = service.search(query, topK, category);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("items", r.getItems());
            m.put("total", r.getTotal());
            m.put("tookMs", r.getTookMs());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }

    @DeleteMapping("/vectors/{algorithmId}")
    @Operation(summary = "删除单条算法题向量")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String algorithmId) {
        try {
            if (!service.isAvailable()) {
                return unavailable();
            }
            boolean ok = service.delete(algorithmId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", ok);
            m.put("message", ok ? "删除成功" : "删除失败");
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return unavailable();
        }
    }
}
