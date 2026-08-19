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
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(words),
                    Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception ignored) {
        }
        return words;
    }

    /** 获取 DFA（带内存缓存，词表变化自动重建） */
    public DfaTrie getTrie() {
        List<SensitiveWord> words = getEnabledWords();
        String fp = words.size() + "@" + (words.isEmpty() ? "-" : words.get(words.size() - 1).getId() + "@" + words.get(0).getId());
        DfaTrie t = trieCache;
        if (t == null || !fp.equals(trieFingerprint)) {
            DfaTrie nt = new DfaTrie();
            for (SensitiveWord w : words) {
                nt.insert(w.getWord());
            }
            trieCache = nt;
            trieFingerprint = fp;
            t = nt;
        }
        return t;
    }

    /** word -> 元信息（等级/分类）：同时放入原词形 + stripNoise 干净词形键，
     *  因为 DfaTrie.Hit.word 在 FUZZY 模式下返回的是干净词形，需要能正确映射回原始行。*/
    public java.util.Map<String, SensitiveWord> getWordMeta() {
        List<SensitiveWord> words = getEnabledWords();
        String fp = words.size() + "@" + (words.isEmpty() ? "-" : words.get(words.size() - 1).getId());
        java.util.Map<String, SensitiveWord> m = wordMetaCache;
        if (m == null || !fp.equals(metaFingerprint)) {
            java.util.Map<String, SensitiveWord> nm = new java.util.HashMap<>();
            for (SensitiveWord w : words) {
                nm.put(w.getWord(), w);
                String clean = DfaTrie.stripNoise(w.getWord());
                if (clean != null && !clean.isEmpty() && !clean.equals(w.getWord())) {
                    // 不同词若剥噪声后冲突，后写会覆盖 —— 通常等级一致，若不同建议在 save 时加唯一性校验
                    nm.put(clean, w);
                }
            }
            wordMetaCache = nm;
            metaFingerprint = fp;
            m = nm;
        }
        return m;
    }

    /** 手动刷新缓存（管理页按钮） */
    public String refreshCache() {
        evictCache();
        List<SensitiveWord> words = wordMapper.selectAllEnabled();
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(words),
                    Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception e) {
            return "词表已加载(" + words.size() + "条)，Redis 写入失败: " + e.getMessage();
        }
        return "词库缓存已刷新，共 " + words.size() + " 条启用词";
    }

    private void evictCache() {
        try {
            redis.delete(CACHE_KEY);
        } catch (Exception ignored) {
        }
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
