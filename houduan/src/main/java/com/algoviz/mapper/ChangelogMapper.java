package com.algoviz.mapper;

import com.algoviz.entity.Changelog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 更新日志 Mapper
 *   管理后台：分页 + type/status/version 过滤、按ID详情、新增、更新、删除
 *   前台：仅查已发布（status=1），按类型过滤，按日期/版本号排序
 */
@Mapper
public interface ChangelogMapper {

    @Select("<script>" +
            "SELECT * FROM changelog WHERE 1=1" +
            "<if test='type != null and type != \"\" and type != \"all\"'>" +
            " AND `type` = #{type}" +
            "</if>" +
            "<if test='status != null'> AND `status` = #{status} </if>" +
            "<if test='version != null and version != \"\"'>" +
            " AND `version` LIKE CONCAT('%', #{version}, '%')" +
            "</if>" +
            " ORDER BY `release_date` DESC, id DESC" +
            " LIMIT #{offset}, #{pageSize}" +
            "</script>")
    @Results(id = "ChangelogMap", value = {
            @Result(column = "release_date", property = "releaseDate"),
            @Result(column = "known_issues", property = "knownIssues"),
            @Result(column = "issues_title", property = "issuesTitle"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<Changelog> findPage(@Param("offset") int offset,
                             @Param("pageSize") int pageSize,
                             @Param("type") String type,
                             @Param("status") Integer status,
                             @Param("version") String version);

    @Select("<script>" +
            "SELECT COUNT(*) FROM changelog WHERE 1=1" +
            "<if test='type != null and type != \"\" and type != \"all\"'>" +
            " AND `type` = #{type}" +
            "</if>" +
            "<if test='status != null'> AND `status` = #{status} </if>" +
            "<if test='version != null and version != \"\"'>" +
            " AND `version` LIKE CONCAT('%', #{version}, '%')" +
            "</if>" +
            "</script>")
    long countPage(@Param("type") String type,
                   @Param("status") Integer status,
                   @Param("version") String version);

    /**
     * 前台公开列表：仅已发布，支持按类型过滤。注意不放 LIMIT，交由前端一次性渲染（更新日志通常几十条）。
     * orderBy: release_date / version
     * orderDir: DESC / ASC
     */
    @Select("<script>" +
            "SELECT * FROM changelog WHERE `status` = 1" +
            "<if test='type != null and type != \"\" and type != \"all\"'>" +
            " AND `type` = #{type}" +
            "</if>" +
            "<choose>" +
            "  <when test='orderBy != null and orderBy == \"version\"'>" +
            "    ORDER BY `version` <choose><when test='orderDir != null and orderDir == \"ASC\"'>ASC</when><otherwise>DESC</otherwise></choose>, `release_date` DESC" +
            "  </when>" +
            "  <otherwise>" +
            "    ORDER BY `release_date` <choose><when test='orderDir != null and orderDir == \"ASC\"'>ASC</when><otherwise>DESC</otherwise></choose>, id DESC" +
            "  </otherwise>" +
            "</choose>" +
            "</script>")
    @ResultMap("ChangelogMap")
    List<Changelog> findPublicList(@Param("type") String type,
                                   @Param("orderBy") String orderBy,
                                   @Param("orderDir") String orderDir);

    @Select("SELECT * FROM changelog WHERE id = #{id}")
    @ResultMap("ChangelogMap")
    Changelog findById(@Param("id") Long id);

    @Insert("INSERT INTO changelog " +
            "(`version`, `type`, `summary`, `release_date`, `modules`, `details`, `known_issues`, `issues_title`, `status`) " +
            "VALUES (#{version}, #{type}, #{summary}, #{releaseDate}, #{modules}, #{details}, #{knownIssues}, #{issuesTitle}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Changelog changelog);

    @Update("<script>" +
            "UPDATE changelog SET " +
            " `version` = #{version}, " +
            " `type` = #{type}, " +
            " `summary` = #{summary}, " +
            " `release_date` = #{releaseDate}, " +
            " `modules` = #{modules}, " +
            " `details` = #{details}, " +
            " `known_issues` = #{knownIssues}, " +
            " `issues_title` = #{issuesTitle}, " +
            " `status` = #{status} " +
            " WHERE id = #{id}" +
            "</script>")
    int update(Changelog changelog);

    @Delete("DELETE FROM changelog WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
