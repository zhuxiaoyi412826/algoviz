package com.algoviz.mapper;

import com.algoviz.entity.UserOauth;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * user_oauth 第三方授权绑定表 Mapper
 *
 * <p>只操作通用模板中明确存在的列（id/user_id/provider/open_id/nickname/avatar_url/created_at），
 * 不依赖 union_id / token 等可选列，保证与不同环境的表结构兼容。</p>
 */
@Mapper
public interface UserOauthMapper {

    /**
     * 按 (provider, openId) 精确查找绑定记录（依赖 UNIQUE(provider, open_id)）
     */
    @Select("SELECT id, user_id, provider, open_id, nickname, avatar_url, created_at " +
            "FROM user_oauth WHERE provider = #{provider} AND open_id = #{openId} LIMIT 1")
    UserOauth findByProviderAndOpenId(@Param("provider") String provider,
                                      @Param("openId") String openId);

    /**
     * 新增绑定记录（created_at 由数据库默认值 CURRENT_TIMESTAMP 写入）
     */
    @Insert("INSERT INTO user_oauth (user_id, provider, open_id, nickname, avatar_url) " +
            "VALUES (#{userId}, #{provider}, #{openId}, #{nickname}, #{avatarUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserOauth oauth);
}
