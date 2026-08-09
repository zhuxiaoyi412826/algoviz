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
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewProblemAdminServiceImpl implements InterviewProblemAdminService {

    private final InterviewProblemMapper problemMapper;
    private final InterviewUserMapper userMapper;

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

    // =================== B3 新增 ===================
    @Override
    @Transactional
    public InterviewProblem create(InterviewProblemSaveDTO dto, String adminId) {
        InterviewProblem p = toEntity(dto);
        validateRequired(p, false);
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
        return problemMapper.logicDelete(id, adminId) > 0;
    }

    @Override
    public int batchLogicDelete(List<Long> ids, String adminId) {
        if (ids == null || ids.isEmpty()) return 0;
        return problemMapper.batchLogicDelete(ids, adminId);
    }

    // =================== B8/B9 物理删除 ===================
    @Override
    public boolean physicalDelete(Long id) {
        return problemMapper.physicalDelete(id) > 0;
    }

    @Override
    public int batchPhysicalDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return problemMapper.batchPhysicalDelete(ids);
    }

    // =================== B10 JSON 批量导入 ===================
    @Override
    @Transactional
    public BatchImportResult batchImport(List<InterviewProblemSaveDTO> problemList,
                                         boolean overwriteOnConflict, String adminId) {
        if (problemList == null) return BatchImportResult.of(0, 0, 0, List.of());
        int total = problemList.size();
        int succ = 0;
        List<String> fails = new ArrayList<>();
        for (int i = 0; i < problemList.size(); i++) {
            InterviewProblemSaveDTO dto = problemList.get(i);
            try {
                InterviewProblem p = toEntity(dto);
                validateRequired(p, false);
                if (p.getProblemNo() == null || p.getProblemNo().isBlank()) {
                    p.setProblemNo(generateNextProblemNo());
                }
                p.setCreatedBy(adminId);
                p.setUpdatedBy(adminId);
                InterviewProblem dup = problemMapper.selectByNo(p.getProblemNo());
                if (dup == null) {
                    problemMapper.insert(p);
                    syncTags("", p.getTags(), p.getCategory());
                    succ++;
                } else if (overwriteOnConflict) {
                    String oldTags = dup.getTags();
                    p.setId(dup.getId());
                    problemMapper.updateById(p);
                    syncTags(oldTags, p.getTags(), p.getCategory());
                    succ++;
                } else {
                    fails.add("第" + (i + 1) + "条: problemNo=" + p.getProblemNo() + " 已存在(未覆盖)");
                }
            } catch (Exception e) {
                fails.add("第" + (i + 1) + "条: " + e.getMessage());
            }
        }
        return BatchImportResult.of(total, succ, fails.size(), fails);
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
