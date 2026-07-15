package com.algoviz.mapper;

import com.algoviz.entity.AIPrompt;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AIPromptMapper {
    @Insert("INSERT INTO ai_prompt (id, title, category, content, usage_count, status, created_at) " +
            "VALUES (#{id}, #{title}, #{category}, #{content}, #{usageCount}, #{status}, datetime('now'))")
    int insert(AIPrompt aiPrompt);

    @Select("SELECT * FROM ai_prompt ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<AIPrompt> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_prompt")
    int count();

    @Select("SELECT * FROM ai_prompt WHERE id = #{id}")
    AIPrompt findById(String id);

    @Update("UPDATE ai_prompt SET title=#{title}, category=#{category}, content=#{content}, status=#{status} WHERE id=#{id}")
    int update(AIPrompt aiPrompt);

    @Update("UPDATE ai_prompt SET usage_count = usage_count + 1 WHERE id=#{id}")
    int incrementUsage(String id);

    @Delete("DELETE FROM ai_prompt WHERE id=#{id}")
    int deleteById(String id);
}
