package com.algoviz.audit;

import com.algoviz.entity.DangerousCodeRule;
import com.algoviz.entity.SensitiveWord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 内容检测入口：敏感词 DFA + 危险代码正则 双层检测
 *
 * 等级语义：
 *  HIGH   → preCheck=BLOCK  直接拦截（auditStatus=blocked）
 *  MEDIUM → preCheck=PASS   放行但进入人工审核（auditStatus=pending）
 *  LOW    → preCheck=PASS   放行仅记录（auditStatus=logonly）
 */
@Service
@RequiredArgsConstructor
public class AuditDetectService {

    private final SensitiveWordService wordService;
    private final DangerousCodeRuleService ruleService;

    private static final int SCORE_HIGH = 90;
    private static final int SCORE_MEDIUM = 60;
    private static final int SCORE_LOW = 30;

    /**
     * @param contentType QUESTION/CODE/COMMENT
     * @param language    编程语言（CODE 场景过滤规则用），题目文本传 ALL
     * @param title       标题
     * @param content     正文（描述/解答/代码）
     */
    public DetectResult detect(String contentType, String language, String title, String content) {
        DetectResult r = new DetectResult();
        List<DetectResult.HitDetail> hits = new ArrayList<>();
        int score = 0;
        String maxLevel = "NONE";

        String fullText = ((title == null ? "" : title) + "\n" + (content == null ? "" : content));

        // ===== 第一层：敏感词 DFA =====
        List<DfaTrie.Hit> wordHits = wordService.getTrie().matchAll(fullText);
        if (!wordHits.isEmpty()) {
            java.util.Map<String, SensitiveWord> meta = wordService.getWordMeta();
            for (DfaTrie.Hit h : wordHits) {
                SensitiveWord w = meta.get(h.word());
                String level = w == null ? "MEDIUM" : (w.getLevel() == null ? "MEDIUM" : w.getLevel());
                int s = switch (level) {
                    case "HIGH" -> SCORE_HIGH;
                    case "LOW" -> SCORE_LOW;
                    default -> SCORE_MEDIUM;
                };
                String category = w == null ? "OTHER" : w.getCategory();
                hits.add(new DetectResult.HitDetail("SENSITIVE_WORD",
                        w == null ? String.valueOf(h.start()) : String.valueOf(w.getId()),
                        h.word(), "[" + category + "/" + level + "]", s));
                score += s;
                maxLevel = maxLevel(maxLevel, level);
            }
        }

        // ===== 第二层：危险代码正则 =====
        for (DangerousCodeRuleService.CompiledRule cr : ruleService.getCompiledRules()) {
            DangerousCodeRule rule = cr.rule();
            if (!"ALL".equalsIgnoreCase(rule.getLanguage())
                    && language != null && !language.equalsIgnoreCase(rule.getLanguage())) {
                continue;
            }
            Matcher m = cr.pattern().matcher(fullText);
            if (m.find()) {
                String hitContent = m.group();
                if (hitContent.length() > 60) hitContent = hitContent.substring(0, 60) + "...";
                hits.add(new DetectResult.HitDetail("DANGEROUS_CODE", rule.getRuleCode(),
                        rule.getRuleName(), hitContent, rule.getScore()));
                score += rule.getScore();
                maxLevel = maxLevel(maxLevel, rule.getRiskLevel());
            }
        }

        r.setHit(!hits.isEmpty());
        r.setTotalScore(Math.min(score, 1000));
        r.setRiskLevel(maxLevel);
        r.setHits(hits);
        if (!r.isHit()) {
            r.setPreCheck("PASS");
            r.setAuditStatus("none");
        } else if ("HIGH".equals(maxLevel)) {
            r.setPreCheck("BLOCK");
            r.setAuditStatus("blocked");
        } else if ("MEDIUM".equals(maxLevel)) {
            r.setPreCheck("PASS");
            r.setAuditStatus("pending");
        } else {
            r.setPreCheck("PASS");
            r.setAuditStatus("logonly");
        }
        return r;
    }

    /** 等级取高 */
    private static String maxLevel(String a, String b) {
        int av = "HIGH".equals(a) ? 3 : "MEDIUM".equals(a) ? 2 : "LOW".equals(a) ? 1 : 0;
        int bv = "HIGH".equals(b) ? 3 : "MEDIUM".equals(b) ? 2 : "LOW".equals(b) ? 1 : 0;
        return av >= bv ? a : b;
    }
}
