package com.algoviz.service.impl;

import com.algoviz.entity.User;
import com.algoviz.mapper.UserMapper;
import com.algoviz.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.algoviz.mapper.UserVisitStatMapper userVisitStatMapper;

    @Autowired
    private com.algoviz.mapper.StatAccumulatorMapper statAccumulatorMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User findByUsernameIncludeDeleted(String username) {
        return userMapper.findByUsernameIncludeDeleted(username);
    }

    @Override
    public User findByEmailIncludeDeleted(String email) {
        return userMapper.findByEmailIncludeDeleted(email);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        if (user.getLoginStatus() == null) {
            user.setLoginStatus(1);   // 新用户默认离线
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setIsDeleted(0);   // 新用户正常，逻辑删除标记固定为 0
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl("https://i.pravatar.cc/150?u=" + System.currentTimeMillis());
        }
        userMapper.insert(user);
        // 访问统计拆表：注册时初始化 user_visit_stat 行（幂等兜底，正常由 upsert 自动建行）
        if (user.getId() != null) {
            userVisitStatMapper.initForUser(user.getId().longValue());
            // 写时累加：有效用户 +1（与回填口径一致：stat 行存在才算数，故放在 init 之后）
            statAccumulatorMapper.incTotalUsers();
        }
        return user;
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return user;
    }

    @Override
    public void updateProfile(Integer id, String nickname, String email, Integer gender, String avatarUrl) {
        userMapper.updateProfile(id, nickname, email, gender, avatarUrl);
    }

    @Override
    public void updateLastLogin(Integer userId) {
        userMapper.updateLastLoginAt(userId);
    }

    @Override
    public List<User> getAllUsers() {
        logger.info("获取所有用户");
        return userMapper.getAllUsers();
    }

    @Override
    public List<User> getUsersByPage(int page, int pageSize) {
        logger.info("分页获取用户 - page: {}, pageSize: {}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.getUsersByPage(offset, pageSize);
    }

    @Override
    public User findById(Integer id) {
        // 高频调用（AuthInterceptor 每个请求都实时校验账号状态），日志降为 debug 避免刷屏
        logger.debug("获取用户：{}", id);
        return userMapper.findById(id);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        logger.info("搜索用户：{}", keyword);
        return userMapper.searchUsers(keyword);
    }

    @Override
    public List<User> searchUsersByPage(String keyword, int page, int pageSize) {
        logger.info("分页搜索用户 - keyword: {}, page: {}, pageSize: {}", keyword, page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.searchUsersByPage(keyword, offset, pageSize);
    }

    @Override
    public int searchUsersCount(String keyword) {
        return userMapper.searchUsersCount(keyword);
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        logger.info("删除用户：{}", id);
        // 写时累加：先读取该用户 stat 行累计贡献，删除后再从 stat_total 逐项扣减
        com.algoviz.entity.UserVisitStat st = userVisitStatMapper.findByUserId(id.longValue());
        long ds = st != null && st.getDsVisits() != null ? st.getDsVisits().longValue() : 0L;
        long algo = st != null && st.getAlgoVisits() != null ? st.getAlgoVisits().longValue() : 0L;
        long oj = st != null && st.getOjVisits() != null ? st.getOjVisits().longValue() : 0L;
        long ai = st != null && st.getAiDialogues() != null ? st.getAiDialogues().longValue() : 0L;
        userMapper.deleteById(id);
        // 方案B：同步冗余到 stat 表，保证 dashboard 聚合 WHERE s.is_deleted=0 与 JOIN 语义一致
        userVisitStatMapper.markDeleted(id.longValue());
        // 有效用户 -1 且减去其历史累计贡献（与旧口径：已删除用户不计入 SUM）
        statAccumulatorMapper.decByUserDelete(ds, algo, oj, ai);
    }

    @Override
    public int countUsers() {
        return userMapper.countUsers();
    }

    @Override
    public List<User> getUsersByConditions(String keyword, Integer gender, Integer status, Integer loginStatus, String order, int page, int pageSize) {
        String safeOrder = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        int offset = (page - 1) * pageSize;
        return userMapper.getUsersByConditions(keyword, gender, status, loginStatus, safeOrder, offset, pageSize);
    }

    @Override
    public int getUsersCountByConditions(String keyword, Integer gender, Integer status, Integer loginStatus) {
        // 写时累加：四筛选全空（即裸 COUNT(*) user is_deleted=0）时读 stat_total.total_users，
        // 避免 94 万行全表 COUNT 每次列表加载 ~260ms；带筛选仍走 COUNT（无法预聚合）
        boolean bare = (keyword == null || keyword.trim().isEmpty())
                && gender == null && status == null && loginStatus == null;
        if (bare) {
            Long cached = statAccumulatorMapper.getTotalUsers();
            if (cached != null) {
                return cached.intValue();
            }
        }
        return userMapper.getUsersCountByConditions(keyword, gender, status, loginStatus);
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        userMapper.updateStatus(id, status);
    }

    @Override
    public int updatePassword(Integer id, String password) {
        return userMapper.updatePassword(id, password);
    }

    @Override
    public void updateLoginStatus(Integer id, Integer loginStatus) {
        userMapper.updateLoginStatus(id, loginStatus);
    }

    @Override
    public int cancelAccount(Integer id) {
        return userMapper.cancelAccount(id);
    }
}
