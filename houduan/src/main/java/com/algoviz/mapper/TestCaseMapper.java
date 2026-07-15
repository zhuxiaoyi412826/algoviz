package com.algoviz.mapper;

import com.algoviz.entity.TestCase;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestCaseMapper {
    @Insert("INSERT INTO test_case (id, problem_id, input, output, score, is_sample) " +
            "VALUES (#{id}, #{problemId}, #{input}, #{output}, #{score}, #{isSample})")
    int insert(TestCase testCase);

    @Select("SELECT * FROM test_case WHERE problem_id = #{problemId}")
    List<TestCase> findByProblemId(String problemId);

    @Select("SELECT * FROM test_case WHERE id = #{id}")
    TestCase findById(String id);

    @Update("UPDATE test_case SET input=#{input}, output=#{output}, score=#{score}, is_sample=#{isSample} WHERE id=#{id}")
    int update(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id=#{id}")
    int deleteById(String id);

    @Delete("DELETE FROM test_case WHERE problem_id = #{problemId}")
    int deleteByProblemId(String problemId);
}
