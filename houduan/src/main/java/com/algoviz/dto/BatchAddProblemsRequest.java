package com.algoviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 批量添加题目请求 DTO
 * 对应管理后台「批量入库」操作：把 AI 生成的若干道题写入数据库
 */
@Schema(description = "批量添加题目请求")
public class BatchAddProblemsRequest {

    /** 题目列表（来自 AI 生成结果的二次编辑结果） */
    @Schema(description = "题目列表")
    private List<AIGenerateProblemResponse.GeneratedProblem> problems;

    /** 是否覆盖 problemNo 冲突的题目（默认 false，冲突时跳过） */
    @Schema(description = "冲突时是否覆盖")
    private Boolean overwriteOnConflict;

    public List<AIGenerateProblemResponse.GeneratedProblem> getProblems() { return problems; }
    public void setProblems(List<AIGenerateProblemResponse.GeneratedProblem> problems) { this.problems = problems; }

    public Boolean getOverwriteOnConflict() { return overwriteOnConflict; }
    public void setOverwriteOnConflict(Boolean overwriteOnConflict) { this.overwriteOnConflict = overwriteOnConflict; }
}
