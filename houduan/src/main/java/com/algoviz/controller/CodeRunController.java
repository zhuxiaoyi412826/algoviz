package com.algoviz.controller;

import com.algoviz.service.CodeRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/code")
@Tag(name = "代码运行", description = "代码运行和判题接口")
public class CodeRunController {

    private static final Logger logger = LoggerFactory.getLogger(CodeRunController.class);

    @Autowired
    private CodeRunService codeRunService;

    /**
     * 运行代码（调试）
     */
    @PostMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "运行代码", description = "运行代码并返回结果，用于调试")
    public ResponseEntity<Map<String, Object>> runCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.get("language");
        String input = request.get("input");
        
        logger.info("收到代码运行请求 - 语言: {}, 代码长度: {}", language, code != null ? code.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "代码不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        
        if (language == null || language.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择编程语言");
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
            Map<String, Object> runResult = codeRunService.runCode(code, language, input);
            
            result.put("success", true);
            result.put("status", runResult.get("status"));
            result.put("output", runResult.get("output"));
            result.put("message", runResult.get("message"));
            result.put("time", runResult.get("time"));
            result.put("memory", runResult.get("memory"));
            
        } catch (Exception e) {
            logger.error("代码运行异常", e);
            result.put("success", false);
            result.put("message", "运行异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 提交代码（判题）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交代码", description = "提交代码进行判题")
    public Map<String, Object> submitCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.get("language");
        String problemNo = request.get("problemNo");
        
        logger.info("收到代码提交请求 - 语言: {}, 题目: {}, 代码长度: {}", language, problemNo, code != null ? code.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "代码不能为空");
            return result;
        }
        
        if (language == null || language.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择编程语言");
            return result;
        }
        
        if (problemNo == null || problemNo.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择题目");
            return result;
        }
        
        try {
            Map<String, Object> submitResult = codeRunService.submitCode(code, language, problemNo);
            
            result.put("success", true);
            result.put("status", submitResult.get("status"));
            result.put("output", submitResult.get("output"));
            result.put("message", submitResult.get("message"));
            result.put("time", submitResult.get("time"));
            result.put("memory", submitResult.get("memory"));
            
        } catch (Exception e) {
            logger.error("代码提交异常", e);
            result.put("success", false);
            result.put("message", "提交异常: " + e.getMessage());
        }
        
        return result;
    }
}