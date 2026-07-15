package com.algoviz.mapper;

import com.algoviz.entity.DataStructure;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DataStructureMapper {
    @Insert("INSERT INTO data_structure (id, name, type, description, status, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{type}, #{description}, #{status}, datetime('now'), datetime('now'))")
    int insert(DataStructure dataStructure);

    @Select("SELECT * FROM data_structure ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<DataStructure> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM data_structure")
    int count();

    @Select("SELECT * FROM data_structure WHERE id = #{id}")
    DataStructure findById(String id);

    @Update("UPDATE data_structure SET name=#{name}, type=#{type}, description=#{description}, status=#{status}, updated_at=datetime('now') WHERE id=#{id}")
    int update(DataStructure dataStructure);

    @Delete("DELETE FROM data_structure WHERE id=#{id}")
    int deleteById(String id);
}
