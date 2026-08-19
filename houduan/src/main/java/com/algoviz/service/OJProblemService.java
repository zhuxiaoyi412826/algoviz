package com.algoviz.service;

import com.algoviz.entity.OJProblem;
import com.algoviz.entity.PageResult;

import java.util.List;

public interface OJProblemService {
    
    /**
     * 分页查询题目（数据库层面排序+分页）
     * @param keyword 搜索关键词（题号、标题、标签）
     * @param difficulty 难度筛选
     * @param status 状态筛选
     * @param sort 排序方式：ASC/DESC
     * @param sortBy 排序字段：id/createdAt
     * @param page 页码（从1开始）
     * @param size 每页大小
     */
    PageResult<OJProblem> getProblemsByPage(
        String keyword, 
        String difficulty, 
        String status,
        String sort, 
        String sortBy, 
        int page, 
        int size
    );

    List<OJProblem> getAllProblems(String sort, String sortBy);
    
    OJProblem getProblemById(String id);
    
    OJProblem getProblemByNo(String problemNo);
    
    List<OJProblem> searchProblems(String keyword, String sort, String sortBy);
    
    List<OJProblem> getProblemsByDifficulty(String difficulty, String sort, String sortBy);
    
    List<OJProblem> getActiveProblems(String sort, String sortBy);
    
    void addProblem(OJProblem problem);
    
    void updateProblem(OJProblem problem);

    /** 仅更新题目状态（上线/下线），避免全字段更新导致非空字段被置 null */
    boolean updateStatus(String id, String status);
    
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