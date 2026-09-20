package com.algoviz.mapper;

import com.algoviz.entity.UserOauth;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * user_oauth 第三方授权绑定表 Mapper
 *
 * <p>查询一律过滤 is_deleted=0；登录仅接受 bind_status=0（授权已撤销/过期的绑定不可登录，
 * 需要用户重新走一次授权流程）。第三方 token 相关列当前业务不持久化（保持 NULL），
 * 若后续启用必须走加密写入。MyBatis map-underscore-to-camel-case 已开启，列名自动映射。</p>
 */
@Mapper
public interface UserOauthMapper {

    /**
     * 按 (provider, openId) 查找有效绑定记录（自动过滤已解绑/授权失效）
     */
    @Select("SELECT id, user_id, provider, open_id, bind_scene, bind_status, login_count, " +
            "nickname, avatar_url, created_at " +
            "FROM user_oauth " +
            "WHERE provider = #{provider} AND open_id = #{openId} " +
            "AND is_deleted = 0 AND bind_status = 0 LIMIT 1")
    UserOauth findByProviderAndOpenId(@Param("provider") String provider,
                                      @Param("openId") String openId);

    /**
     * 新增绑定记录（自动注册场景 bind_scene=1；手动账号绑定 bind_scene=2；raw_profile 为 OAuth 原始 JSON 快照）
     * created_at / bind_status / is_deleted / token 相关列取数据库默认值
     */
    @Insert("INSERT INTO user_oauth (user_id, provider, open_id, bind_scene, nickname, avatar_url, raw_profile) " +
            "VALUES (#{userId}, #{provider}, #{openId}, #{bindScene}, #{nickname}, #{avatarUrl}, #{rawProfile})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserOauth oauth);

    /**
     * 登录成功计数：该绑定行累计登录次数 +1
     */
    @Update("UPDATE user_oauth SET login_count = login_count + 1 " +
            "WHERE id = #{id} AND is_deleted = 0")
    int increaseLoginCount(@Param("id") Long id);

    /**
     * 查询某用户的全部有效绑定（用于个人中心展示、后台用户详情）
     */
    @Select("SELECT id, user_id, provider, open_id, bind_scene, bind_status, login_count, " +
            "nickname, avatar_url, created_at " +
            "FROM user_oauth WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY provider ASC")
    List<UserOauth> findByUserId(@Param("userId") Integer userId);

    /**
     * 查询某用户在指定平台上的有效绑定（用于绑定前重复校验、解绑定位）
     */
    @Select("SELECT id, user_id, provider, open_id, bind_scene, bind_status, login_count, " +
            "nickname, avatar_url, created_at " +
            "FROM user_oauth WHERE user_id = #{userId} AND provider = #{provider} " +
            "AND is_deleted = 0 AND bind_status = 0 LIMIT 1")
    UserOauth findByUserIdAndProvider(@Param("userId") Integer userId,
                                      @Param("provider") String provider);

    /**
     * 解绑（逻辑删除）：仅解绑本用户本平台的有效绑定，避免误删他人同名平台绑定
     * @return 受影响行数（0=未找到可解绑记录）
     */
    @Update("UPDATE user_oauth SET is_deleted = 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND provider = #{provider} " +
            "AND is_deleted = 0 AND bind_status = 0")
    int unbind(@Param("userId") Integer userId, @Param("provider") String provider);
}
