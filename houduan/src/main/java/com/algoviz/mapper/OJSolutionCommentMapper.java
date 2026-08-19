package com.algoviz.mapper;

import com.algoviz.entity.OJSolutionComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OJSolutionCommentMapper {

    /** 用户侧：查询顶层评论（parent_id=0） */
    List<OJSolutionComment> selectTopLevel(@Param("solutionId") Long solutionId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    int countTopLevel(@Param("solutionId") Long solutionId);

    /** 查询某个顶层评论下的子评论 */
    List<OJSolutionComment> selectReplies(@Param("rootId") Long rootId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    int countReplies(@Param("rootId") Long rootId);

    /** 后台：分页查询所有评论 */
    List<OJSolutionComment> selectByPage(@Param("keyword") String keyword,
                                         @Param("auditStatus") String auditStatus,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    int countByPage(@Param("keyword") String keyword,
                    @Param("auditStatus") String auditStatus);

    OJSolutionComment selectById(@Param("id") Long id);

    int insert(OJSolutionComment c);

    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") String auditStatus,
                          @Param("riskLevel") String riskLevel,
                          @Param("detectSummary") String detectSummary);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int updateRootId(@Param("id") Long id, @Param("rootId") Long rootId);
}
