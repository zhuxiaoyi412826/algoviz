package com.algoviz.mapper;

import com.algoviz.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubmissionMapper {
    
    /**
     * 获取所有提交记录
     */
    List<Submission> getAllSubmissions();
    
    /**
     * 根据用户ID获取提交记录
     */
    List<Submission> getSubmissionsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据题目ID获取提交记录
     */
    List<Submission> getSubmissionsByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据状态获取提交记录
     */
    List<Submission> getSubmissionsByStatus(@Param("status") String status);
    
    /**
     * 根据提交ID获取提交记录
     */
    Submission getSubmissionById(@Param("submissionId") String submissionId);
    
    /**
     * 插入提交记录
     */
    int insertSubmission(Submission submission);
    
    /**
     * 更新提交状态
     */
    int updateSubmissionStatus(@Param("submissionId") String submissionId, 
                               @Param("status") String status,
                               @Param("runtime") Integer runtime,
                               @Param("memory") Integer memory,
                               @Param("errorMessage") String errorMessage,
                               @Param("judgeLog") String judgeLog);
    
    /**
     * 删除提交记录
     */
    int deleteSubmission(@Param("submissionId") String submissionId);
    
    /**
     * 统计用户提交数量
     */
    int countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计题目提交数量
     */
    int countByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 统计AC数量
     */
    int countACByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户最近提交
     */
    List<Submission> getRecentSubmissionsByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}