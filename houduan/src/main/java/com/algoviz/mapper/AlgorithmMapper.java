package com.algoviz.mapper;

import com.algoviz.entity.Algorithm;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AlgorithmMapper {
    @Insert("INSERT INTO algorithm (id, name, category, description, time_complexity, space_complexity, pseudocode, status, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{category}, #{description}, #{timeComplexity}, #{spaceComplexity}, #{pseudocode}, #{status}, datetime('now'), datetime('now'))")
    int insert(Algorithm algorithm);

    @Select("SELECT * FROM algorithm ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Algorithm> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM algorithm")
    int count();

    @Select("SELECT * FROM algorithm WHERE id = #{id}")
    Algorithm findById(String id);

    @Update("UPDATE algorithm SET name=#{name}, category=#{category}, description=#{description}, time_complexity=#{timeComplexity}, " +
            "space_complexity=#{spaceComplexity}, pseudocode=#{pseudocode}, status=#{status}, updated_at=datetime('now') WHERE id=#{id}")
    int update(Algorithm algorithm);

    @Delete("DELETE FROM algorithm WHERE id=#{id}")
    int deleteById(String id);
}
