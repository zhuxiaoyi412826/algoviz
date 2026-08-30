package com.algoviz.service.impl;

import com.algoviz.service.CodeRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import java.util.concurrent.*;

@Service
public class CodeRunServiceImpl implements CodeRunService {

    private static final Logger logger = LoggerFactory.getLogger(CodeRunServiceImpl.class);
    
    // 线程池用于运行代码，限制并发数
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    // 代码运行超时时间（秒）
    private static final int RUN_TIMEOUT = 30;
    
    // 最大输出长度
    private static final int MAX_OUTPUT_LENGTH = 10000;

    // ==================== OJ 代码沙箱安全加固 ====================
    // Java 类名校验：仅允许合法 Java 标识符（防止 extractClassName 把 "Main extends Object" 吞进进程参数）
    private static final Pattern JAVA_CLASS_NAME_PATTERN = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*$");

    // 危险 API 静态过滤（第一层辅助防护；真正的拦截由子 JVM SecurityManager 完成）
    private static final String[] FORBIDDEN_API_KEYWORDS = {
            "ProcessBuilder", "Runtime", "getRuntime", "exec(", "System.load",
            "Class.forName", "URLClassLoader", "Socket", "ServerSocket", "URLConnection",
            "HttpURLConnection", "new File(", "FileInputStream", "FileOutputStream",
            "FileWriter", "FileReader", "Files."
    };

    // Python 危险 API（辅助防护：Python 无内置沙箱，此层为主要拦截）
    private static final String[] PYTHON_FORBIDDEN_KEYWORDS = {
            "import os", "from os", "os.system", "os.popen", "os.remove", "os.listdir", "os.unlink",
            "subprocess", "eval(", "exec(", "__import__", "compile(", "pickle", "ctypes",
            "import socket", "from socket", "import requests", "import urllib", "http.client",
            "shutil", "open(", "file("
    };

    // C++ 危险 API（辅助防护：C++ 无内置沙箱，此层为主要拦截）
    private static final String[] CPP_FORBIDDEN_KEYWORDS = {
            "system(", "popen(", "fork(", "execvp(", "execve(", "execl", "fopen", "fstream",
            "ifstream", "ofstream", "remove(", "rename(", "getenv", "chmod",
            "#include <unistd.h>", "#include <sys", "#include <net", "#include <arpa", "socket("
    };

    // JavaScript 危险 API（辅助防护：Node 无内置沙箱，此层为主要拦截）
    private static final String[] JS_FORBIDDEN_KEYWORDS = {
            "child_process", "require('fs')", "require(\"fs\")", "process.execPath",
            "process.mainModule", "process.binding", "process.env", "execSync", "spawn(",
            "fork(", "eval(", "Function(", "global.require", "fs.", "net.", "dns.", "vm.",
            "http.request", "https.request"
    };

    /**
     * 静态危险 API 扫描：命中返回命中的关键字，未命中返回 null
     */
    private String scanForbiddenCode(String code, String[] keywords) {
        for (String keyword : keywords) {
            if (code.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> runCode(String code, String language, String input) {
        logger.info("运行代码 - 语言: {}, 代码长度: {}, 输入长度: {}", language, code.length(), input != null ? input.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (language.toLowerCase()) {
                case "java":
                    result = runJavaCode(code, input);
                    break;
                case "python":
                    result = runPythonCode(code, input);
                    break;
                case "cpp":
                    result = runCppCode(code, input);
                    break;
                case "javascript":
                    result = runJavaScriptCode(code, input);
                    break;
                default:
                    result.put("status", "error");
                    result.put("message", "不支持的编程语言: " + language);
            }
        } catch (Exception e) {
            logger.error("代码运行异常", e);
            result.put("status", "error");
            result.put("message", "运行异常: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> submitCode(String code, String language, String problemNo) {
        logger.info("提交代码判题 - 语言: {}, 题目: {}", language, problemNo);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (language.toLowerCase()) {
                case "java":
                    result = judgeJavaCode(code, problemNo);
                    break;
                case "python":
                    result = judgePythonCode(code, problemNo);
                    break;
                case "cpp":
                    result = judgeCppCode(code, problemNo);
                    break;
                case "javascript":
                    result = judgeJavaScriptCode(code, problemNo);
                    break;
                default:
                    result.put("status", "error");
                    result.put("message", "不支持的编程语言: " + language);
            }
        } catch (Exception e) {
            logger.error("判题异常", e);
            result.put("status", "error");
            result.put("message", "判题异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 判题Java代码（添加main方法包装）
     */
    private Map<String, Object> judgeJavaCode(String solutionCode, String problemNo) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 为两数之和题目生成完整的Java程序
        String fullCode = generateJavaMain(solutionCode, problemNo);
        logger.info("生成完整Java代码，长度: {}", fullCode.length());
        
        // 获取测试用例
        String testInput = getTestInput(problemNo);
        String expectedOutput = getExpectedOutput(problemNo);
        
        // 运行代码
        Map<String, Object> runResult = runJavaCode(fullCode, testInput);
        
        if (!"success".equals(runResult.get("status"))) {
            return runResult;
        }
        
        // 对比结果
        String actualOutput = (String) runResult.get("output");
        if (actualOutput != null && actualOutput.trim().equals(expectedOutput.trim())) {
            result.put("status", "AC");
            result.put("message", "答案正确");
        } else {
            result.put("status", "WA");
            result.put("message", "答案错误");
            result.put("expected", expectedOutput);
        }
        
        result.put("output", actualOutput);
        result.put("time", runResult.get("time"));
        result.put("memory", runResult.get("memory"));
        
        return result;
    }
    
    /**
     * 生成Java主程序
     */
    private String generateJavaMain(String solutionCode, String problemNo) {
        // 如果代码已经包含 public static void main 方法，则直接返回
        if (solutionCode.contains("public static void main")) {
            return solutionCode;
        }
        
        // 根据题目生成不同的main方法
        switch (problemNo) {
            case "1001": // 两数之和
                return solutionCode + "\n\n" + 
                    "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        Solution sol = new Solution();\n" +
                    "        java.util.Scanner sc = new java.util.Scanner(System.in);\n" +
                    "        String[] parts = sc.nextLine().split(\",\");\n" +
                    "        int[] nums = new int[parts.length];\n" +
                    "        for (int i = 0; i < parts.length; i++) {\n" +
                    "            nums[i] = Integer.parseInt(parts[i].trim());\n" +
                    "        }\n" +
                    "        int target = Integer.parseInt(sc.nextLine().trim());\n" +
                    "        int[] result = sol.twoSum(nums, target);\n" +
                    "        System.out.println(\"[\" + result[0] + \",\" + result[1] + \"]\");\n" +
                    "    }\n" +
                    "}";
            default:
                return solutionCode;
        }
    }
    
    /**
     * 获取测试用例输入
     */
    private String getTestInput(String problemNo) {
        switch (problemNo) {
            case "1001": // 两数之和测试用例
                return "2,7,11,15\n9";
            default:
                return "";
        }
    }
    
    /**
     * 获取期望输出
     */
    private String getExpectedOutput(String problemNo) {
        switch (problemNo) {
            case "1001": // 两数之和期望输出
                return "[0,1]";
            default:
                return "";
        }
    }
    
    /**
     * 判题Python代码
     */
    private Map<String, Object> judgePythonCode(String code, String problemNo) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String testInput = getTestInput(problemNo);
        String expectedOutput = getExpectedOutput(problemNo);
        
        Map<String, Object> runResult = runPythonCode(code, testInput);
        
        if (!"success".equals(runResult.get("status"))) {
            return runResult;
        }
        
        String actualOutput = (String) runResult.get("output");
        if (actualOutput != null && actualOutput.trim().equals(expectedOutput.trim())) {
            result.put("status", "AC");
            result.put("message", "答案正确");
        } else {
            result.put("status", "WA");
            result.put("message", "答案错误");
            result.put("expected", expectedOutput);
        }
        
        result.put("output", actualOutput);
        result.put("time", runResult.get("time"));
        result.put("memory", runResult.get("memory"));
        
        return result;
    }
    
    /**
     * 判题C++代码
     */
    private Map<String, Object> judgeCppCode(String code, String problemNo) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String testInput = getTestInput(problemNo);
        String expectedOutput = getExpectedOutput(problemNo);
        
        Map<String, Object> runResult = runCppCode(code, testInput);
        
        if (!"success".equals(runResult.get("status"))) {
            return runResult;
        }
        
        String actualOutput = (String) runResult.get("output");
        if (actualOutput != null && actualOutput.trim().equals(expectedOutput.trim())) {
            result.put("status", "AC");
            result.put("message", "答案正确");
        } else {
            result.put("status", "WA");
            result.put("message", "答案错误");
            result.put("expected", expectedOutput);
        }
        
        result.put("output", actualOutput);
        result.put("time", runResult.get("time"));
        result.put("memory", runResult.get("memory"));
        
        return result;
    }
    
    /**
     * 判题JavaScript代码
     */
    private Map<String, Object> judgeJavaScriptCode(String code, String problemNo) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String testInput = getTestInput(problemNo);
        String expectedOutput = getExpectedOutput(problemNo);
        
        Map<String, Object> runResult = runJavaScriptCode(code, testInput);
        
        if (!"success".equals(runResult.get("status"))) {
            return runResult;
        }
        
        String actualOutput = (String) runResult.get("output");
        if (actualOutput != null && actualOutput.trim().equals(expectedOutput.trim())) {
            result.put("status", "AC");
            result.put("message", "答案正确");
        } else {
            result.put("status", "WA");
            result.put("message", "答案错误");
            result.put("expected", expectedOutput);
        }
        
        result.put("output", actualOutput);
        result.put("time", runResult.get("time"));
        result.put("memory", runResult.get("memory"));
        
        return result;
    }

    /**
     * 运行Java代码
     */
    private Map<String, Object> runJavaCode(String code, String input) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 检查Java环境
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            result.put("status", "error");
            result.put("message", "无法找到Java运行环境");
            return result;
        }
        
        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "algoviz_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) {
            result.put("status", "error");
            result.put("message", "无法创建临时目录");
            return result;
        }
        
        try {
            // 0) 静态危险 API 扫描（辅助防护：编译前拦截文件IO/进程/网络相关 API）
            String forbidden = scanForbiddenCode(code, FORBIDDEN_API_KEYWORDS);
            if (forbidden != null) {
                result.put("status", "ce");
                result.put("message", "编译错误: 检测到禁止使用的 API \"" + forbidden
                        + "\"，出于安全考虑不允许文件IO/进程/网络相关操作");
                return result;
            }

            // 写入安全策略文件（供子 JVM 的 SecurityManager 使用）
            File policyFile = new File(tempDir, "security.policy");
            try (InputStream in = new ClassPathResource("security.policy").getInputStream();
                 FileOutputStream fos = new FileOutputStream(policyFile)) {
                in.transferTo(fos);
            } catch (Exception e) {
                logger.warn("安全策略文件写入失败，子进程将以受限策略运行: {}", e.getMessage());
            }

            // 提取类名
            String className = extractClassName(code);
            if (className == null) {
                result.put("status", "ce");
                result.put("message", "编译错误: 无法找到类名，请确保类名为public且与文件名一致");
                return result;
            }
            // 类名合法性校验：仅允许 Java 标识符，杜绝 "Main extends Object" 等被拼接进进程参数
            if (!JAVA_CLASS_NAME_PATTERN.matcher(className).matches()) {
                result.put("status", "ce");
                result.put("message", "编译错误: 非法类名 \"" + className + "\"，类名只能由字母、数字、下划线、$ 组成");
                return result;
            }
            
            // 创建Java源文件
            File sourceFile = new File(tempDir, className + ".java");
            try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
                fos.write(code.getBytes(StandardCharsets.UTF_8));
            }
            
            // 编译代码
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                result.put("status", "error");
                result.put("message", "无法找到Java编译器，请确保使用JDK运行");
                return result;
            }
            
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            // 添加 -encoding UTF-8 参数确保编译器使用UTF-8编码
            int compileResult = compiler.run(null, null, errStream, "-encoding", "UTF-8", sourceFile.getPath());
            
            if (compileResult != 0) {
                // 尝试用UTF-8解码，如果乱码则尝试GBK（Windows默认编码）
                String errorMsg = errStream.toString(StandardCharsets.UTF_8.name());
                // 检查是否是乱码（包含不可读字符）
                if (errorMsg.contains("\uFFFD") || errorMsg.matches(".*[\\u0080-\\uFFFF].*")) {
                    errorMsg = errStream.toString("GBK");
                }
                result.put("status", "ce");
                result.put("message", "编译错误:\n" + truncate(errorMsg, 2000));
                return result;
            }
            
            // 运行代码（带超时）
            Future<Map<String, Object>> future = executorService.submit(() -> {
                Map<String, Object> runResult = new HashMap<>();
                long startTime = System.currentTimeMillis();
                
                try {
                    // 创建进程并捕获输出
                    // 沙箱：启用 SecurityManager + 安全策略，限制子 JVM 权限
                    //   - 禁 createProcess（拦截 ProcessBuilder / Runtime.exec 执行系统命令）
                    //   - 禁临时目录外文件读取（拦截读 C:/Windows 等）
                    //   - 禁 Socket（拦截网络）
                    ProcessBuilder pb = new ProcessBuilder(
                        javaHome + "/bin/java",
                        "-Dfile.encoding=UTF-8",
                        "-Djava.security.manager",
                        "-Djava.security.policy=" + policyFile.toURI(),
                        "-cp", tempDir.getPath(),
                        className
                    );
                    pb.directory(tempDir);
                    pb.redirectErrorStream(true);
                    
                    // 设置进程环境变量，确保输出编码为UTF-8
                    Map<String, String> env = pb.environment();
                    env.put("LC_ALL", "en_US.UTF-8");
                    env.put("LANG", "en_US.UTF-8");
                    
                    Process process = pb.start();
                    
                    // 写入输入
                    if (input != null && !input.isEmpty()) {
                        try (OutputStream os = process.getOutputStream()) {
                            os.write(input.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }
                    
                    // 读取输出（同时尝试UTF-8和GBK）
                    StringBuilder output = new StringBuilder();
                    byte[] outputBytes = process.getInputStream().readAllBytes();
                    String outputStr = new String(outputBytes, StandardCharsets.UTF_8);
                    // 检查是否是乱码
                    if (outputStr.contains("\uFFFD") || outputStr.matches(".*[\\u0080-\\uFFFF].*")) {
                        outputStr = new String(outputBytes, "GBK");
                    }
                    
                    String[] lines = outputStr.split("\\r?\\n");
                    for (String line : lines) {
                        output.append(line).append("\n");
                        if (output.length() > MAX_OUTPUT_LENGTH) {
                            output.append("...（输出被截断）");
                            break;
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    long endTime = System.currentTimeMillis();
                    
                    runResult.put("status", exitCode == 0 ? "success" : "re");
                    runResult.put("output", output.toString().trim());
                    runResult.put("time", endTime - startTime);
                    runResult.put("memory", 0);
                    
                } catch (Exception e) {
                    runResult.put("status", "re");
                    runResult.put("message", "运行时错误: " + e.getMessage());
                }
                
                return runResult;
            });
            
            try {
                result = future.get(RUN_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                result.put("status", "tle");
                result.put("message", "运行超时");
            } catch (ExecutionException e) {
                result.put("status", "re");
                result.put("message", "运行异常: " + e.getCause().getMessage());
            }
            
        } finally {
            // 清理临时文件
            deleteDir(tempDir);
        }
        
        return result;
    }

    /**
     * 运行Python代码
     */
    private Map<String, Object> runPythonCode(String code, String input) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 0) 静态危险 API 扫描（Python 无内置沙箱，此层为主要拦截）
        String forbidden = scanForbiddenCode(code, PYTHON_FORBIDDEN_KEYWORDS);
        if (forbidden != null) {
            result.put("status", "ce");
            result.put("message", "编译错误: 检测到禁止使用的 API \"" + forbidden
                    + "\"，出于安全考虑不允许文件IO/进程/网络相关操作");
            return result;
        }

        // 创建临时文件
        File tempFile = File.createTempFile("algoviz_", ".py");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(code.getBytes(StandardCharsets.UTF_8));
        }
        
        try {
            Future<Map<String, Object>> future = executorService.submit(() -> {
                Map<String, Object> runResult = new HashMap<>();
                long startTime = System.currentTimeMillis();
                
                try {
                    // -I：隔离模式（忽略 PYTHON* 环境变量与用户 site-packages）
                    // -u：无缓冲输出
                    ProcessBuilder pb = new ProcessBuilder("python", "-I", "-u", tempFile.getPath());
                    pb.redirectErrorStream(true);
                    
                    Process process = pb.start();
                    
                    // 写入输入
                    if (input != null && !input.isEmpty()) {
                        try (OutputStream os = process.getOutputStream()) {
                            os.write(input.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }
                    
                    // 读取输出
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                            if (output.length() > MAX_OUTPUT_LENGTH) {
                                output.append("...（输出被截断）");
                                break;
                            }
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    long endTime = System.currentTimeMillis();
                    
                    runResult.put("status", exitCode == 0 ? "success" : "re");
                    runResult.put("output", output.toString().trim());
                    runResult.put("time", endTime - startTime);
                    runResult.put("memory", 0);
                    
                } catch (Exception e) {
                    runResult.put("status", "error");
                    runResult.put("message", "Python环境错误: " + e.getMessage());
                }
                
                return runResult;
            });
            
            try {
                result = future.get(RUN_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                result.put("status", "tle");
                result.put("message", "运行超时");
            }
            
        } finally {
            tempFile.delete();
        }
        
        return result;
    }

    /**
     * 运行C++代码
     */
    private Map<String, Object> runCppCode(String code, String input) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 0) 静态危险 API 扫描（C++ 无内置沙箱，此层为主要拦截）
        String forbidden = scanForbiddenCode(code, CPP_FORBIDDEN_KEYWORDS);
        if (forbidden != null) {
            result.put("status", "ce");
            result.put("message", "编译错误: 检测到禁止使用的 API \"" + forbidden
                    + "\"，出于安全考虑不允许文件IO/进程/网络相关操作");
            return result;
        }

        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "algoviz_cpp_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) {
            result.put("status", "error");
            result.put("message", "无法创建临时目录");
            return result;
        }
        
        try {
            // 创建源文件
            File sourceFile = new File(tempDir, "main.cpp");
            try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
                fos.write(code.getBytes(StandardCharsets.UTF_8));
            }
            
            // 编译（-w 静默警告，-O2 优化）
            ProcessBuilder compilePb = new ProcessBuilder("g++", "-w", "-O2", "-o", "main", "main.cpp");
            compilePb.directory(tempDir);
            ByteArrayOutputStream compileErr = new ByteArrayOutputStream();
            
            Process compileProcess = compilePb.start();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(compileProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileErr.write(line.getBytes());
                    compileErr.write("\n".getBytes());
                }
            }
            
            int compileResult = compileProcess.waitFor();
            if (compileResult != 0) {
                result.put("status", "ce");
                result.put("message", "编译错误:\n" + compileErr.toString(StandardCharsets.UTF_8.name()));
                return result;
            }
            
            // 运行
            Future<Map<String, Object>> future = executorService.submit(() -> {
                Map<String, Object> runResult = new HashMap<>();
                long startTime = System.currentTimeMillis();
                
                try {
                    ProcessBuilder runPb = new ProcessBuilder("./main");
                    runPb.directory(tempDir);
                    runPb.redirectErrorStream(true);
                    
                    Process process = runPb.start();
                    
                    if (input != null && !input.isEmpty()) {
                        try (OutputStream os = process.getOutputStream()) {
                            os.write(input.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }
                    
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                            if (output.length() > MAX_OUTPUT_LENGTH) {
                                output.append("...（输出被截断）");
                                break;
                            }
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    long endTime = System.currentTimeMillis();
                    
                    runResult.put("status", exitCode == 0 ? "success" : "re");
                    runResult.put("output", output.toString().trim());
                    runResult.put("time", endTime - startTime);
                    runResult.put("memory", 0);
                    
                } catch (Exception e) {
                    runResult.put("status", "error");
                    runResult.put("message", "运行错误: " + e.getMessage());
                }
                
                return runResult;
            });
            
            try {
                result = future.get(RUN_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                result.put("status", "tle");
                result.put("message", "运行超时");
            }
            
        } finally {
            deleteDir(tempDir);
        }
        
        return result;
    }

    /**
     * 运行JavaScript代码
     */
    private Map<String, Object> runJavaScriptCode(String code, String input) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 0) 静态危险 API 扫描（Node 无内置沙箱，此层为主要拦截）
        String forbidden = scanForbiddenCode(code, JS_FORBIDDEN_KEYWORDS);
        if (forbidden != null) {
            result.put("status", "ce");
            result.put("message", "编译错误: 检测到禁止使用的 API \"" + forbidden
                    + "\"，出于安全考虑不允许文件IO/进程/网络相关操作");
            return result;
        }

        // 创建临时文件
        File tempFile = File.createTempFile("algoviz_", ".js");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(code.getBytes(StandardCharsets.UTF_8));
        }
        
        try {
            Future<Map<String, Object>> future = executorService.submit(() -> {
                Map<String, Object> runResult = new HashMap<>();
                long startTime = System.currentTimeMillis();
                
                try {
                    // --disallow-code-generation-from-strings：禁止 eval/Function 动态生成代码
                    ProcessBuilder pb = new ProcessBuilder("node", "--disallow-code-generation-from-strings", tempFile.getPath());
                    pb.redirectErrorStream(true);
                    
                    Process process = pb.start();
                    
                    if (input != null && !input.isEmpty()) {
                        try (OutputStream os = process.getOutputStream()) {
                            os.write(input.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }
                    
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                            if (output.length() > MAX_OUTPUT_LENGTH) {
                                output.append("...（输出被截断）");
                                break;
                            }
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    long endTime = System.currentTimeMillis();
                    
                    runResult.put("status", exitCode == 0 ? "success" : "re");
                    runResult.put("output", output.toString().trim());
                    runResult.put("time", endTime - startTime);
                    runResult.put("memory", 0);
                    
                } catch (Exception e) {
                    runResult.put("status", "error");
                    runResult.put("message", "Node.js环境错误: " + e.getMessage());
                }
                
                return runResult;
            });
            
            try {
                result = future.get(RUN_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                result.put("status", "tle");
                result.put("message", "运行超时");
            }
            
        } finally {
            tempFile.delete();
        }
        
        return result;
    }

    /**
     * 从Java代码中提取类名
     */
    private String extractClassName(String code) {
        // 查找 public class 后面的类名
        int classIndex = code.indexOf("public class");
        if (classIndex == -1) {
            classIndex = code.indexOf("class ");
            if (classIndex == -1) return null;
        }
        
        int startIndex = classIndex + (code.startsWith("public class", classIndex) ? 13 : 6);
        int endIndex = code.indexOf("{", startIndex);
        
        if (endIndex == -1) {
            endIndex = code.indexOf("\n", startIndex);
        }
        
        if (endIndex == -1) {
            endIndex = code.length();
        }
        
        return code.substring(startIndex, endIndex).trim();
    }

    /**
     * 删除目录
     */
    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}