package com.algoviz.mapper;

import com.algoviz.entity.BanAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BanAppealMapper {

    /**
     * 新增申诉
     */
    int insert(BanAppeal appeal);

    /**
     * 根据ID查询申诉详情（含申诉理由全文）
     */
    BanAppeal findById(@Param("id") Long id);

    /**
     * 分页查询申诉列表，支持按状态筛选、按账号/邮箱模糊搜索
     */
    List<BanAppeal> findByPage(@Param("status") String status,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("pageSize") int pageSize);

    /**
     * 统计符合条件的申诉总数
     */
    int countByConditions(@Param("status") String status, @Param("keyword") String keyword);

    /**
     * 管理员处理申诉：写入处理结果、处理人、处理时间
     */
    int handle(@Param("id") Long id,
               @Param("status") String status,
               @Param("reply") String reply,
               @Param("handlerId") Long handlerId,
               @Param("handlerName") String handlerName);

    /**
     * 统计某用户待处理的申诉条数（用于拦截重复提交）
     */
    int countPendingByUserId(@Param("userId") Long userId);
}