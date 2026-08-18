package com.algoviz.mapper;

import com.algoviz.entity.ContentAuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContentAuditRecordMapper {

    List<ContentAuditRecord> selectByPage(@Param("auditStatus") String auditStatus,
                                          @Param("riskLevel") String riskLevel,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    int countByPage(@Param("auditStatus") String auditStatus,
                    @Param("riskLevel") String riskLevel,
                    @Param("keyword") String keyword);

    ContentAuditRecord selectBySubmitId(@Param("submitId") String submitId);

    int insert(ContentAuditRecord r);

    /** 各状态数量统计 */
    List<Map<String, Object>> countByStatus();

    long countAll();
}
