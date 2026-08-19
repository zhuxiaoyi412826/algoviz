package com.algoviz.mapper;

import com.algoviz.entity.OJSolution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OJSolutionMapper {

    /** 用户侧：分页查询已发布题解（排除审核驳回/已删除） */
    List<OJSolution> selectPublished(@Param("problemId") Long problemId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    int countPublished(@Param("problemId") Long problemId);

    /** 后台：分页查询所有题解（含审核中/驳回） */
    List<OJSolution> selectByPage(@Param("keyword") String keyword,
                                  @Param("auditStatus") String auditStatus,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    int countByPage(@Param("keyword") String keyword,
                    @Param("auditStatus") String auditStatus);

    OJSolution selectById(@Param("id") Long id);

    /** 查询用户自己的题解（编辑页用，返回原文） */
    OJSolution selectByUserAndProblem(@Param("userId") Long userId,
                                      @Param("problemId") Long problemId);

    int insert(OJSolution s);

    int updateById(OJSolution s);

    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") String auditStatus,
                          @Param("riskLevel") String riskLevel,
                          @Param("detectSummary") String detectSummary);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int incrementViewCount(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementCommentCount(@Param("id") Long id, @Param("delta") int delta);

    int countByProblemAndUser(@Param("problemId") Long problemId, @Param("userId") Long userId);
}
