package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.Algorithm;
import com.algoviz.entity.AIPrompt;
import com.algoviz.entity.DataStructure;
import com.algoviz.entity.OJProblem;
import com.algoviz.entity.TestCase;
import com.algoviz.mapper.AlgorithmMapper;
import com.algoviz.mapper.AIPromptMapper;
import com.algoviz.mapper.DataStructureMapper;
import com.algoviz.mapper.OJProblemMapper;
import com.algoviz.mapper.TestCaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "内容管理", description = "数据结构/算法/题目/测试用例/AI提示词")
public class AdminContentController {

    @Autowired
    private DataStructureMapper dataStructureMapper;

    @Autowired
    private AlgorithmMapper algorithmMapper;

    @Autowired
    private OJProblemMapper ojProblemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private AIPromptMapper aiPromptMapper;

    @GetMapping("/content/data-structure")
    @Operation(summary = "查询数据结构列表", description = "分页查询数据结构列表")
    public ApiResponse<Map<String, Object>> getDataStructureList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<DataStructure> list = dataStructureMapper.findByPage(offset, pageSize);
        int total = dataStructureMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/data-structure")
    @Operation(summary = "新增数据结构", description = "新增一条数据结构记录")
    public ApiResponse<DataStructure> createDataStructure(@RequestBody DataStructure dataStructure) {
        dataStructure.setId(UUID.randomUUID().toString());
        dataStructure.setStatus("enabled");
        dataStructureMapper.insert(dataStructure);
        return ApiResponse.success(dataStructureMapper.findById(dataStructure.getId()));
    }

    @PutMapping("/content/data-structure/{id}")
    @Operation(summary = "更新数据结构", description = "根据ID更新数据结构记录")
    public ApiResponse<DataStructure> updateDataStructure(@PathVariable String id, @RequestBody DataStructure dataStructure) {
        dataStructure.setId(id);
        dataStructureMapper.update(dataStructure);
        return ApiResponse.success(dataStructureMapper.findById(id));
    }

    @DeleteMapping("/content/data-structure/{id}")
    @Operation(summary = "删除数据结构", description = "根据ID删除数据结构记录")
    public ApiResponse<Void> deleteDataStructure(@PathVariable String id) {
        boolean success = dataStructureMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/content/algorithm")
    @Operation(summary = "查询算法列表", description = "分页查询算法列表")
    public ApiResponse<Map<String, Object>> getAlgorithmList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Algorithm> list = algorithmMapper.findByPage(offset, pageSize);
        int total = algorithmMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/algorithm")
    @Operation(summary = "新增算法", description = "新增一条算法记录")
    public ApiResponse<Algorithm> createAlgorithm(@RequestBody Algorithm algorithm) {
        algorithm.setId(UUID.randomUUID().toString());
        algorithm.setStatus("enabled");
        algorithmMapper.insert(algorithm);
        return ApiResponse.success(algorithmMapper.findById(algorithm.getId()));
    }

    @PutMapping("/content/algorithm/{id}")
    @Operation(summary = "更新算法", description = "根据ID更新算法记录")
    public ApiResponse<Algorithm> updateAlgorithm(@PathVariable String id, @RequestBody Algorithm algorithm) {
        algorithm.setId(id);
        algorithmMapper.update(algorithm);
        return ApiResponse.success(algorithmMapper.findById(id));
    }

    @DeleteMapping("/content/algorithm/{id}")
    @Operation(summary = "删除算法", description = "根据ID删除算法记录")
    public ApiResponse<Void> deleteAlgorithm(@PathVariable String id) {
        boolean success = algorithmMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/content/oj-problem")
    @Operation(summary = "查询OJ题目列表", description = "分页查询OJ题目列表")
    public ApiResponse<Map<String, Object>> getOJProblemList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OJProblem> list = ojProblemMapper.findByPage(offset, pageSize);
        int total = ojProblemMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/oj-problem")
    @Operation(summary = "新增OJ题目", description = "新增一条OJ题目记录并初始化为上线状态")
    public ApiResponse<OJProblem> createOJProblem(@RequestBody OJProblem problem) {
        problem.setStatus("online");
        problem.setSubmissionCount(0);
        problem.setAcRate(0.0);
        ojProblemMapper.insert(problem);
        return ApiResponse.success(ojProblemMapper.findById(problem.getId()));
    }

    @PutMapping("/content/oj-problem/{id}")
    @Operation(summary = "更新OJ题目", description = "根据ID更新OJ题目记录")
    public ApiResponse<OJProblem> updateOJProblem(@PathVariable Long id, @RequestBody OJProblem problem) {
        problem.setId(id);
        ojProblemMapper.update(problem);
        return ApiResponse.success(ojProblemMapper.findById(id));
    }

    @DeleteMapping("/content/oj-problem/{id}")
    @Operation(summary = "删除OJ题目", description = "根据ID删除OJ题目及其测试用例")
    public ApiResponse<Void> deleteOJProblem(@PathVariable Long id) {
        testCaseMapper.deleteByProblemId(String.valueOf(id));
        boolean success = ojProblemMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/content/test-case/{problemId}")
    @Operation(summary = "查询测试用例列表", description = "根据题目ID查询该题目的测试用例列表")
    public ApiResponse<List<TestCase>> getTestCaseList(@PathVariable String problemId) {
        return ApiResponse.success(testCaseMapper.findByProblemId(problemId));
    }

    @PostMapping("/content/test-case")
    @Operation(summary = "新增测试用例", description = "新增一条测试用例记录")
    public ApiResponse<TestCase> createTestCase(@RequestBody TestCase testCase) {
        testCase.setId(UUID.randomUUID().toString());
        testCaseMapper.insert(testCase);
        return ApiResponse.success(testCaseMapper.findById(testCase.getId()));
    }

    @PutMapping("/content/test-case/{id}")
    @Operation(summary = "更新测试用例", description = "根据ID更新测试用例记录")
    public ApiResponse<TestCase> updateTestCase(@PathVariable String id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseMapper.update(testCase);
        return ApiResponse.success(testCaseMapper.findById(id));
    }

    @DeleteMapping("/content/test-case/{id}")
    @Operation(summary = "删除测试用例", description = "根据ID删除测试用例记录")
    public ApiResponse<Void> deleteTestCase(@PathVariable String id) {
        boolean success = testCaseMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/content/ai-prompt")
    @Operation(summary = "查询AI提示词列表", description = "分页查询AI提示词列表")
    public ApiResponse<Map<String, Object>> getAIPromptList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AIPrompt> list = aiPromptMapper.findByPage(offset, pageSize);
        int total = aiPromptMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/ai-prompt")
    @Operation(summary = "新增AI提示词", description = "新增一条AI提示词记录")
    public ApiResponse<AIPrompt> createAIPrompt(@RequestBody AIPrompt prompt) {
        prompt.setId(UUID.randomUUID().toString());
        prompt.setUsageCount(0);
        prompt.setStatus("enabled");
        aiPromptMapper.insert(prompt);
        return ApiResponse.success(aiPromptMapper.findById(prompt.getId()));
    }

    @PutMapping("/content/ai-prompt/{id}")
    @Operation(summary = "更新AI提示词", description = "根据ID更新AI提示词记录")
    public ApiResponse<AIPrompt> updateAIPrompt(@PathVariable String id, @RequestBody AIPrompt prompt) {
        prompt.setId(id);
        aiPromptMapper.update(prompt);
        return ApiResponse.success(aiPromptMapper.findById(id));
    }

    @DeleteMapping("/content/ai-prompt/{id}")
    @Operation(summary = "删除AI提示词", description = "根据ID删除AI提示词记录")
    public ApiResponse<Void> deleteAIPrompt(@PathVariable String id) {
        boolean success = aiPromptMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }
}
