package com.algoviz.mapper;

import com.algoviz.entity.SystemConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SystemConfigMapper {
    @Select("SELECT * FROM system_config")
    List<SystemConfig> findAll();

    @Select("SELECT * FROM system_config WHERE config_group = #{group}")
    List<SystemConfig> findByGroup(String group);

    @Select("SELECT * FROM system_config WHERE key = #{key}")
    SystemConfig findByKey(String key);

    @Insert("INSERT OR REPLACE INTO system_config (key, value, type, label, description, config_group) " +
            "VALUES (#{key}, #{value}, #{type}, #{label}, #{description}, #{configGroup})")
    int insertOrUpdate(SystemConfig config);

    @Update("UPDATE system_config SET value = #{value} WHERE key = #{key}")
    int updateValue(@Param("key") String key, @Param("value") String value);
}
