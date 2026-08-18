package com.algoviz.mapper;

import com.algoviz.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {

    List<SensitiveWord> selectByPage(@Param("keyword") String keyword,
                                     @Param("category") String category,
                                     @Param("level") String level,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    int countByPage(@Param("keyword") String keyword,
                    @Param("category") String category,
                    @Param("level") String level);

    /** 全部启用词（构建 DFA / 发布版本用） */
    List<SensitiveWord> selectAllEnabled();

    SensitiveWord selectById(@Param("id") Long id);

    SensitiveWord selectByWord(@Param("word") String word);

    int insert(SensitiveWord w);

    int insertBatch(@Param("list") List<SensitiveWord> list);

    int updateById(SensitiveWord w);

    int deleteById(@Param("id") Long id);

    int deleteBatch(@Param("ids") List<Long> ids);

    int deleteAll();

    int countAll();
}
