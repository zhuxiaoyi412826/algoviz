package com.algoviz.mapper;

import com.algoviz.entity.InterviewFavorite;
import com.algoviz.entity.InterviewHistory;
import com.algoviz.entity.InterviewLike;
import com.algoviz.entity.InterviewTag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterviewUserMapper {

    // ============ 收藏 interview_favorite ============
    @Select("SELECT f.*, p.problem_no AS problem_no FROM interview_favorite f " +
            "LEFT JOIN interview_problem p ON p.id = f.problem_id " +
            "WHERE f.user_id = #{userId} ORDER BY f.collect_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<InterviewFavorite> selectFavoritesByUser(@Param("userId") Long userId,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM interview_favorite WHERE user_id = #{userId}")
    int countFavoritesByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM interview_favorite WHERE user_id = #{userId} AND problem_id = #{problemId}")
    InterviewFavorite findFavorite(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Insert("INSERT IGNORE INTO interview_favorite (user_id, problem_id, problem_no, collect_time) " +
            "VALUES (#{userId}, #{problemId}, #{problemNo}, NOW())")
    int insertFavorite(@Param("userId") Long userId,
                       @Param("problemId") Long problemId,
                       @Param("problemNo") String problemNo);

    @Delete("DELETE FROM interview_favorite WHERE user_id = #{userId} AND problem_id = #{problemId}")
    int deleteFavorite(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Delete("DELETE FROM interview_favorite WHERE user_id = #{userId}")
    int clearFavorites(@Param("userId") Long userId);

    // ============ 历史 interview_history ============
    @Select("SELECT h.* FROM interview_history h WHERE h.user_id = #{userId} " +
            "ORDER BY h.view_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<InterviewHistory> selectHistoryByUser(@Param("userId") Long userId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM interview_history WHERE user_id = #{userId}")
    int countHistoryByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM interview_history WHERE user_id = #{userId} AND problem_id = #{problemId}")
    InterviewHistory findHistory(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Insert("INSERT INTO interview_history (user_id, problem_id, view_time) VALUES (#{userId}, #{problemId}, NOW())")
    int insertHistory(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Update("UPDATE interview_history SET view_time = NOW() WHERE id = #{id}")
    int updateHistoryViewTime(@Param("id") Long id);

    @Delete("DELETE FROM interview_history WHERE user_id = #{userId} AND problem_id = #{problemId}")
    int deleteHistory(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Delete("DELETE FROM interview_history WHERE user_id = #{userId}")
    int clearHistory(@Param("userId") Long userId);

    // ============ 点赞点踩 interview_like ============
    @Select("SELECT * FROM interview_like WHERE user_id = #{userId} AND problem_id = #{problemId}")
    InterviewLike findLike(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Insert("INSERT INTO interview_like (user_id, problem_id, type) VALUES (#{userId}, #{problemId}, #{type})")
    int insertLike(@Param("userId") Long userId,
                   @Param("problemId") Long problemId,
                   @Param("type") String type);

    @Update("UPDATE interview_like SET type = #{type} WHERE id = #{id}")
    int updateLikeType(@Param("id") Long id, @Param("type") String type);

    // ============ 标签 interview_tag ============
    @Select("SELECT * FROM interview_tag ORDER BY use_count DESC, sort_order ASC, id ASC")
    List<InterviewTag> selectAllTags();

    @Select("SELECT * FROM interview_tag WHERE name = #{name}")
    InterviewTag findTagByName(@Param("name") String name);

    @Insert("INSERT INTO interview_tag (name, category, use_count, sort_order) VALUES (#{name}, #{category}, 1, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTag(InterviewTag tag);

    @Update("UPDATE interview_tag SET use_count = use_count + #{delta} WHERE name = #{name}")
    int incTagUseCount(@Param("name") String name, @Param("delta") int delta);

    // ============ 前台全站统计 ============
    @Select("SELECT COALESCE(SUM(view_count),0) FROM interview_problem WHERE status='ACTIVE' AND is_deleted=0")
    long sumFrontViewCount();
    @Select("SELECT COALESCE(SUM(like_count),0) FROM interview_problem WHERE status='ACTIVE' AND is_deleted=0")
    long sumFrontLikeCount();
    @Select("SELECT COALESCE(SUM(dislike_count),0) FROM interview_problem WHERE status='ACTIVE' AND is_deleted=0")
    long sumFrontDislikeCount();
    @Select("SELECT COUNT(*) FROM interview_favorite")
    long sumFrontCollectCount();
}
