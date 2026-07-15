package com.algoviz.controller;

import com.algoviz.entity.OJProblem;
import com.algoviz.entity.Submission;
import com.algoviz.mapper.OJProblemMapper;
import com.algoviz.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@Tag(name = "提交管理", description = "OJ题目提交相关接口")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionMapper submissionMapper;
    
    @Autowired
    private OJProblemMapper problemMapper;

    @GetMapping
    @Operation(summary = "获取提交列表", description = "获取所有提交记录（管理后台使用）")
    public Map<String, Object> getAllSubmissions(
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status) {
        
        logger.info("获取提交列表 - 题目ID: {}, 状态: {}", problemId, status);
        
        Map<String, Object> result = new HashMap<>();
        List<Submission> submissions;

        if (problemId != null) {
            submissions = submissionMapper.getSubmissionsByProblemId(problemId);
        } else if (status != null && !status.isEmpty()) {
            submissions = submissionMapper.getSubmissionsByStatus(status);
        } else {
            submissions = submissionMapper.getAllSubmissions();
        }

        result.put("success", true);
        result.put("submissions", submissions);
        result.put("count", submissions.size());
        
        return result;
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户提交", description = "根据用户ID获取提交记录")
    public Map<String, Object> getSubmissionsByUserId(@PathVariable Long userId) {
        logger.info("获取用户提交：{}", userId);
        
        Map<String, Object> result = new HashMap<>();
        List<Submission> submissions = submissionMapper.getSubmissionsByUserId(userId);
        
        result.put("success", true);
        result.put("submissions", submissions);
        
        return result;
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "获取提交详情", description = "根据提交ID获取提交详情")
    public Map<String, Object> getSubmissionById(@PathVariable String submissionId) {
        logger.info("获取提交详情：{}", submissionId);
        
        Map<String, Object> result = new HashMap<>();
        Submission submission = submissionMapper.getSubmissionById(submissionId);
        
        if (submission != null) {
            result.put("success", true);
            result.put("submission", submission);
        } else {
            result.put("success", false);
            result.put("message", "提交记录不存在");
        }
        
        return result;
    }

    @PostMapping
    @Operation(summary = "提交代码", description = "用户提交代码进行判题")
    public Map<String, Object> submitCode(@RequestBody Map<String, Object> body) {
        logger.info("用户提交代码");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long problemId = ((Number) body.get("problemId")).longValue();
            Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
            String code = (String) body.get("code");
            String language = (String) body.get("language");
            String username = (String) body.get("username");
            
            OJProblem problem = problemMapper.getProblemById(Long.valueOf(String.valueOf(problemId)));
            if (problem == null) {
                result.put("success", false);
                result.put("message", "题目不存在");
                return result;
            }
            
            String submissionId = "SUB" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
            
            Submission submission = new Submission();
            submission.setSubmissionId(submissionId);
            submission.setProblemId(problemId);
            submission.setProblemTitle(problem.getTitle());
            submission.setUserId(userId);
            submission.setUsername(username);
            submission.setCode(code);
            submission.setLanguage(language);
            submission.setStatus("PENDING");
            submission.setSubmitTime(LocalDateTime.now());
            
            submissionMapper.insertSubmission(submission);
            
            // 异步执行判题
            judgeCode(submissionId, problem, code);
            
            result.put("success", true);
            result.put("message", "提交成功");
            result.put("submissionId", submissionId);
            
        } catch (Exception e) {
            logger.error("提交代码失败", e);
            result.put("success", false);
            result.put("message", "提交失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 模拟判题逻辑
     */
    private void judgeCode(String submissionId, OJProblem problem, String code) {
        new Thread(() -> {
            try {
                // 模拟判题延迟
                Thread.sleep(1000 + (long) (Math.random() * 2000));
                
                String status;
                int runtime = 0;
                int memory = 0;
                String errorMessage = null;
                String judgeLog = "";
                
                // 模拟判题结果
                double rand = Math.random();
                
                if (rand < 0.1) {
                    // 10% 编译错误
                    status = "CE";
                    errorMessage = "编译错误：语法错误";
                    judgeLog = "编译失败";
                } else if (rand < 0.15) {
                    // 5% 运行时错误
                    status = "RE";
                    errorMessage = "运行时错误：数组越界";
                    judgeLog = "运行时异常";
                } else if (rand < 0.2) {
                    // 5% 超时
                    status = "TLE";
                    runtime = 10000;
                    judgeLog = "时间限制超出";
                } else if (rand < 0.23) {
                    // 3% 内存超限
                    status = "MLE";
                    memory = 134217728; // 128MB + 1MB
                    judgeLog = "内存限制超出";
                } else if (rand < 0.35) {
                    // 12% 答案错误
                    status = "WA";
                    runtime = (int) (Math.random() * 500) + 50;
                    memory = (int) (Math.random() * 10000) + 5000;
                    judgeLog = "答案错误";
                } else {
                    // 65% 通过
                    status = "AC";
                    runtime = (int) (Math.random() * 500) + 50;
                    memory = (int) (Math.random() * 10000) + 5000;
                    judgeLog = "所有测试用例通过";
                }
                
                submissionMapper.updateSubmissionStatus(submissionId, status, runtime, memory, errorMessage, judgeLog);
                
                logger.info("判题完成 - {}: {}", submissionId, status);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("判题失败", e);
            }
        }).start();
    }

    @DeleteMapping("/{submissionId}")
    @Operation(summary = "删除提交", description = "管理员删除提交记录")
    public Map<String, Object> deleteSubmission(@PathVariable String submissionId) {
        logger.info("删除提交：{}", submissionId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Submission submission = submissionMapper.getSubmissionById(submissionId);
            if (submission == null) {
                result.put("success", false);
                result.put("message", "提交记录不存在");
                return result;
            }
            
            submissionMapper.deleteSubmission(submissionId);
            
            result.put("success", true);
            result.put("message", "删除成功");
            
        } catch (Exception e) {
            logger.error("删除提交失败", e);
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/stats/user/{userId}")
    @Operation(summary = "用户提交统计", description = "获取用户提交统计信息")
    public Map<String, Object> getUserStats(@PathVariable Long userId) {
        logger.info("获取用户提交统计：{}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        int total = submissionMapper.countByUserId(userId);
        int ac = submissionMapper.countACByUserId(userId);
        
        result.put("success", true);
        result.put("totalSubmissions", total);
        result.put("acCount", ac);
        result.put("acRate", total > 0 ? String.format("%.2f%%", (double) ac / total * 100) : "0%");
        
        return result;
    }

    @GetMapping("/stats/problem/{problemId}")
    @Operation(summary = "题目提交统计", description = "获取题目提交统计信息")
    public Map<String, Object> getProblemStats(@PathVariable Long problemId) {
        logger.info("获取题目提交统计：{}", problemId);
        
        Map<String, Object> result = new HashMap<>();
        
        int total = submissionMapper.countByProblemId(problemId);
        
        result.put("success", true);
        result.put("totalSubmissions", total);
        
        return result;
    }
}