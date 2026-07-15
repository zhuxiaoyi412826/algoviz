package com.algoviz.mapper;

import com.algoviz.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    
    /**
     * 分页获取公告
     */
    List<Announcement> findByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    /**
     * 获取公告总数
     */
    int count();
    
    /**
     * 获取所有公告
     */
    List<Announcement> getAllAnnouncements();
    
    /**
     * 获取已发布的公告
     */
    List<Announcement> getPublishedAnnouncements();
    
    /**
     * 根据ID获取公告
     */
    Announcement findById(@Param("id") String id);
    
    /**
     * 插入公告
     */
    int insert(Announcement announcement);
    
    /**
     * 更新公告
     */
    int update(Announcement announcement);
    
    /**
     * 更新公告状态
     */
    int updateAnnouncementStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 删除公告
     */
    int deleteById(@Param("id") String id);
    
    /**
     * 获取最大排序号
     */
    Integer getMaxSortOrder();
}