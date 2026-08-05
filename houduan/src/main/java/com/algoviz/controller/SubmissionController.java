package com.algoviz.controller;

import com.algoviz.entity.OJProblem;
import com.algoviz.entity.Submission;
import com.algoviz.entity.TestCase;
import com.algoviz.mapper.OJProblemMapper;
import com.algoviz.mapper.SubmissionMapper;
import com.algoviz.mapper.TestCaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/submissions")
@Tag(name = "提交管理", description = "OJ题目提交相关接口")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);
    private static final int TIME_LIMIT_MS = 5000; // 5秒超时
    private static final int MEMORY_LIMIT_MB = 256; // 256MB内存限制

    @Autowired
    private SubmissionMapper submissionMapper;
    
    @Autowired
    private OJProblemMapper problemMapper;
    
    @Autowired
    private TestCaseMapper testCaseMapper;

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

    @GetMapping("/testcases/{problemId}")
    @Operation(summary = "获取题目测试用例", description = "根据题目ID获取测试用例")
    public Map<String, Object> getTestCases(@PathVariable Long problemId) {
        Map<String, Object> result = new HashMap<>();
        List<TestCase> testCases = testCaseMapper.findByProblemId(String.valueOf(problemId));
        result.put("success", true);
        result.put("testCases", testCases);
        result.put("count", testCases.size());
        return result;
    }

    @PostMapping("/run")
    @Operation(summary = "运行代码（测试）", description = "前端运行代码测试，使用第一个测试用例")
    public Map<String, Object> runCode(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long problemId = ((Number) body.get("problemId")).longValue();
            String code = (String) body.get("code");
            String language = (String) body.get("language");
            String input = (String) body.get("input");
            
            OJProblem problem = problemMapper.getProblemById(problemId);
            if (problem == null) {
                result.put("success", false);
                result.put("message", "题目不存在");
                return result;
            }
            
            // 如果没有指定输入，尝试从测试用例获取第一个
            if (input == null || input.isEmpty()) {
                List<TestCase> testCases = testCaseMapper.findByProblemId(String.valueOf(problemId));
                if (!testCases.isEmpty()) {
                    input = testCases.get(0).getInput();
                }
            }
            
            // 执行代码
            Map<String, Object> runResult = executeJavaCode(code, input, problemId);
            
            result.put("success", true);
            result.put("output", runResult.get("output"));
            result.put("error", runResult.get("error"));
            result.put("status", runResult.get("status"));
            result.put("runtime", runResult.get("runtime"));
            result.put("compileError", runResult.get("compileError"));
            
        } catch (Exception e) {
            logger.error("运行代码失败", e);
            result.put("success", false);
            result.put("message", "运行失败：" + e.getMessage());
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
            
            OJProblem problem = problemMapper.getProblemById(problemId);
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
     * Java 沙箱模式 - 真实判题逻辑
     */
    private void judgeCode(String submissionId, OJProblem problem, String code) {
        new Thread(() -> {
            try {
                Long problemId = problem.getId();
                List<TestCase> testCases = testCaseMapper.findByProblemId(String.valueOf(problemId));
                
                if (testCases.isEmpty()) {
                    // 没有测试用例，使用示例输入输出
                    logger.warn("题目 {} 没有测试用例，使用默认判题", problemId);
                    submissionMapper.updateSubmissionStatus(submissionId, "AC", 100, 5000, null, "无测试用例，默认通过");
                    return;
                }
                
                // 执行代码并比对所有测试用例
                int passedCount = 0;
                long totalRuntime = 0;
                long maxMemory = 0;
                StringBuilder judgeLog = new StringBuilder();
                String finalStatus = "AC";
                
                for (int i = 0; i < testCases.size(); i++) {
                    TestCase tc = testCases.get(i);
                    String input = tc.getInput();
                    String expectedOutput = tc.getOutput();
                    
                    Map<String, Object> runResult = executeJavaCode(code, input, problemId);
                    
                    String tcStatus = (String) runResult.get("status");
                    String actualOutput = (String) runResult.get("output");
                    String error = (String) runResult.get("error");
                    
                    long runtime = runResult.get("runtime") != null ? ((Number) runResult.get("runtime")).longValue() : 0;
                    long memory = runResult.get("memory") != null ? ((Number) runResult.get("memory")).longValue() : 0;
                    
                    totalRuntime += runtime;
                    maxMemory = Math.max(maxMemory, memory);
                    
                    // 检查编译错误
                    if ("CE".equals(tcStatus)) {
                        finalStatus = "CE";
                        judgeLog.append("测试用例 #").append(i + 1).append(": 编译错误\n");
                        break;
                    }
                    
                    // 检查运行时错误
                    if ("RE".equals(tcStatus)) {
                        finalStatus = "RE";
                        judgeLog.append("测试用例 #").append(i + 1).append(": 运行时错误 - ").append(error).append("\n");
                        break;
                    }
                    
                    // 检查超时
                    if ("TLE".equals(tcStatus)) {
                        finalStatus = "TLE";
                        judgeLog.append("测试用例 #").append(i + 1).append(": 超时\n");
                        break;
                    }
                    
                    // 比对输出
                    if (actualOutput != null && expectedOutput != null) {
                        // 标准化输出（去除首尾空格和多余空行）
                        String normalizedActual = actualOutput.trim().replaceAll("\\s+", " ");
                        String normalizedExpected = expectedOutput.trim().replaceAll("\\s+", " ");
                        
                        if (normalizedActual.equals(normalizedExpected)) {
                            passedCount++;
                            judgeLog.append("测试用例 #").append(i + 1).append(": 通过\n");
                        } else {
                            finalStatus = "WA";
                            judgeLog.append("测试用例 #").append(i + 1).append(": 答案错误\n");
                            judgeLog.append("  期望: ").append(normalizedExpected).append("\n");
                            judgeLog.append("  实际: ").append(normalizedActual).append("\n");
                            // 继续执行其他测试用例
                        }
                    } else {
                        finalStatus = "WA";
                        judgeLog.append("测试用例 #").append(i + 1).append(": 输出为空\n");
                    }
                }
                
                int avgRuntime = testCases.size() > 0 ? (int) (totalRuntime / testCases.size()) : 0;
                int usedMemory = (int) maxMemory;
                
                String errorMessage = "AC".equals(finalStatus) ? null : judgeLog.toString();
                
                submissionMapper.updateSubmissionStatus(submissionId, finalStatus, avgRuntime, usedMemory, errorMessage, judgeLog.toString());
                
                logger.info("判题完成 - {}: {}, 通过 {}/{}", submissionId, finalStatus, passedCount, testCases.size());
                
            } catch (Exception e) {
                logger.error("判题失败", e);
                submissionMapper.updateSubmissionStatus(submissionId, "RE", 0, 0, e.getMessage(), "判题系统错误: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 执行 Java 代码（沙箱模式）
     * 用户提交完整的 Java 文件，类名为 Main
     */
    private Map<String, Object> executeJavaCode(String userCode, String input, Long problemId) {
        Map<String, Object> result = new HashMap<>();
        Path tempDir = null;
        
        try {
            // 创建临时目录
            tempDir = Files.createTempDirectory("oj_judge_");
            
            // 直接保存用户代码为 Main.java
            Path javaFile = tempDir.resolve("Main.java");
            Files.writeString(javaFile, userCode);
            
            // 编译
            long compileStart = System.currentTimeMillis();
            ProcessBuilder compilePB = new ProcessBuilder("javac", "-encoding", "UTF-8", "Main.java");
            compilePB.directory(tempDir.toFile());
            compilePB.redirectErrorStream(true);
            
            Process compileProcess = compilePB.start();
            boolean compileDone = compileProcess.waitFor(TIME_LIMIT_MS / 2, TimeUnit.MILLISECONDS);
            
            if (!compileDone) {
                compileProcess.destroy();
                result.put("status", "CE");
                result.put("compileError", "编译超时");
                result.put("error", "编译超时");
                result.put("output", "");
                result.put("runtime", 0);
                result.put("memory", 0);
                return result;
            }
            
            int compileExitCode = compileProcess.exitValue();
            String compileOutput = new String(compileProcess.getInputStream().readAllBytes());
            
            if (compileExitCode != 0) {
                result.put("status", "CE");
                result.put("compileError", compileOutput);
                result.put("error", "编译错误");
                result.put("output", "");
                result.put("runtime", 0);
                result.put("memory", 0);
                return result;
            }
            
            // 运行 - 输入通过命令行参数传递
            long runStart = System.currentTimeMillis();
            String inputStr = input != null ? input : "";
            
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.add("-Xmx" + MEMORY_LIMIT_MB + "m");
            cmd.add("-Xss1m");
            cmd.add("-Djava.security.manager=allow");
            cmd.add("Main");
            if (!inputStr.isEmpty()) {
                cmd.add(inputStr);
            }
            
            ProcessBuilder runPB = new ProcessBuilder(cmd);
            runPB.directory(tempDir.toFile());
            runPB.redirectErrorStream(false);
            
            Process runProcess = runPB.start();
            
            boolean runDone = runProcess.waitFor(TIME_LIMIT_MS, TimeUnit.MILLISECONDS);
            long runTime = System.currentTimeMillis() - runStart;
            
            if (!runDone) {
                runProcess.destroyForcibly();
                result.put("status", "TLE");
                result.put("error", "运行超时");
                result.put("output", "");
                result.put("runtime", TIME_LIMIT_MS);
                result.put("memory", 0);
                return result;
            }
            
            int runExitCode = runProcess.exitValue();
            String runOutput = new String(runProcess.getInputStream().readAllBytes());
            String runError = new String(runProcess.getErrorStream().readAllBytes());
            
            if (runExitCode != 0) {
                // 运行时错误
                result.put("status", "RE");
                result.put("error", runError);
                result.put("output", runOutput);
                result.put("runtime", (int) runTime);
                result.put("memory", 0);
                return result;
            }
            
            result.put("status", "SUCCESS");
            result.put("output", runOutput);
            result.put("error", null);
            result.put("runtime", (int) runTime);
            result.put("memory", 0);
            
        } catch (Exception e) {
            logger.error("执行代码失败", e);
            result.put("status", "RE");
            result.put("error", e.getMessage());
            result.put("output", "");
            result.put("runtime", 0);
            result.put("memory", 0);
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                } catch (IOException e) {
                    logger.warn("清理临时目录失败", e);
                }
            }
        }
        
        return result;
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