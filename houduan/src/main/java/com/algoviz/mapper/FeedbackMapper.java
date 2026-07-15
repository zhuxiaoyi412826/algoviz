package com.algoviz.mapper;

import com.algoviz.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedbackMapper {
    
    /**
     * 分页获取反馈
     */
    List<Feedback> findByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    /**
     * 获取反馈总数
     */
    int count();
    
    /**
     * 获取所有反馈
     */
    List<Feedback> getAllFeedbacks();
    
    /**
     * 根据用户ID获取反馈
     */
    List<Feedback> getFeedbacksByUserId(@Param("userId") Long userId);
    
    /**
     * 根据状态获取反馈
     */
    List<Feedback> getFeedbacksByStatus(@Param("status") String status);
    
    /**
     * 根据类型获取反馈
     */
    List<Feedback> getFeedbacksByType(@Param("type") String type);
    
    /**
     * 根据ID获取反馈
     */
    Feedback findById(@Param("id") String id);
    
    /**
     * 插入反馈
     */
    int insert(Feedback feedback);
    
    /**
     * 更新反馈
     */
    int update(Feedback feedback);
    
    /**
     * 更新反馈（回复）
     */
    int updateFeedbackReply(@Param("id") Long id, 
                           @Param("reply") String reply, 
                           @Param("status") String status);
    
    /**
     * 删除反馈
     */
    int deleteById(@Param("id") String id);
    
    /**
     * 统计各状态数量
     */
    int countByStatus(@Param("status") String status);
}