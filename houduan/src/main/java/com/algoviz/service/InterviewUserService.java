package com.algoviz.service;

import com.algoviz.dto.interview.InterviewFrontStats;
import com.algoviz.dto.interview.InterviewTagVO;
import com.algoviz.dto.interview.InterviewUserListVO;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.InterviewProblem;

import java.util.List;

public interface InterviewUserService {
    // F1 前台列表
    PageResult<InterviewProblem> listProblems(String keyword, String difficulty, String category,
                                              String tag, Integer onlyFrequent,
                                              String sortBy, String order, int page, int pageSize);

    // F2 按 id 查看详情（含阅读量累加 + 浏览历史记录）
    InterviewProblem getDetailById(Long id, Long userId);

    // F3 按 problemNo 查看详情
    InterviewProblem getDetailByNo(String problemNo, Long userId);

    // F4 所有标签（按热度）
    List<InterviewTagVO> listTags();

    // F5 分类列表
    List<String> listCategories();

    // F6 收藏列表
    PageResult<InterviewUserListVO> listFavorites(Long userId, int page, int pageSize);

    // F7 浏览历史
    PageResult<InterviewUserListVO> listHistory(Long userId, int page, int pageSize);

    // F8 删除单条历史
    boolean deleteHistory(Long userId, Long problemId);

    // F9 清空历史
    int clearHistory(Long userId);

    // F10 收藏
    boolean addFavorite(Long userId, Long problemId);

    // F11 取消收藏
    boolean removeFavorite(Long userId, Long problemId);

    // F12 清空收藏
    int clearFavorites(Long userId);

    // F13 点赞
    boolean likeProblem(Long userId, Long id);

    // F14 点踩
    boolean dislikeProblem(Long userId, Long id);

    // F15 是否收藏
    boolean isFavorite(Long userId, Long problemId);

    // F16 前台统计
    InterviewFrontStats frontStats();

    // F17 模糊搜索（含 description）
    PageResult<InterviewProblem> search(String keyword, int page, int pageSize);
}
