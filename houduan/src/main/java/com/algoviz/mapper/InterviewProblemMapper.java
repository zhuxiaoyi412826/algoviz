package com.algoviz.mapper;

import com.algoviz.entity.InterviewProblem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewProblemMapper {

    // === 后台管理筛选（含逻辑删除） ===
    List<InterviewProblem> selectAdminList(@Param("keyword") String keyword,
                                           @Param("tag") String tag,
                                           @Param("difficulty") String difficulty,
                                           @Param("category") String category,
                                           @Param("status") String status,
                                           @Param("isFrequent") Integer isFrequent,
                                           @Param("sortBy") String sortBy,
                                           @Param("order") String order,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    int countAdminList(@Param("keyword") String keyword,
                       @Param("tag") String tag,
                       @Param("difficulty") String difficulty,
                       @Param("category") String category,
                       @Param("status") String status,
                       @Param("isFrequent") Integer isFrequent);

    // === 前台查询（仅 ACTIVE + is_deleted=0） ===
    List<InterviewProblem> selectFrontList(@Param("keyword") String keyword,
                                           @Param("difficulty") String difficulty,
                                           @Param("category") String category,
                                           @Param("tag") String tag,
                                           @Param("onlyFrequent") Integer onlyFrequent,
                                           @Param("sortBy") String sortBy,
                                           @Param("order") String order,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    int countFrontList(@Param("keyword") String keyword,
                       @Param("difficulty") String difficulty,
                       @Param("category") String category,
                       @Param("tag") String tag,
                       @Param("onlyFrequent") Integer onlyFrequent);

    // === 前台全局搜索（扩展到 description 字段） ===
    List<InterviewProblem> selectFrontSearch(@Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    int countFrontSearch(@Param("keyword") String keyword);

    // === CRUD ===
    InterviewProblem selectById(@Param("id") Long id);
    InterviewProblem selectByIdActive(@Param("id") Long id);
    InterviewProblem selectByNo(@Param("problemNo") String problemNo);
    InterviewProblem selectByNoActive(@Param("problemNo") String problemNo);

    int insert(InterviewProblem p);
    int updateById(InterviewProblem p);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") String updatedBy);
    int logicDelete(@Param("id") Long id, @Param("updatedBy") String updatedBy);
    int batchLogicDelete(@Param("ids") List<Long> ids, @Param("updatedBy") String updatedBy);
    int physicalDelete(@Param("id") Long id);
    int batchPhysicalDelete(@Param("ids") List<Long> ids);

    // === 计数更新 ===
    int incViewCount(@Param("id") Long id);
    int updateLikeCount(@Param("id") Long id,
                        @Param("likeDelta") int likeDelta,
                        @Param("dislikeDelta") int dislikeDelta);

    // === 统计 ===
    long getMaxNumericProblemNo();    // 当前最大数字题号
    List<String> listAllCategories(); // 分类列表（去重+非空）

    // === 批量导入导出 ===
    List<InterviewProblem> listAllForExport(@Param("difficulty") String difficulty,
                                            @Param("category") String category);

    // === 按 ID 列表批量查询（向量检索后查 MySQL） ===
    List<InterviewProblem> selectByIds(@Param("ids") List<Long> ids);
}
