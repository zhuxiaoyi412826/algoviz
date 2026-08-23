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

    // ============ 分类查询（操作人 / 模块 / 操作类型 / 日期范围） ============

    /**
     * 全条件分页查询
     * 支持：操作人 username 模糊匹配、module 精确匹配、action 精确匹配、startDate/endDate 日期范围
     */
    @Select("<script>" +
            "SELECT * FROM operation_log WHERE 1=1" +
            "<if test='username != null and username != \"\"'>" +
            " AND username LIKE CONCAT('%', #{username}, '%')" +
            "</if>" +
            "<if test='module != null and module != \"\"'>" +
            " AND module = #{module}" +
            "</if>" +
            "<if test='action != null and action != \"\"'>" +
            " AND action = #{action}" +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            " AND DATE(created_at) >= #{startDate}" +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            " AND DATE(created_at) &lt;= #{endDate}" +
            "</if>" +
            " ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<OperationLog> findByAllFilters(@Param("offset") int offset,
                                        @Param("pageSize") int pageSize,
                                        @Param("username") String username,
                                        @Param("module") String module,
                                        @Param("action") String action,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log WHERE 1=1" +
            "<if test='username != null and username != \"\"'>" +
            " AND username LIKE CONCAT('%', #{username}, '%')" +
            "</if>" +
            "<if test='module != null and module != \"\"'>" +
            " AND module = #{module}" +
            "</if>" +
            "<if test='action != null and action != \"\"'>" +
            " AND action = #{action}" +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            " AND DATE(created_at) >= #{startDate}" +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            " AND DATE(created_at) &lt;= #{endDate}" +
            "</if>" +
            "</script>")
    int countByAllFilters(@Param("username") String username,
                          @Param("module") String module,
                          @Param("action") String action,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate);

    /**
     * 取 operation_log 中出现过的所有去重操作人（用于前端下拉）
     */
    @Select("SELECT DISTINCT username FROM operation_log " +
            "WHERE username IS NOT NULL AND username <> '' AND username <> 'unknown' " +
            "ORDER BY username ASC")
    List<String> findAllOperators();
}
