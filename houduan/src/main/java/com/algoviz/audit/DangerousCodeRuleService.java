package com.algoviz.audit;

import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.DangerousCodeRule;
import com.algoviz.mapper.DangerousCodeRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 危险代码规则服务：CRUD + 内存正则缓存（指纹变化自动重建）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DangerousCodeRuleService {

    private final DangerousCodeRuleMapper ruleMapper;

    private volatile List<CompiledRule> compiled;
    private volatile String fingerprint = "";

    /** 预编译规则（language=ALL 或与提交语言一致时生效） */
    public record CompiledRule(DangerousCodeRule rule, Pattern pattern) {}

    public List<CompiledRule> getCompiledRules() {
        List<DangerousCodeRule> rules = ruleMapper.selectAllEnabled();
        String fp = rules.size() + "@" + (rules.isEmpty() ? "-" : rules.get(rules.size() - 1).getUpdateTime());
        List<CompiledRule> c = compiled;
        if (c == null || !fp.equals(fingerprint)) {
            java.util.List<CompiledRule> nc = new java.util.ArrayList<>();
            for (DangerousCodeRule r : rules) {
                try {
                    Pattern p = "KEYWORD".equals(r.getRuleType())
                            ? Pattern.compile(Pattern.quote(r.getRuleContent()))
                            : Pattern.compile(r.getRuleContent());
                    nc.add(new CompiledRule(r, p));
                } catch (Exception e) {
                    log.warn("[audit] 危险规则 {} 正则编译失败，已跳过: {}", r.getRuleCode(), e.getMessage());
                }
            }
            compiled = nc;
            fingerprint = fp;
            c = nc;
        }
        return c;
    }

    public PageResult<DangerousCodeRule> list(String keyword, String language, String riskLevel, int page, int pageSize) {
        int total = ruleMapper.countByPage(keyword, language, riskLevel);
        List<DangerousCodeRule> items = ruleMapper.selectByPage(keyword, language, riskLevel,
                (page - 1) * pageSize, pageSize);
        return PageResult.of(items, total, page, pageSize);
    }

    public DangerousCodeRule save(DangerousCodeRule r) {
        if (r.getRuleCode() == null || r.getRuleCode().isBlank()) throw new IllegalArgumentException("ruleCode 不能为空");
        if (r.getRuleContent() == null || r.getRuleContent().isBlank()) throw new IllegalArgumentException("规则内容不能为空");
        // 预校验正则合法性
        try {
            if (!"KEYWORD".equals(r.getRuleType())) {
                Pattern.compile(r.getRuleContent());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("正则不合法: " + e.getMessage());
        }
        if (r.getLanguage() == null) r.setLanguage("ALL");
        if (r.getRuleType() == null) r.setRuleType("REGEX");
        if (r.getRiskLevel() == null) r.setRiskLevel("HIGH");
        if (r.getScore() == null) r.setScore(80);
        if (r.getEnabled() == null) r.setEnabled(1);
        if (r.getId() == null) {
            ruleMapper.insert(r);
        } else {
            ruleMapper.updateById(r);
        }
        fingerprint = ""; // 强制重建
        return r;
    }

    public boolean delete(Long id) {
        int n = ruleMapper.deleteById(id);
        fingerprint = "";
        return n > 0;
    }
}
