package com.algoviz.service;

import com.algoviz.dto.interview.BatchImportResult;
import com.algoviz.dto.interview.InterviewAdminStats;
import com.algoviz.dto.interview.InterviewProblemSaveDTO;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.InterviewProblem;

import java.util.List;

public interface InterviewProblemAdminService {
    // B1 后台分页列表
    PageResult<InterviewProblem> list(String keyword, String tag, String difficulty, String category,
                                       String status, Integer isFrequent,
                                       String sortBy, String order, int page, int pageSize);

    // B2 详情
    InterviewProblem getById(Long id);

    // B3 手动添加
    InterviewProblem create(InterviewProblemSaveDTO dto, String adminId);

    // B4 修改
    boolean update(Long id, InterviewProblemSaveDTO dto, String adminId);

    // B5 状态切换
    boolean updateStatus(Long id, String status, String adminId);

    // B6 逻辑删除
    boolean logicDelete(Long id, String adminId);

    // B7 批量逻辑删除
    int batchLogicDelete(List<Long> ids, String adminId);

    // B8 物理删除
    boolean physicalDelete(Long id);

    // B9 批量物理删除
    int batchPhysicalDelete(List<Long> ids);

    // B10 JSON 批量导入
    BatchImportResult batchImport(List<InterviewProblemSaveDTO> problemList,
                                  boolean overwriteOnConflict, String adminId);

    // B12 导出
    List<InterviewProblem> listAllForExport(String difficulty, String category);

    // B13 AI 生成（同步）
    List<InterviewProblemSaveDTO> generateByAI(String category, String difficulty, int num);

    // B14 批量保存 AI 生成结果
    BatchImportResult batchSaveAIGenerated(List<InterviewProblemSaveDTO> problemList, String adminId);

    // B15 后台统计
    InterviewAdminStats adminStats();

    // 工具：生成下一个题目编号 MSxxx
    String generateNextProblemNo();
}
