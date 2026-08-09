package com.algoviz.service.impl;

import com.algoviz.dto.interview.InterviewFrontStats;
import com.algoviz.dto.interview.InterviewTagVO;
import com.algoviz.dto.interview.InterviewUserListVO;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.InterviewFavorite;
import com.algoviz.entity.InterviewHistory;
import com.algoviz.entity.InterviewLike;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.entity.InterviewTag;
import com.algoviz.mapper.InterviewProblemMapper;
import com.algoviz.mapper.InterviewUserMapper;
import com.algoviz.service.InterviewUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewUserServiceImpl implements InterviewUserService {

    private final InterviewProblemMapper problemMapper;
    private final InterviewUserMapper userMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =================== F1 列表 ===================
    @Override
    public PageResult<InterviewProblem> listProblems(String keyword, String difficulty, String category,
                                                     String tag, Integer onlyFrequent,
                                                     String sortBy, String order, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        int offset = (page - 1) * pageSize;
        List<InterviewProblem> list = problemMapper.selectFrontList(
                keyword, difficulty, category, tag, onlyFrequent, sortBy, order, offset, pageSize);
        int total = problemMapper.countFrontList(keyword, difficulty, category, tag, onlyFrequent);
        return PageResult.of(list, total, page, pageSize);
    }

    // =================== 查看详情（阅读量+1，记录历史） ===================
    private InterviewProblem doGetDetail(InterviewProblem p, Long userId) {
        if (p == null) return null;
        problemMapper.incViewCount(p.getId());
        p.setViewCount(p.getViewCount() + 1);
        if (userId != null && userId > 0) {
            InterviewHistory exist = userMapper.findHistory(userId, p.getId());
            if (exist == null) {
                userMapper.insertHistory(userId, p.getId());
            } else {
                userMapper.updateHistoryViewTime(exist.getId());
            }
        }
        return p;
    }

    @Override
    @Transactional
    public InterviewProblem getDetailById(Long id, Long userId) {
        InterviewProblem p = problemMapper.selectByIdActive(id);
        return doGetDetail(p, userId);
    }

    @Override
    @Transactional
    public InterviewProblem getDetailByNo(String problemNo, Long userId) {
        InterviewProblem p = problemMapper.selectByNoActive(problemNo);
        return doGetDetail(p, userId);
    }

    // =================== F4 标签 ===================
    @Override
    public List<InterviewTagVO> listTags() {
        List<InterviewTag> all = userMapper.selectAllTags();
        List<InterviewTagVO> out = new ArrayList<>(all.size());
        for (InterviewTag t : all) {
            if (t.getUseCount() == null || t.getUseCount() < 0) continue;
            out.add(InterviewTagVO.of(t.getName(), t.getUseCount()));
        }
        return out;
    }

    // =================== F5 分类 ===================
    @Override
    public List<String> listCategories() {
        return problemMapper.listAllCategories();
    }

    // =================== F6 收藏列表 ===================
    @Override
    public PageResult<InterviewUserListVO> listFavorites(Long userId, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        int offset = (page - 1) * pageSize;
        List<InterviewFavorite> favs = userMapper.selectFavoritesByUser(userId, offset, pageSize);
        int total = userMapper.countFavoritesByUser(userId);
        List<InterviewUserListVO> out = new ArrayList<>(favs.size());
        for (InterviewFavorite f : favs) {
            InterviewProblem p = problemMapper.selectByIdActive(f.getProblemId());
            if (p == null) p = problemMapper.selectById(f.getProblemId());
            InterviewUserListVO v = new InterviewUserListVO();
            v.setId(f.getProblemId());
            v.setProblemNo(f.getProblemNo() != null ? f.getProblemNo() : (p != null ? p.getProblemNo() : ""));
            v.setTitle(p != null ? p.getTitle() : "(题目已删除)");
            v.setDifficulty(p != null ? p.getDifficulty() : "easy");
            v.setDifficultyLabel(p != null ? p.getDifficultyLabel() : "简单");
            v.setCategory(p != null ? p.getCategory() : "");
            v.setViewCount(p != null ? p.getViewCount() : 0);
            v.setIsFrequent(p != null ? p.getIsFrequent() : 0);
            if (f.getCollectTime() != null) v.setCollectTime(f.getCollectTime().format(FMT));
            out.add(v);
        }
        return PageResult.of(out, total, page, pageSize);
    }

    // =================== F7 浏览历史 ===================
    @Override
    public PageResult<InterviewUserListVO> listHistory(Long userId, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        int offset = (page - 1) * pageSize;
        List<InterviewHistory> his = userMapper.selectHistoryByUser(userId, offset, pageSize);
        int total = userMapper.countHistoryByUser(userId);
        List<InterviewUserListVO> out = new ArrayList<>(his.size());
        for (InterviewHistory h : his) {
            InterviewProblem p = problemMapper.selectByIdActive(h.getProblemId());
            if (p == null) p = problemMapper.selectById(h.getProblemId());
            InterviewUserListVO v = new InterviewUserListVO();
            v.setId(h.getProblemId());
            v.setProblemNo(p != null ? p.getProblemNo() : "");
            v.setTitle(p != null ? p.getTitle() : "(题目已删除)");
            v.setDifficulty(p != null ? p.getDifficulty() : "easy");
            v.setDifficultyLabel(p != null ? p.getDifficultyLabel() : "简单");
            v.setCategory(p != null ? p.getCategory() : "");
            v.setViewCount(p != null ? p.getViewCount() : 0);
            v.setIsFrequent(p != null ? p.getIsFrequent() : 0);
            if (h.getViewTime() != null) v.setViewTime(h.getViewTime().format(FMT));
            out.add(v);
        }
        return PageResult.of(out, total, page, pageSize);
    }

    // =================== F8/F9 历史删除 ===================
    @Override
    public boolean deleteHistory(Long userId, Long problemId) {
        return userMapper.deleteHistory(userId, problemId) > 0;
    }

    @Override
    public int clearHistory(Long userId) {
        return userMapper.clearHistory(userId);
    }

    // =================== F10/F11/F12 收藏 ===================
    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long problemId) {
        InterviewProblem p = problemMapper.selectByIdActive(problemId);
        if (p == null) return false;
        return userMapper.insertFavorite(userId, problemId, p.getProblemNo()) > 0;
    }

    @Override
    public boolean removeFavorite(Long userId, Long problemId) {
        return userMapper.deleteFavorite(userId, problemId) > 0;
    }

    @Override
    public int clearFavorites(Long userId) {
        return userMapper.clearFavorites(userId);
    }

    // =================== F13 点赞 F14 点踩 ===================
    @Override
    @Transactional
    public boolean likeProblem(Long userId, Long id) {
        InterviewProblem p = problemMapper.selectByIdActive(id);
        if (p == null) return false;
        InterviewLike like = userMapper.findLike(userId, id);
        if (like == null) {
            userMapper.insertLike(userId, id, "LIKE");
            problemMapper.updateLikeCount(id, 1, 0);
            return true;
        }
        if ("LIKE".equals(like.getType())) return true; // 已点赞
        // DISLIKE -> LIKE
        userMapper.updateLikeType(like.getId(), "LIKE");
        problemMapper.updateLikeCount(id, 1, -1);
        return true;
    }

    @Override
    @Transactional
    public boolean dislikeProblem(Long userId, Long id) {
        InterviewProblem p = problemMapper.selectByIdActive(id);
        if (p == null) return false;
        InterviewLike like = userMapper.findLike(userId, id);
        if (like == null) {
            userMapper.insertLike(userId, id, "DISLIKE");
            problemMapper.updateLikeCount(id, 0, 1);
            return true;
        }
        if ("DISLIKE".equals(like.getType())) return true;
        userMapper.updateLikeType(like.getId(), "DISLIKE");
        problemMapper.updateLikeCount(id, -1, 1);
        return true;
    }

    // =================== F15 是否收藏 ===================
    @Override
    public boolean isFavorite(Long userId, Long problemId) {
        return userMapper.findFavorite(userId, problemId) != null;
    }

    // =================== F16 前台统计 ===================
    @Override
    public InterviewFrontStats frontStats() {
        InterviewFrontStats s = new InterviewFrontStats();
        s.setTotalView(userMapper.sumFrontViewCount());
        s.setTotalLike(userMapper.sumFrontLikeCount());
        s.setTotalDislike(userMapper.sumFrontDislikeCount());
        s.setTotalCollect(userMapper.sumFrontCollectCount());
        return s;
    }

    // =================== F17 搜索（含 description） ===================
    @Override
    public PageResult<InterviewProblem> search(String keyword, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        int offset = (page - 1) * pageSize;
        List<InterviewProblem> list = problemMapper.selectFrontSearch(keyword, offset, pageSize);
        int total = problemMapper.countFrontSearch(keyword);
        return PageResult.of(list, total, page, pageSize);
    }
}
