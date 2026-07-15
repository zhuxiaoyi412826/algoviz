package com.algoviz.entity;

public class TestCase {
    private String id;
    private String problemId;
    private String input;
    private String output;
    private Integer score;
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
