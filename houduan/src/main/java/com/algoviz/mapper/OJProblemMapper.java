package com.algoviz.mapper;

import com.algoviz.entity.OJProblem;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OJProblemMapper {
    
    @Select("SELECT * FROM oj_problem ORDER BY problem_no ASC")
    List<OJProblem> getAllProblems();
    
    @Select("SELECT * FROM oj_problem WHERE id = #{id}")
    OJProblem getProblemById(Long id);
    
    @Select("SELECT * FROM oj_problem WHERE problem_no = #{problemNo}")
    OJProblem getProblemByNo(String problemNo);
    
    @Select("SELECT * FROM oj_problem WHERE title LIKE CONCAT('%', #{keyword}, '%') OR tags LIKE CONCAT('%', #{keyword}, '%') OR problem_no LIKE CONCAT('%', #{keyword}, '%') ORDER BY problem_no ASC")
    List<OJProblem> searchProblems(@Param("keyword") String keyword);
    
    @Select("SELECT * FROM oj_problem WHERE difficulty = #{difficulty} ORDER BY problem_no ASC")
    List<OJProblem> getProblemsByDifficulty(String difficulty);
    
    @Select("SELECT * FROM oj_problem WHERE status = #{status} ORDER BY problem_no ASC")
    List<OJProblem> getProblemsByStatus(String status);
    
    @Insert("INSERT INTO oj_problem (problem_no, title, difficulty, tags, description, template, status, submission_count, ac_rate, created_at, updated_at) " +
            "VALUES (#{problemNo}, #{title}, #{difficulty}, #{tags}, #{description}, #{template}, #{status}, #{submissionCount}, #{acRate}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertProblem(OJProblem problem);

    @Update("UPDATE oj_problem SET problem_no=#{problemNo}, title=#{title}, difficulty=#{difficulty}, tags=#{tags}, description=#{description}, " +
            "template=#{template}, status=#{status}, submission_count=#{submissionCount}, ac_rate=#{acRate}, updated_at=#{updatedAt} WHERE id=#{id}")
    void updateProblem(OJProblem problem);

    @Update("UPDATE oj_problem SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM oj_problem WHERE id = #{id}")
    void deleteProblem(Long id);

    @Update("UPDATE oj_problem SET submission_count = submission_count + 1 WHERE id = #{id}")
    void updateSubmissionCount(Long id);
    
    @Select("SELECT COUNT(*) FROM oj_problem")
    int countProblems();
    
    @Select("SELECT COUNT(*) FROM oj_problem WHERE difficulty = #{difficulty}")
    int countByDifficulty(String difficulty);
    
    // 原有方法保持兼容
    @Insert("INSERT INTO oj_problem (problem_no, title, difficulty, tags, description, template, status, submission_count, ac_rate, created_at, updated_at) " +
            "VALUES (#{problemNo}, #{title}, #{difficulty}, #{tags}, #{description}, #{template}, #{status}, #{submissionCount}, #{acRate}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OJProblem problem);

    @Select("SELECT * FROM oj_problem ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<OJProblem> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM oj_problem")
    int count();

    @Select("SELECT * FROM oj_problem WHERE id = #{id}")
    OJProblem findById(Long id);

    @Select("SELECT * FROM oj_problem WHERE problem_no = #{problemNo}")
    OJProblem findByProblemNo(String problemNo);

    /**
     * 查询数据库中最大的数字题号（用于智能生成下一个题号）
     * 只统计纯数字 problem_no，忽略非数字的（如 "AI-xxx"）
     */
    @Select("SELECT COALESCE(MAX(CAST(problem_no AS UNSIGNED)), 0) FROM oj_problem WHERE problem_no REGEXP '^[0-9]+$'")
    Long getMaxNumericProblemNo();

    @Update("UPDATE oj_problem SET title=#{title}, difficulty=#{difficulty}, tags=#{tags}, description=#{description}, template=#{template}, " +
            "status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int update(OJProblem problem);

    @Delete("DELETE FROM oj_problem WHERE id=#{id}")
    int deleteById(Long id);
}