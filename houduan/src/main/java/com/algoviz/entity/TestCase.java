package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "测试用例实体")
public class TestCase {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "题目ID")
    private String problemId;
    @Schema(description = "输入")
    private String input;
    @Schema(description = "预期输出")
    private String output;
    @Schema(description = "分值")
    private Integer score;
    @Schema(description = "是否为示例用例")
    private Boolean isSample;

    public TestCase() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getIsSample() { return isSample; }
    public void setIsSample(Boolean isSample) { this.isSample = isSample; }
}