package com.algoviz.service.impl;

import com.algoviz.entity.OJProblem;
import com.algoviz.entity.PageResult;
import com.algoviz.mapper.OJProblemMapper;
import com.algoviz.service.OJProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OJProblemServiceImpl implements OJProblemService {

    private static final Logger logger = LoggerFactory.getLogger(OJProblemServiceImpl.class);

    @Autowired
    private OJProblemMapper problemMapper;

    /**
     * 分页查询（数据库层面排序+分页）
     * 这是核心方法：先在数据库排序，再分页，保证数据正确性
     */
    @Override
    public PageResult<OJProblem> getProblemsByPage(
        String keyword, 
        String difficulty, 
        String status,
        String sort, 
        String sortBy, 
        int page, 
        int size
    ) {
        logger.info("分页查询 - keyword={}, difficulty={}, status={}, sort={}, sortBy={}, page={}, size={}", 
            keyword, difficulty, status, sort, sortBy, page, size);
        
        // 参数校验与默认值
        if (page <= 0) page = 1;
        if (size <= 0 || size > 100) size = 20;
        
        // 白名单校验：防止 SQL 注入
        String safeSort = "desc".equalsIgnoreCase(sort) ? "DESC" : "ASC";
        String safeSortBy;
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            safeSortBy = "createdAt";
        } else if ("problemNo".equalsIgnoreCase(sortBy)) {
            safeSortBy = "problemNo";
        } else {
            safeSortBy = "id";
        }
        
        // 计算偏移量
        int offset = (page - 1) * size;
        
        // 查询数据（数据库层面排序+分页）
        List<OJProblem> list = problemMapper.selectByCondition(
            keyword, difficulty, status, safeSort, safeSortBy, offset, size
        );
        
        // 查询总数
        int total = problemMapper.countByCondition(keyword, difficulty, status);
        
        logger.info("分页查询结果 - total={}, page={}, size={}, list.size={}", total, page, size, list.size());
        
        return new PageResult<>(list, total, page, size);
    }

    @Override
    public List<OJProblem> getAllProblems(String sort, String sortBy) {
        logger.info("获取所有题目列表, sort={}, sortBy={}", sort, sortBy);
        List<OJProblem> list = problemMapper.getAllProblems();
        return sortByParam(list, sort, sortBy);
    }

    @Override
    public OJProblem getProblemById(String id) {
        logger.info("获取题目详情：{}", id);
        return problemMapper.getProblemById(Long.parseLong(id));
    }

    @Override
    public OJProblem getProblemByNo(String problemNo) {
        logger.info("获取题目：{}", problemNo);
        return problemMapper.getProblemByNo(problemNo);
    }

    @Override
    public List<OJProblem> searchProblems(String keyword, String sort, String sortBy) {
        logger.info("搜索题目：{}, sort={}, sortBy={}", keyword, sort, sortBy);
        List<OJProblem> list = problemMapper.searchProblems(keyword);
        return sortByParam(list, sort, sortBy);
    }

    @Override
    public List<OJProblem> getProblemsByDifficulty(String difficulty, String sort, String sortBy) {
        logger.info("获取难度为 {} 的题目, sort={}, sortBy={}", difficulty, sort, sortBy);
        List<OJProblem> list = problemMapper.getProblemsByDifficulty(difficulty);
        return sortByParam(list, sort, sortBy);
    }

    @Override
    public List<OJProblem> getActiveProblems(String sort, String sortBy) {
        logger.info("获取可用题目列表, sort={}, sortBy={}", sort, sortBy);
        List<OJProblem> list = problemMapper.getProblemsByStatus("ACTIVE");
        return sortByParam(list, sort, sortBy);
    }

    /**
     * 根据排序参数对列表进行排序（内存排序，用于不分页的场景）
     */
    private List<OJProblem> sortByParam(List<OJProblem> list, String sort, String sortBy) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        boolean desc = "desc".equalsIgnoreCase(sort);
        boolean byTime = "createdAt".equalsIgnoreCase(sortBy);
        
        if (byTime) {
            list.sort(Comparator.comparing(OJProblem::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            list.sort(Comparator.comparing(OJProblem::getId, 
                Comparator.nullsLast(Comparator.naturalOrder())));
        }
        
        if (desc) {
            Collections.reverse(list);
        }
        return list;
    }

    @Override
    public void addProblem(OJProblem problem) {
        logger.info("添加新题目：{}", problem.getTitle());

        // === id 改用自增主键，不再手动 setId ===
        problem.setStatus("ACTIVE");
        problem.setSubmissionCount(0);
        problem.setAcRate(0.0);

        // === null 兜底：避免 MySQL 8 NOT NULL 约束失败 ===
        if (problem.getDifficulty() == null || problem.getDifficulty().isEmpty()) {
            problem.setDifficulty("medium");
        }
        if (problem.getTags() == null) {
            problem.setTags("");
        }
        if (problem.getDescription() == null) {
            problem.setDescription("");
        }
        if (problem.getTemplate() == null) {
            problem.setTemplate("");
        }

        // === 智能题号：若传入题号为空/重复 → 自动递增 ===
        if (problem.getProblemNo() == null || problem.getProblemNo().trim().isEmpty()
                || isProblemNoExists(problem.getProblemNo())) {
            String newNo = generateNextProblemNo();
            logger.info("题号 {} 不可用，自动分配新题号：{}", problem.getProblemNo(), newNo);
            problem.setProblemNo(newNo);
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        problem.setCreatedAt(now);
        problem.setUpdatedAt(now);

        problemMapper.insertProblem(problem);
        logger.info("题目添加成功：id={}, problemNo={}", problem.getId(), problem.getProblemNo());
    }

    /**
     * 检查题号是否已存在
     */
    private boolean isProblemNoExists(String problemNo) {
        return problemMapper.findByProblemNo(problemNo) != null;
    }

    /**
     * 生成下一个可用题号：
     *   1. 查数据库中最大的纯数字题号
     *   2. +1
     *   3. 若冲突（理论上不该发生，但防止并发），再 +1 直到不冲突
     */
    @Override
    public String generateNextProblemNo() {
        Long currentMax = problemMapper.getMaxNumericProblemNo();
        long candidate = (currentMax == null ? 0L : currentMax) + 1;
        int maxRetry = 1000;   // 防止极端死循环
        while (isProblemNoExists(String.valueOf(candidate))) {
            candidate++;
            if (--maxRetry <= 0) {
                // 极端兜底：使用时间戳后缀
                return String.valueOf(System.currentTimeMillis()).substring(5); // 9 位
            }
        }
        return String.valueOf(candidate);
    }

    /**
     * 批量生成 N 个连续题号：max+1, max+2, ... max+N
     * 用于 AI 一次性生成多道题时给每道题预分配真实题号
     */
    @Override
    public List<String> generateNextProblemNos(int count) {
        if (count <= 0) return Collections.emptyList();
        Long currentMax = problemMapper.getMaxNumericProblemNo();
        long base = (currentMax == null ? 0L : currentMax) + 1;
        List<String> result = new ArrayList<>(count);
        long candidate = base;
        for (int i = 0; i < count; i++) {
            // 跳过冲突（极端情况：批量内/并发）
            int retry = 1000;
            while (isProblemNoExists(String.valueOf(candidate))) {
                candidate++;
                if (--retry <= 0) {
                    candidate = System.currentTimeMillis() / 1000;
                    break;
                }
            }
            result.add(String.valueOf(candidate));
            candidate++;
        }
        return result;
    }

    @Override
    public void updateProblem(OJProblem problem) {
        logger.info("更新题目：{}", problem.getId());

        // === null 兜底 ===
        if (problem.getTags() == null) {
            problem.setTags("");
        }
        if (problem.getDescription() == null) {
            problem.setDescription("");
        }
        if (problem.getTemplate() == null) {
            problem.setTemplate("");
        }
        if (problem.getDifficulty() == null || problem.getDifficulty().isEmpty()) {
            problem.setDifficulty("medium");
        }

        problem.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        problemMapper.updateProblem(problem);

        logger.info("题目更新成功：{}", problem.getId());
    }

    @Override
    public boolean updateStatus(String id, String status) {
        logger.info("更新题目状态：id={}, status={}", id, status);
        int rows = problemMapper.updateStatus(Long.valueOf(id), status);
        logger.info("题目状态更新{}：id={}", rows > 0 ? "成功" : "失败（题号不存在）", id);
        return rows > 0;
    }

    @Override
    public void deleteProblem(String id) {
        logger.info("删除题目：{}", id);
        problemMapper.deleteProblem(Long.valueOf(id));
        logger.info("题目删除成功：{}", id);
    }

    @Override
    public void incrementSubmission(String problemId) {
        logger.info("增加提交次数：{}", problemId);
        problemMapper.updateSubmissionCount(Long.valueOf(problemId));
    }

    @Override
    public int countProblems() {
        return problemMapper.countProblems();
    }

    @Override
    public int countByDifficulty(String difficulty) {
        return problemMapper.countByDifficulty(difficulty);
    }
}