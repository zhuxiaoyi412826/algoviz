package com.algoviz.service;

import com.algoviz.entity.OJProblem;

import java.util.List;

public interface OJProblemService {
    
    List<OJProblem> getAllProblems();
    
    OJProblem getProblemById(String id);
    
    OJProblem getProblemByNo(String problemNo);
    
    List<OJProblem> searchProblems(String keyword);
    
    List<OJProblem> getProblemsByDifficulty(String difficulty);
    
    List<OJProblem> getActiveProblems();
    
    void addProblem(OJProblem problem);
    
    void updateProblem(OJProblem problem);
    
    void deleteProblem(String id);
    
    void incrementSubmission(String problemId);
    
    int countProblems();

    int countByDifficulty(String difficulty);

    /**
     * 生成下一个可用的题号（数据库最大题号 + 1，跳过冲突）
     */
    String generateNextProblemNo();

    /**
     * 批量生成 N 个连续题号（max+1, max+2, ...），用于 AI 一次性生成多道题
     */
    List<String> generateNextProblemNos(int count);
}