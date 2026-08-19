package com.algoviz.mapper;

import com.algoviz.entity.OJSolutionLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OJSolutionLikeMapper {

    OJSolutionLike selectByUserAndTarget(@Param("userId") Long userId,
                                         @Param("targetType") String targetType,
                                         @Param("targetId") Long targetId);

    int insert(OJSolutionLike l);

    int deleteByUserAndTarget(@Param("userId") Long userId,
                              @Param("targetType") String targetType,
                              @Param("targetId") Long targetId);
}
