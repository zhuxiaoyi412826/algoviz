package com.algoviz.mapper;

import com.algoviz.entity.SensitiveWordVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SensitiveWordVersionMapper {

    List<SensitiveWordVersion> selectAll();

    SensitiveWordVersion selectByVersionNo(@Param("versionNo") Integer versionNo);

    SensitiveWordVersion selectLatest();

    int insert(SensitiveWordVersion v);

    int count();
}
