package com.algoviz.audit;

import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.SensitiveWord;
import com.algoviz.entity.SensitiveWordVersion;
import com.algoviz.mapper.SensitiveWordMapper;
import com.algoviz.mapper.SensitiveWordVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 敏感词库服务：
 * - CRUD 直接操作 MySQL 工作区，写后失效 Redis 缓存
 * - 检测用词表优先读 Redis（audit:words:cache，TTL 可配），miss 回源 MySQL 并回填
 * - 版本管理：发布 = 冻结快照到 sensitive_word_version；回滚 = 快照写回工作区
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    public static final String CACHE_KEY = "audit:words:cache";
    public static final String VERSION_KEY = "audit:words:current";
    /** 词表指纹（快速判定是否变化，避免每次 getTrie 时反序列化 4 万条 JSON） */
    public static final String FINGERPRINT_KEY = "audit:words:fp";

    private final SensitiveWordMapper wordMapper;
    private final SensitiveWordVersionMapper versionMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${audit.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    // 内存 DFA 缓存（Redis 词表 JSON 变化时重建）
    private volatile DfaTrie trieCache;
    private volatile String trieFingerprint = "";
    private volatile java.util.Map<String, SensitiveWord> wordMetaCache;
    private volatile String metaFingerprint = "";
    // 用于快速跳过 fp 查询：Redis 里的 fp 没变时，连 Redis fp 都不用再读（同一毫秒内高并发场景）
    private volatile String lastSeenRedisFp = "";

    // ==================== 检测侧：获取词库（Redis 优先） ====================

    /** 启用词列表：Redis -> MySQL（Redis 故障自动降级直查） */
    public List<SensitiveWord> getEnabledWords() {
        try {
            String json = redis.opsForValue().get(CACHE_KEY);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SensitiveWord.class));
            }
        } catch (Exception e) {
            log.warn("[audit] Redis 读取词库失败，降级 MySQL: {}", e.getMessage());
        }
        List<SensitiveWord> words = wordMapper.selectAllEnabled();
        refreshCacheWithWords(words);
        return words;
    }

    /** 计算词表指纹（只用来判断"是否变化"，不参与业务） */
    private static String fingerprintOf(List<SensitiveWord> words) {
        if (words == null || words.isEmpty()) return "0@-@0";
        return words.size() + "@" + words.get(0).getId() + "@" + words.get(words.size() - 1).getId();
    }

    /** 从 Redis 取 fp（如果没有就从 MySQL 回填），失败返回 null */
    private String loadRedisFingerprint() {
        try {
            String fp = redis.opsForValue().get(FINGERPRINT_KEY);
            if (fp != null && !fp.isEmpty()) return fp;
        } catch (Exception ignored) {}
        // fp 缺失：触发一次词表回填
        try {
            List<SensitiveWord> words = wordMapper.selectAllEnabled();
            refreshCacheWithWords(words);
            return fingerprintOf(words);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 DFA（高并发版本）。
     * 性能关键点：不再每次 getEnabledWords 做"Redis 读几 MB JSON + 4 万对象反序列化"，
     * 而是先对比 20 字节以内的指纹字符串：
     *   1) 内存指纹 == Redis fp → 直接返回 trieCache（0 IO）
     *   2) 不一致 → 再拉整表构建
     * 这将评论列表从 8~9 秒 / 页降到毫秒级（敏感词 4 万条场景）。
     */
    public DfaTrie getTrie() {
        DfaTrie cached = trieCache;
        String cachedFp = trieFingerprint;
        String redisFp = loadRedisFingerprint();
        // 0) Redis 挂了 / 读失败：降级返回内存缓存
        if (redisFp == null) {
            if (cached != null) return cached;
        } else if (cached != null && redisFp.equals(cachedFp)) {
            // 1) 快速路径：词表未变 → 无额外开销
            lastSeenRedisFp = redisFp;
            return cached;
        }
        // 2) 慢路径：词表变化或首次初始化，重建 Trie（多线程竞争时双重检查）
        synchronized (this) {
            String newRedisFp = (redisFp == null) ? loadRedisFingerprint() : redisFp;
            DfaTrie cur = trieCache;
            String curFp = trieFingerprint;
            if (cur != null && newRedisFp != null && newRedisFp.equals(curFp)) {
                return cur;
            }
            List<SensitiveWord> words = getEnabledWords();
            String newFp = fingerprintOf(words);
            // 更新 fp（保证下次快速路径）
            try { redis.opsForValue().set(FINGERPRINT_KEY, newFp, Duration.ofSeconds(cacheTtlSeconds)); } catch (Exception ignored) {}
            lastSeenRedisFp = newFp;
            if (cur != null && newFp.equals(curFp)) return cur;  // fp 没变，仍沿用旧 Trie
            DfaTrie nt = new DfaTrie();
            for (SensitiveWord w : words) {
                if (w.getWord() != null) nt.insert(w.getWord());
            }
            trieCache = nt;
            trieFingerprint = newFp;
            return nt;
        }
    }

    /**
     * word -> 元信息（等级/分类）：与 getTrie 相同的 fp 对比快速路径，
     * 避免每次调用都反序列化整份词表。
     */
    public java.util.Map<String, SensitiveWord> getWordMeta() {
        java.util.Map<String, SensitiveWord> cached = wordMetaCache;
        String cachedFp = metaFingerprint;
        String redisFp = loadRedisFingerprint();
        if (redisFp != null && cached != null && redisFp.equals(cachedFp)) {
            return cached;
        }
        synchronized (this) {
            String newRedisFp = (redisFp == null) ? loadRedisFingerprint() : redisFp;
            java.util.Map<String, SensitiveWord> cur = wordMetaCache;
            String curFp = metaFingerprint;
            if (cur != null && newRedisFp != null && newRedisFp.equals(curFp)) return cur;
            List<SensitiveWord> words = getEnabledWords();
            String newFp = fingerprintOf(words);
            java.util.Map<String, SensitiveWord> nm = new java.util.HashMap<>(words.size() * 2);
            for (SensitiveWord w : words) {
                if (w.getWord() == null) continue;
                nm.put(w.getWord(), w);
                String clean = DfaTrie.stripNoise(w.getWord());
                if (clean != null && !clean.isEmpty() && !clean.equals(w.getWord())) {
                    nm.put(clean, w);
                }
            }
            wordMetaCache = nm;
            metaFingerprint = newFp;
            return nm;
        }
    }

    /** 手动刷新缓存（管理页按钮） */
    public String refreshCache() {
        evictCache();
        List<SensitiveWord> words = wordMapper.selectAllEnabled();
        refreshCacheWithWords(words);
        return "词库缓存已刷新，共 " + words.size() + " 条启用词";
    }

    /** 把词表写入 Redis（词 JSON + 指纹 fp），不抛异常 */
    private void refreshCacheWithWords(List<SensitiveWord> words) {
        String fp = fingerprintOf(words);
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(words),
                    Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception e) {
            log.warn("[audit] 词库缓存写 Redis 失败: {}", e.getMessage());
        }
        try {
            redis.opsForValue().set(FINGERPRINT_KEY, fp, Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception ignored) {}
    }

    private void evictCache() {
        try {
            redis.delete(CACHE_KEY);
            redis.delete(FINGERPRINT_KEY);
        } catch (Exception ignored) {}
    }

    // ==================== CRUD ====================

    public PageResult<SensitiveWord> list(String keyword, String category, String level, int page, int pageSize) {
        int total = wordMapper.countByPage(keyword, category, level);
        List<SensitiveWord> items = wordMapper.selectByPage(keyword, category, level,
                (page - 1) * pageSize, pageSize);
        return PageResult.of(items, total, page, pageSize);
    }

    public SensitiveWord save(SensitiveWord w) {
        if (w.getWord() == null || w.getWord().isBlank()) throw new IllegalArgumentException("词不能为空");
        if (w.getCategory() == null) w.setCategory("OTHER");
        if (w.getLevel() == null) w.setLevel("MEDIUM");
        if (w.getMatchMode() == null) w.setMatchMode("EXACT");
        if (w.getEnabled() == null) w.setEnabled(1);
        if (w.getId() == null) {
            wordMapper.insert(w);
        } else {
            wordMapper.updateById(w);
        }
        evictCache();
        return w;
    }

    public int saveBatch(List<SensitiveWord> words) {
        if (words == null || words.isEmpty()) return 0;
        for (SensitiveWord w : words) {
            if (w.getCategory() == null) w.setCategory("OTHER");
            if (w.getLevel() == null) w.setLevel("MEDIUM");
            if (w.getMatchMode() == null) w.setMatchMode("EXACT");
            if (w.getEnabled() == null) w.setEnabled(1);
        }
        wordMapper.insertBatch(words);
        evictCache();
        return words.size();
    }

    public boolean delete(Long id) {
        int n = wordMapper.deleteById(id);
        evictCache();
        return n > 0;
    }

    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int n = wordMapper.deleteBatch(ids);
        evictCache();
        return n;
    }

    // ==================== 版本管理 ====================

    /** 发布版本：冻结当前工作区快照 */
    @Transactional
    public SensitiveWordVersion publishVersion(String remark, String adminId) {
        List<SensitiveWord> words = wordMapper.selectAllEnabled();
        SensitiveWordVersion latest = versionMapper.selectLatest();
        int nextNo = (latest == null ? 0 : latest.getVersionNo()) + 1;

        SensitiveWordVersion v = new SensitiveWordVersion();
        v.setVersionNo(nextNo);
        v.setWordCount(words.size());
        try {
            v.setSnapshotJson(objectMapper.writeValueAsString(words));
        } catch (Exception e) {
            throw new IllegalStateException("快照序列化失败: " + e.getMessage());
        }
        v.setRemark(remark);
        v.setCreatedBy(adminId);
        versionMapper.insert(v);
        try {
            redis.opsForValue().set(VERSION_KEY, String.valueOf(nextNo));
        } catch (Exception ignored) {
        }
        refreshCache();
        log.info("[audit] 敏感词版本 v{} 已发布，词条数 {}", nextNo, words.size());
        return v;
    }

    public List<SensitiveWordVersion> versions() {
        return versionMapper.selectAll();
    }

    /** 回滚到指定版本：快照写回工作区 */
    @Transactional
    public String rollback(int versionNo) {
        SensitiveWordVersion v = versionMapper.selectByVersionNo(versionNo);
        if (v == null) throw new IllegalArgumentException("版本 v" + versionNo + " 不存在");
        List<SensitiveWord> words;
        try {
            words = objectMapper.readValue(v.getSnapshotJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SensitiveWord.class));
        } catch (Exception e) {
            throw new IllegalStateException("快照解析失败: " + e.getMessage());
        }
        wordMapper.deleteAll();
        if (!words.isEmpty()) {
            wordMapper.insertBatch(words);
        }
        evictCache();
        log.info("[audit] 敏感词库已回滚到 v{}，词条数 {}", versionNo, words.size());
        return "已回滚到 v" + versionNo + "，恢复 " + words.size() + " 条";
    }

    public InterviewResponse<PageResult<SensitiveWord>> page(String keyword, String category, String level, int page, int pageSize) {
        return InterviewResponse.ok(list(keyword, category, level, page, pageSize));
    }
}
