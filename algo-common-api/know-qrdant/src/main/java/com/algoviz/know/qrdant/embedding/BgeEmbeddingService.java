package com.algoviz.know.qrdant.embedding;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.algoviz.know.qrdant.config.EmbeddingProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * bge-large-zh-v1.5 嵌入服务（ONNX Runtime 推理，1024 维）。
 * 管线：WordPiece tokenize → 模型 forward → last_hidden_state 均值池化 → L2 归一化。
 * 模型文件（model.onnx + vocab.txt）由 tools/export_onnx.py 一次性生成，缺模型时服务降级（modelReady=false）。
 */
@Component
public class BgeEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(BgeEmbeddingService.class);

    private final EmbeddingProperties props;

    private OrtEnvironment env;
    private OrtSession session;
    private WordPieceTokenizer tokenizer;
    private String[] inputNames = {"input_ids", "attention_mask", "token_type_ids"};
    private boolean ready = false;

    public BgeEmbeddingService(EmbeddingProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        try {
            Path dir = Path.of(props.getModelDir()).toAbsolutePath();
            Path onnx = dir.resolve("model.onnx");
            Path vocab = dir.resolve("vocab.txt");
            if (!Files.exists(onnx) || !Files.exists(vocab)) {
                log.warn("嵌入模型未就绪：{} 缺少 model.onnx/vocab.txt。请先运行 tools/export_onnx.py。当前目录={}",
                        props.getModelDir(), dir);
                return;
            }
            this.tokenizer = WordPieceTokenizer.fromFile(vocab);
            this.env = OrtEnvironment.getEnvironment();
            this.session = env.createSession(onnx.toString(), new OrtSession.SessionOptions());
            // 动态探测输入名（不同导出工具的命名可能不同）
            var meta = session.getInputInfo();
            String[] detected = meta.keySet().toArray(new String[0]);
            if (detected.length >= 2) {
                inputNames = detected;
            }
            this.ready = true;
            log.info("bge-large-zh-v1.5 模型已加载: inputs={}, 维度={}", String.join(",", inputNames), dim());
        } catch (Exception e) {
            log.error("嵌入模型加载失败", e);
            this.ready = false;
        }
    }

    @PreDestroy
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("关闭 ONNX session 失败: {}", e.getMessage());
            }
        }
    }

    public boolean isReady() {
        return ready;
    }

    /** 向量维度（bge-large-zh-v1.5 = 1024） */
    public int dim() {
        return 1024;
    }

    /**
     * 文本 → 1024 维向量（均值池化 + L2 归一化）。
     *
     * @param text        文本
     * @param isQuery     是否检索 query（true 时附加 BGE 指令前缀）
     */
    public float[] embed(String text, boolean isQuery) {
        if (!ready) {
            throw new IllegalStateException("嵌入模型未就绪（缺少 model.onnx / vocab.txt，请先运行 tools/export_onnx.py）");
        }
        String content = isQuery && props.getQueryPrefix() != null && !props.getQueryPrefix().isEmpty()
                ? props.getQueryPrefix() + text : text;
        int[][] encoded = tokenizer.encode(content, props.getMaxSeqLen());
        try (OnnxTensor ids = OnnxTensor.createTensor(env, new long[][]{toLongs(encoded[0])});
             OnnxTensor mask = OnnxTensor.createTensor(env, new long[][]{toLongs(encoded[1])});
             OnnxTensor seg = OnnxTensor.createTensor(env, new long[][]{toLongs(encoded[2])})) {

            Map<String, OnnxTensor> feeds = new HashMap<>();
            feeds.put(inputNames[0], ids);
            feeds.put(inputNames[1], mask);
            if (inputNames.length > 2) {
                feeds.put(inputNames[2], seg);
            }

            try (OrtSession.Result result = session.run(feeds)) {
                // 取 last_hidden_state [1, seq, hidden]
                float[][][] lastHidden = (float[][][]) result.get(0).getValue();
                int hidden = lastHidden[0][0].length;
                float[] pooled = new float[hidden];
                int valid = 0;
                for (int t = 0; t < lastHidden[0].length; t++) {
                    if (encoded[1][t] == 0) {
                        continue;
                    }
                    for (int h = 0; h < hidden; h++) {
                        pooled[h] += lastHidden[0][t][h];
                    }
                    valid++;
                }
                if (valid > 0) {
                    for (int h = 0; h < hidden; h++) {
                        pooled[h] /= valid;
                    }
                }
                return l2Normalize(pooled);
            }
        } catch (Exception e) {
            throw new IllegalStateException("嵌入计算失败: " + e.getMessage(), e);
        }
    }

    private static float[] l2Normalize(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm < 1e-9) {
            return v;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
        return v;
    }

    private static long[] toLongs(int[] arr) {
        long[] out = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            out[i] = arr[i];
        }
        return out;
    }
}
