package com.algoviz.know.qrdant.controller;

import com.algoviz.know.qrdant.config.QdrantProperties;
import com.algoviz.know.qrdant.embedding.BgeEmbeddingService;
import com.algoviz.know.qrdant.qdrant.QdrantRestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** HTTP 探活（可选；主服务走 Dubbo ping，此接口便于运维/浏览器直查） */
@RestController
public class HealthController {

    private final QdrantProperties qdrantProps;
    private final BgeEmbeddingService embeddingService;
    private final QdrantRestClient qdrant;

    public HealthController(QdrantProperties qdrantProps,
                            BgeEmbeddingService embeddingService,
                            QdrantRestClient qdrant) {
        this.qdrantProps = qdrantProps;
        this.embeddingService = embeddingService;
        this.qdrant = qdrant;
    }

    @GetMapping({"/", "/health"})
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("service", "know-qrdant");
        m.put("modelReady", embeddingService.isReady());
        m.put("dim", qdrantProps.getDim());
        m.put("collection", qdrantProps.getCollection());
        try {
            qdrant.ensureCollection();
            m.put("qdrantConnected", true);
            m.put("total", qdrant.count());
        } catch (Exception e) {
            m.put("qdrantConnected", false);
            m.put("error", e.getMessage());
        }
        return m;
    }
}
