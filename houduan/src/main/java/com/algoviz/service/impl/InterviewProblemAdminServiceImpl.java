package com.algoviz.service.impl;

import com.algoviz.dto.interview.BatchImportResult;
import com.algoviz.dto.interview.InterviewAdminStats;
import com.algoviz.dto.interview.InterviewProblemSaveDTO;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.entity.InterviewTag;
import com.algoviz.mapper.InterviewProblemMapper;
import com.algoviz.mapper.InterviewUserMapper;
import com.algoviz.service.InterviewProblemAdminService;
import com.algoviz.audit.AuditDetectService;
import com.algoviz.audit.AuditLogEntry;
import com.algoviz.audit.AuditLogService;
import com.algoviz.audit.AuditReviewService;
import com.algoviz.audit.DetectResult;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewProblemAdminServiceImpl implements InterviewProblemAdminService, ApplicationContextAware {

    private final InterviewProblemMapper problemMapper;
    private final InterviewUserMapper userMapper;
    private final com.algoviz.service.VectorSearchService vectorSearchService;
    private final AuditDetectService auditDetectService;
    private final AuditLogService auditLogService;
    private final AuditReviewService auditReviewService;

    /** 通过 ApplicationContext 获取 self 代理，确保同类内部调用时 @Transactional 生效 */
    private ApplicationContext applicationContext;
    private InterviewProblemAdminServiceImpl self;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 延迟到 setter 之后再取 bean，避免初始化期循环依赖
    }

    private InterviewProblemAdminServiceImpl self() {
        if (self == null) {
            self = applicationContext.getBean(InterviewProblemAdminServiceImpl.class);
        }
        return self;
    }

    /** 批量导入小事务：每批 50 条一提交，大幅缩短 InnoDB 行锁持有时间 */
    public static final int IMPORT_CHUNK_SIZE = 50;

    // =================== 工具 ===================
    /** 允许 Markdown 常用标签（允许 img 但仅使用 http/https/data 协议） */
    private static final Safelist SAFE = Safelist.relaxed()
            .addTags("h1","h2","h3","h4","h5","h6","pre","code","blockquote","table","thead","tbody","tr","th","td")
            .addAttributes("code", "class")
            .addAttributes("a","target")
            .addProtocols("img","src","http","https","data");

    private static String cleanXss(String s) {
        if (s == null) return null;
        // 保留换行
        return Jsoup.clean(s, SAFE);
    }

    /** Object (List/Array/String) -> 逗号分隔字符串 */
    private static String tagsToStr(Object tags) {
        if (tags == null) return "";
        if (tags instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","));
        }
        if (tags.getClass().isArray()) {
            String[] arr = (String[]) tags;
            return String.join(",", arr);
        }
        return tags.toString();
    }

    /** 规范化：允许 ACTIVE/INACTIVE，也兼容 published/draft/online/offline/1/0 */
    private static String normalizeStatus(String s) {
        if (s == null) return "ACTIVE";
        switch (s.toLowerCase()) {
            case "published":
            case "online":
            case "1":
            case "active":
                return "ACTIVE";
            case "draft":
            case "offline":
            case "0":
            case "inactive":
                return "INACTIVE";
            default:
                return s;
        }
    }

    private static String normalizeDifficulty(String s) {
        if (s == null || s.isEmpty()) return "easy";
        switch (s.toLowerCase()) {
            case "easy":
            case "简单":
            case "e":
                return "easy";
            case "medium":
            case "中等":
            case "m":
            case "middle":
                return "medium";
            case "hard":
            case "困难":
            case "h":
                return "hard";
            default:
                return s;
        }
    }

    private static int normalizeFreq(Integer i) {
        if (i == null) return 0;
        return i == 0 ? 0 : 1;
    }

    /** 同步标签：新增计数 / 新增标签 */
    @Transactional
    public void syncTags(String oldTags, String newTags, String category) {
        // -1 for removed, +1 for added
        Set<String> old = splitTagSet(oldTags);
        Set<String> new_ = splitTagSet(newTags);
        Set<String> removed = new HashSet<>(old);
        removed.removeAll(new_);
        Set<String> added = new HashSet<>(new_);
        added.removeAll(old);

        for (String t : removed) {
            userMapper.incTagUseCount(t, -1);
        }
        for (String t : added) {
            InterviewTag exist = userMapper.findTagByName(t);
            if (exist == null) {
                InterviewTag n = new InterviewTag();
                n.setName(t);
                n.setCategory(category == null ? "" : category);
                n.setSortOrder(0);
                userMapper.insertTag(n); // use_count=1 by default in SQL
            } else {
                userMapper.incTagUseCount(t, 1);
            }
        }
    }

    private static Set<String> splitTagSet(String tags) {
        Set<String> s = new LinkedHashSet<>();
        if (tags == null || tags.isEmpty()) return s;
        for (String t : tags.split("[,，;；|]")) {
            t = t.trim();
            if (!t.isEmpty()) s.add(t);
        }
        return s;
    }

    /** DTO -> Entity（不含 ID） */
    private InterviewProblem toEntity(InterviewProblemSaveDTO dto) {
        InterviewProblem p = new InterviewProblem();
        p.setProblemNo(dto.getProblemNo());
        p.setTitle(cleanXss(dto.getTitle()));
        p.setDifficulty(normalizeDifficulty(dto.getDifficulty()));
        p.setCategory(cleanXss(dto.getCategory()));
        String tagsStr = tagsToStr(dto.getTags());
        p.setTags(tagsStr);
        p.setDescription(cleanXss(dto.getDescription()));
        p.setInputFormat(cleanXss(dto.getInputFormat()));
        p.setOutputFormat(cleanXss(dto.getOutputFormat()));
        p.setSolution(cleanXss(dto.getSolution()));
        p.setStatus(normalizeStatus(dto.getStatus()));
        p.setIsFrequent(normalizeFreq(dto.getIsFrequent()));
        return p;
    }

    private void validateRequired(InterviewProblem p, boolean checkSolution) {
        if (p.getTitle() == null || p.getTitle().isBlank()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (checkSolution && (p.getSolution() == null || p.getSolution().isBlank())) {
            throw new IllegalArgumentException("solution 不能为空");
        }
        if (!List.of("easy","medium","hard").contains(p.getDifficulty())) {
            throw new IllegalArgumentException("difficulty 必须是 easy/medium/hard");
        }
    }

    // =================== B1 列表 ===================
    @Override
    public PageResult<InterviewProblem> list(String keyword, String tag, String difficulty, String category,
                                              String status, Integer isFrequent,
                                              String sortBy, String order, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        int offset = (page - 1) * pageSize;
        List<InterviewProblem> list = problemMapper.selectAdminList(
                keyword, tag, difficulty, category, status, isFrequent, sortBy, order, offset, pageSize);
        int total = problemMapper.countAdminList(keyword, tag, difficulty, category, status, isFrequent);
        return PageResult.of(list, total, page, pageSize);
    }

    // =================== B2 详情 ===================
    @Override
    public InterviewProblem getById(Long id) {
        return problemMapper.selectById(id);
    }

    // =================== 关键词屏蔽检测 ===================

    /** 题目全文（供检测） */
    private static String auditText(InterviewProblem p) {
        StringBuilder sb = new StringBuilder();
        if (p.getTags() != null) sb.append(p.getTags()).append('\n');
        if (p.getCategory() != null) sb.append(p.getCategory()).append('\n');
        if (p.getDescription() != null) sb.append(p.getDescription()).append('\n');
        if (p.getSolution() != null) sb.append(p.getSolution());
        return sb.toString();
    }

    /** 截断内容快照 */
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** 检测 + BLOCK 时抛异常拦截（拦截前输出审计日志并落库） */
    private DetectResult auditCheck(InterviewProblem p) {
        DetectResult d;
        try {
            d = auditDetectService.detect("QUESTION", "ALL", p.getTitle(), auditText(p));
        } catch (Exception e) {
            // 审核系统故障不阻断业务
            return null;
        }
        if (d.isHit()) {
            AuditLogEntry e = new AuditLogEntry();
            e.setSubmitId(AuditLogService.nextSubmitId("q"));
            e.setContentType("QUESTION");
            e.setTitle(truncate(p.getTitle(), 100));
            e.setContent(truncate(p.getTitle() + "\n" + auditText(p), 500));
            e.setRiskLevel(d.getRiskLevel());
            e.setTotalScore(d.getTotalScore());
            e.setHitDetails(d.getHits());
            e.setPreCheck(d.getPreCheck());
            e.setAuditStatus(d.getAuditStatus());
            if ("BLOCK".equals(d.getPreCheck())) {
                auditLogService.log(e);
                auditReviewService.recordBlocked(e, d);
                String words = d.getHits().stream()
                        .map(DetectResult.HitDetail::getRuleName)
                        .limit(5).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("内容命中高危关键词/危险代码，已拦截: " + words);
            }
            // pending / logonly：放行，插入成功后补日志（带题目ID）
            auditLogService.log(e);
        }
        return d;
    }

    // =================== B3 新增 ===================
    @Override
    @Transactional
    public InterviewProblem create(InterviewProblemSaveDTO dto, String adminId) {
        InterviewProblem p = toEntity(dto);
        validateRequired(p, false);
        // 关键词屏蔽检测（BLOCK 直接拦截）
        auditCheck(p);
        if (p.getProblemNo() == null || p.getProblemNo().isBlank()) {
            p.setProblemNo(generateNextProblemNo());
        } else {
            // 重复校验
            if (problemMapper.selectByNo(p.getProblemNo()) != null) {
                throw new IllegalArgumentException("problemNo 已存在: " + p.getProblemNo());
            }
        }
        p.setCreatedBy(adminId);
        p.setUpdatedBy(adminId);
        problemMapper.insert(p);
        syncTags("", p.getTags(), p.getCategory());
        // 自动同步到向量库
        vectorSearchService.syncSingle(p);
        // 自动同步到 ES 索引
        vectorSearchService.esSyncSingle(p);
        return p;
    }

    // =================== B4 修改 ===================
    @Override
    @Transactional
    public boolean update(Long id, InterviewProblemSaveDTO dto, String adminId) {
        InterviewProblem exist = problemMapper.selectById(id);
        if (exist == null) return false;
        InterviewProblem p = toEntity(dto);
        validateRequired(p, false);
        // problemNo 不能和其他重复
        if (p.getProblemNo() != null && !p.getProblemNo().isBlank()) {
            InterviewProblem dup = problemMapper.selectByNo(p.getProblemNo());
            if (dup != null && !dup.getId().equals(id)) {
                throw new IllegalArgumentException("problemNo 已被占用: " + p.getProblemNo());
            }
        } else {
            p.setProblemNo(exist.getProblemNo());
        }
        p.setId(id);
        p.setUpdatedBy(adminId);
        int affected = problemMapper.updateById(p);
        if (affected > 0) {
            syncTags(exist.getTags(), p.getTags(), p.getCategory());
            // 自动同步到向量库
            vectorSearchService.syncSingle(p);
            // 自动同步到 ES 索引
            vectorSearchService.esSyncSingle(p);
            return true;
        }
        return false;
    }

    // =================== B5 状态 ===================
    @Override
    public boolean updateStatus(Long id, String status, String adminId) {
        String s = normalizeStatus(status);
        return problemMapper.updateStatus(id, s, adminId) > 0;
    }

    // =================== B6/B7 逻辑删除 ===================
    @Override
    public boolean logicDelete(Long id, String adminId) {
        boolean ok = problemMapper.logicDelete(id, adminId) > 0;
        if (ok) {
            // 自动从向量库删除
            vectorSearchService.delete(id);
            // 自动从 ES 索引删除
            vectorSearchService.esDelete(id);
        }
        return ok;
    }

    @Override
    public int batchLogicDelete(List<Long> ids, String adminId) {
        if (ids == null || ids.isEmpty()) return 0;
        int n = problemMapper.batchLogicDelete(ids, adminId);
        if (n > 0) {
            // 自动从向量库删除
            vectorSearchService.deleteBatch(ids);
            // 自动从 ES 索引删除
            vectorSearchService.esDeleteBatch(ids);
        }
        return n;
    }

    // =================== B8/B9 物理删除 ===================
    @Override
    public boolean physicalDelete(Long id) {
        boolean ok = problemMapper.physicalDelete(id) > 0;
        if (ok) {
            // 自动从向量库删除
            vectorSearchService.delete(id);
            // 自动从 ES 索引删除
            vectorSearchService.esDelete(id);
        }
        return ok;
    }

    @Override
    public int batchPhysicalDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int n = problemMapper.batchPhysicalDelete(ids);
        if (n > 0) {
            // 自动从向量库删除
            vectorSearchService.deleteBatch(ids);
            // 自动从 ES 索引删除
            vectorSearchService.esDeleteBatch(ids);
        }
        return n;
    }

    // =================== B10 JSON 批量导入 ===================
    // 注意：去掉外层大 @Transactional，避免长事务把 InnoDB 行锁持有到分钟级
    // 改为：
    //   1) 预处理 & 关键词检测（无 DB 事务）
    //   2) 分块 IMPORT_CHUNK_SIZE 条，每块通过 self.saveChunkTransactional()
    //      在独立小事务里完成 selectByNo+insert/update+syncTags，一提交立即释放锁
    //   3) 所有入库提交完成后，再调用 vectorSearchService.syncBatch / esSyncBatch
    //      二者已标注 @Async，在线程池里异步执行，完全不占用 MySQL 连接和锁
    @Override
    public BatchImportResult batchImport(List<InterviewProblemSaveDTO> problemList,
                                         boolean overwriteOnConflict, String adminId) {
        if (problemList == null || problemList.isEmpty()) {
            return BatchImportResult.of(0, 0, 0, List.of());
        }
        final int total = problemList.size();
        final List<String> fails = new ArrayList<>();
        // 收集成功入库的题目（用于向量/ES 异步同步）
        final List<InterviewProblem> syncedProblems = new ArrayList<>();
        int succCount = 0;

        // ========== 阶段 1：逐条进行实体转换 & 关键词屏蔽检测（非事务） ==========
        List<ChunkItem> allValidItems = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            InterviewProblemSaveDTO dto = problemList.get(i);
            try {
                InterviewProblem p = toEntity(dto);
                validateRequired(p, false);
                // 关键词屏蔽检测：BLOCK 拦截该条（不阻断整批）
                try {
                    auditCheck(p);
                } catch (IllegalArgumentException be) {
                    fails.add("第" + (i + 1) + "条: " + be.getMessage());
                    continue;
                }
                if (p.getProblemNo() == null || p.getProblemNo().isBlank()) {
                    p.setProblemNo(generateNextProblemNo());
                }
                p.setCreatedBy(adminId);
                p.setUpdatedBy(adminId);
                // 构造外层 ChunkItem（注意 dupIfExist 下一阶段才填充）
                allValidItems.add(new ChunkItem(i + 1, p, null));
            } catch (Exception e) {
                fails.add("第" + (i + 1) + "条: " + e.getMessage());
            }
        }

        // 收集所有有效记录的 problem_no，一次性批量 SELECT 查出已有冲突
        Set<String> existNoSet = new HashSet<>();
        if (!allValidItems.isEmpty()) {
            List<String> nos = allValidItems.stream()
                    .map(it -> it.entity.getProblemNo())
                    .distinct()
                    .toList();
            // 分批查询（IN 列表过大时分批，避免 IN 超长 SQL）
            for (int i = 0; i < nos.size(); i += 200) {
                int end = Math.min(i + 200, nos.size());
                List<String> sub = nos.subList(i, end);
                List<InterviewProblem> founds = problemMapper.selectByNos(sub);
                for (InterviewProblem f : founds) existNoSet.add(f.getProblemNo());
            }
            // 对冲突项查出 dup 实体填入 ChunkItem.dupIfExist
            for (ChunkItem it : allValidItems) {
                if (existNoSet.contains(it.entity.getProblemNo())) {
                    it.dupIfExist = problemMapper.selectByNo(it.entity.getProblemNo());
                }
            }
        }

        // ========== 阶段 2：分块写入，每块独立小事务，写一块提交一块 ==========
        int chunks = (allValidItems.size() + IMPORT_CHUNK_SIZE - 1) / IMPORT_CHUNK_SIZE;
        for (int c = 0; c < chunks; c++) {
            int from = c * IMPORT_CHUNK_SIZE;
            int to = Math.min(from + IMPORT_CHUNK_SIZE, allValidItems.size());
            List<ChunkItem> chunk = allValidItems.subList(from, to);
            try {
                ChunkSaveResult chunkRes = self().saveChunkTransactional(
                        new ArrayList<>(chunk), overwriteOnConflict
                );
                succCount += chunkRes.successCount;
                fails.addAll(chunkRes.failMessages);
                syncedProblems.addAll(chunkRes.syncedProblems);
            } catch (Exception e) {
                // 整块异常：按块内每条分别添加失败
                for (ChunkItem it : chunk) {
                    fails.add("第" + it.idx1Based + "条: 入库事务异常, " + e.getMessage());
                }
            }
        }

        // ========== 阶段 3：全部数据落库 & 提交后，异步触发向量/ES 同步 ==========
        // syncBatch / esSyncBatch 现在带 @Async("syncTaskExecutor")，会在新线程里执行，
        // 调用后立即返回，进度通过 SyncProgressHolder 轮询。
        if (!syncedProblems.isEmpty()) {
            List<InterviewProblem> copy = new ArrayList<>(syncedProblems);
            vectorSearchService.syncBatch(copy);
            vectorSearchService.esSyncBatch(copy);
        }

        return BatchImportResult.of(total, succCount, fails.size(), fails);
    }

    /** 单块批量写入的返回值 */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChunkSaveResult {
        int successCount;
        List<String> failMessages;
        List<InterviewProblem> syncedProblems;
    }

    /**
     * 单块写入事务（每个事务 ~IMPORT_CHUNK_SIZE 条）：
     *   - 事务传播 REQUIRES_NEW：每次调用都开启新事务并在结束时提交
     *   - 完成即释放 InnoDB 行锁/gap lock，避免 Lock wait timeout
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ChunkSaveResult saveChunkTransactional(
            List<ChunkItem> items,
            boolean overwriteOnConflict
    ) {
        int successCount = 0;
        List<String> failMessages = new ArrayList<>();
        List<InterviewProblem> syncedProblems = new ArrayList<>();
        for (ChunkItem it : items) {
            InterviewProblem p = it.entity;
            try {
                InterviewProblem dup = it.dupIfExist;
                if (dup == null) {
                    // 新增：写入后 id 自动回写
                    problemMapper.insert(p);
                    syncTags("", p.getTags(), p.getCategory());
                    syncedProblems.add(p);
                    successCount++;
                } else if (overwriteOnConflict) {
                    String oldTags = dup.getTags();
                    p.setId(dup.getId());
                    problemMapper.updateById(p);
                    syncTags(oldTags, p.getTags(), p.getCategory());
                    syncedProblems.add(p);
                    successCount++;
                } else {
                    failMessages.add("第" + it.idx1Based + "条: problemNo=" + p.getProblemNo() + " 已存在(未覆盖)");
                }
            } catch (Exception e) {
                failMessages.add("第" + it.idx1Based + "条: 写入异常, " + e.getMessage());
            }
        }
        return new ChunkSaveResult(successCount, failMessages, syncedProblems);
    }

    /** 批量写入块的单条记录（外层静态内部类，供 saveChunkTransactional 跨方法共用） */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChunkItem {
        /** 1-based 原始序号 */
        public int idx1Based;
        /** 已转换好的实体（必定不为 null） */
        public InterviewProblem entity;
        /** 若已存在冲突，这里为 DB 中的旧实体；否则为 null */
        public InterviewProblem dupIfExist;
    }

    // =================== B12 导出 ===================
    @Override
    public List<InterviewProblem> listAllForExport(String difficulty, String category) {
        return problemMapper.listAllForExport(difficulty, category);
    }

    // =================== B13 AI 生成（mock：基于关键词造题） ===================
    @Override
    public List<InterviewProblemSaveDTO> generateByAI(String category, String difficulty, int num) {
        num = Math.max(1, Math.min(num, 10));
        List<String> topics;
        if (category == null || category.isBlank()) {
            topics = List.of("数组与字符串","链表","二叉树","动态规划","贪心","回溯","图论","哈希表","双指针","滑动窗口");
        } else {
            topics = List.of(category);
        }
        List<String> diffs = List.of("easy","medium","hard");
        String d = (difficulty == null || difficulty.isBlank()) ? "medium" : normalizeDifficulty(difficulty);

        List<InterviewProblemSaveDTO> out = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            String topic = topics.get(i % topics.size());
            String dd = diffs.get((i + diffs.indexOf(d)) % 3);
            InterviewProblemSaveDTO r = new InterviewProblemSaveDTO();
            r.setDifficulty(dd);
            r.setCategory(topic);
            r.setTitle(topic + "练习 - " + (i + 1));
            r.setTags(List.of(topic, "AI生成"));
            r.setDescription("### 题目描述\n" +
                    "请设计一个算法，解决「" + topic + "」相关的问题。\n\n" +
                    "**约束**：\n- 输入可能为空，时间复杂度尽量优于 O(n^2)。");
            r.setInputFormat("一行输入，包含若干整数，空格分隔。");
            r.setOutputFormat("输出满足条件的结果，按空格分隔。");
            r.setSolution("### 思路\n使用经典思路结合边界条件处理即可。\n\n```java\n// TODO: 代码实现\n```");
            r.setStatus("INACTIVE");
            r.setIsFrequent(0);
            out.add(r);
        }
        return out;
    }

    // =================== B14 批量保存 AI 结果 ===================
    @Override
    public BatchImportResult batchSaveAIGenerated(List<InterviewProblemSaveDTO> problemList, String adminId) {
        return batchImport(problemList, false, adminId);
    }

    // =================== B15 统计 ===================
    @Override
    public InterviewAdminStats adminStats() {
        PageResult<InterviewProblem> all = list(null, null, null, null, null, null,
                "id", "desc", 1, Integer.MAX_VALUE);
        long total = all.getTotal();
        InterviewAdminStats s = new InterviewAdminStats();
        s.setTotalNum(total);
        long active = 0, inactive = 0, freq = 0, easy = 0, medium = 0, hard = 0;
        for (InterviewProblem p : all.getList()) {
            if ("ACTIVE".equals(p.getStatus())) active++; else inactive++;
            if (p.getIsFrequent() != null && p.getIsFrequent() == 1) freq++;
            if ("easy".equals(p.getDifficulty())) easy++;
            else if ("medium".equals(p.getDifficulty())) medium++;
            else if ("hard".equals(p.getDifficulty())) hard++;
        }
        s.setActiveNum(active);
        s.setInactiveNum(inactive);
        s.setFrequentNum(freq);
        s.setEasyNum(easy);
        s.setMediumNum(medium);
        s.setHardNum(hard);
        return s;
    }

    // =================== 工具方法 ===================
    @Override
    public String generateNextProblemNo() {
        long max = problemMapper.getMaxNumericProblemNo();
        return String.format("MS%04d", max + 1);
    }
}
