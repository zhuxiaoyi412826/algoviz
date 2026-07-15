package com.algoviz.mapper;

import com.algoviz.entity.AppUser;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AppUserMapper {
    @Insert("INSERT INTO app_user (id, openid, nickname, avatar, bind_time, status, ds_visits, algo_visits, oj_visits, ai_dialogues) " +
            "VALUES (#{id}, #{openid}, #{nickname}, #{avatar}, datetime('now'), #{status}, #{dsVisits}, #{algoVisits}, #{ojVisits}, #{aiDialogues})")
    int insert(AppUser appUser);

    @Select("SELECT * FROM app_user ORDER BY bind_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AppUser> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM app_user")
    int count();

    @Select("SELECT * FROM app_user WHERE id = #{id}")
    AppUser findById(String id);

    @Select("SELECT * FROM app_user WHERE openid = #{openid}")
    AppUser findByOpenid(String openid);

    @Update("UPDATE app_user SET nickname=#{nickname}, avatar=#{avatar}, status=#{status}, ds_visits=#{dsVisits}, " +
            "algo_visits=#{algoVisits}, oj_visits=#{ojVisits}, ai_dialogues=#{aiDialogues}, last_visit_time=#{lastVisitTime} WHERE id=#{id}")
    int update(AppUser appUser);

    @Delete("DELETE FROM app_user WHERE id=#{id}")
    int deleteById(String id);

    @Select("SELECT * FROM app_user ORDER BY bind_time DESC")
    List<AppUser> findAll();
}
