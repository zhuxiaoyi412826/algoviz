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

    @Select("SELECT * FROM system_config WHERE `key` = #{key}")
    SystemConfig findByKey(String key);

    /**
     * MySQL 方言：主键（key）重复时更新 value/type/label/description/config_group 字段
     */
    @Insert("INSERT INTO system_config (`key`, value, type, label, description, config_group) " +
            "VALUES (#{key}, #{value}, #{type}, #{label}, #{description}, #{configGroup}) " +
            "ON DUPLICATE KEY UPDATE " +
            "  value = VALUES(value), " +
            "  type = VALUES(type), " +
            "  label = VALUES(label), " +
            "  description = VALUES(description), " +
            "  config_group = VALUES(config_group)")
    int insertOrUpdate(SystemConfig config);

    @Update("UPDATE system_config SET value = #{value} WHERE `key` = #{key}")
    int updateValue(@Param("key") String key, @Param("value") String value);
}
