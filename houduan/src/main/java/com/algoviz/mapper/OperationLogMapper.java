package com.algoviz.mapper;

import com.algoviz.entity.OperationLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    @Insert("INSERT INTO operation_log (id, user_id, username, module, action, detail, ip, created_at) " +
            "VALUES (#{id}, #{userId}, #{username}, #{module}, #{action}, #{detail}, #{ip}, #{createdAt})")
    int insert(OperationLog operationLog);

    @Select("SELECT * FROM operation_log ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<OperationLog> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM operation_log")
    int count();

    @Select("SELECT * FROM operation_log WHERE id = #{id}")
    OperationLog findById(String id);
}
