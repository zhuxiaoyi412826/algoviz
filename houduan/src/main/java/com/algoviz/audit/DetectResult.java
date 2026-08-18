package com.algoviz.audit;

import lombok.Data;

import java.util.List;

/** 检测结果 */
@Data
public class DetectResult {

    private boolean hit;
    private String riskLevel;     // HIGH/MEDIUM/LOW/NONE
    private int totalScore;
    private String preCheck;      // BLOCK / PASS
    private String auditStatus;   // blocked / pending / logonly / none
    private List<HitDetail> hits;

    @Data
    public static class HitDetail {
        private String type;       // SENSITIVE_WORD / DANGEROUS_CODE
        private String ruleId;
        private String ruleName;   // 词本身 / 规则名
        private String hitContent;
        private int score;

        public HitDetail() {}

        public HitDetail(String type, String ruleId, String ruleName, String hitContent, int score) {
            this.type = type;
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.hitContent = hitContent;
            this.score = score;
        }
    }
}
