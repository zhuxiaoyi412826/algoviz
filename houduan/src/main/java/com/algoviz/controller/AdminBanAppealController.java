package com.algoviz.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.BanAppeal;
import com.algoviz.entity.User;
import com.algoviz.entity.rbac.SysUser;
import com.algoviz.mapper.BanAppealMapper;
import com.algoviz.mapper.rbac.SysUserMapper;
import com.algoviz.service.EmailService;
import com.algoviz.service.LoginLockService;
import com.algoviz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封禁申诉管理（后台）。
 *
 * <p>路径挂在 /api/admin/** 下，由 Sa-Token 统一做管理员登录校验（见 SaTokenConfig），
 * 且已被 WebConfig 排除前台 AuthInterceptor，不需要额外的鉴权注解。</p>
 *
 * <p>处理动作：approve（通过）→ 自动把用户 status 置回 1 解封，并清除该账号的登录失败锁定，
 * 使用户能立刻登录；reject（驳回）→ 只记录处理结果。两种结果都会邮件通知申诉人。</p>
 */
@RestController
@RequestMapping("/api/admin/ban-appeals")
@Tag(name = "封禁申诉管理", description = "后台查看与处理封禁申诉")
public class AdminBanAppealController {

    private static final Logger logger = LoggerFactory.getLogger(AdminBanAppealController.class);

    /** 处理回复长度上限 */
    private static final int REPLY_MAX_LENGTH = 500;
    /** 正常账号状态 */
    private static final int USER_STATUS_NORMAL = 1;
    /** 封禁账号状态 */
    private static final int USER_STATUS_BANNED = 0;

    @Autowired
    private BanAppealMapper banAppealMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LoginLockService loginLockService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    @Operation(summary = "查询申诉列表", description = "分页查询封禁申诉，支持按状态筛选与账号/邮箱模糊搜索，待处理置顶")
    public ApiResponse<Map<String, Object>> getAppealList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        int offset = (page - 1) * pageSize;
        List<BanAppeal> list = banAppealMapper.findByPage(status, keyword, offset, pageSize);
        int total = banAppealMapper.countByConditions(status, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询申诉详情", description = "根据ID查询申诉全文、处理结果与处理人")
    public ApiResponse<BanAppeal> getAppealById(@PathVariable Long id) {
        BanAppeal appeal = banAppealMapper.findById(id);
        if (appeal == null) {
            return ApiResponse.error(404, "申诉记录不存在");
        }
        return ApiResponse.success(appeal);
    }

    @PutMapping("/{id}")
    @Operation(summary = "处理申诉", description = "approve=通过并自动解封，reject=驳回；处理结果会邮件通知申诉人")
    public ApiResponse<BanAppeal> handleAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body.get("action") == null ? "" : body.get("action").trim();
        String reply = body.get("reply") == null ? "" : body.get("reply").trim();

        boolean approve;
        if ("approve".equalsIgnoreCase(action)) {
            approve = true;
        } else if ("reject".equalsIgnoreCase(action)) {
            approve = false;
        } else {
            return ApiResponse.error(400, "处理动作无效，只能为 approve（通过）或 reject（驳回）");
        }

        if (reply.length() > REPLY_MAX_LENGTH) {
            return ApiResponse.error(400, "处理回复不能超过 " + REPLY_MAX_LENGTH + " 个字");
        }

        BanAppeal appeal = banAppealMapper.findById(id);
        if (appeal == null) {
            return ApiResponse.error(404, "申诉记录不存在");
        }
        if (!BanAppeal.STATUS_PENDING.equals(appeal.getStatus())) {
            return ApiResponse.error(409, "该申诉已处理过，不能重复处理");
        }

        // 通过：解封账号
        String unbanNote = "";
        if (approve) {
            User user = appeal.getUserId() == null ? null : userService.findById(appeal.getUserId().intValue());
            if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
                return ApiResponse.error(400, "该申诉对应的用户不存在或已被删除，无法解封，请直接驳回");
            }
            if (user.getStatus() != null && user.getStatus() == USER_STATUS_BANNED) {
                userService.updateStatus(user.getId(), USER_STATUS_NORMAL);
                // 封禁期间累积的登录失败锁定一并清除，否则解封后仍会被锁定拦截
                loginLockService.reset(LoginLockService.LoginLockType.USER, appeal.getUsername());
                logger.info("封禁申诉通过并解封: appealId={}, userId={}, username={}",
                        id, user.getId(), user.getUsername());
            } else {
                unbanNote = "（该账号当前并非封禁状态，未做解封操作）";
                logger.info("封禁申诉通过，但账号已非封禁状态: appealId={}, userId={}, status={}",
                        id, user.getId(), user.getStatus());
            }
        }

        Long handlerId = StpUtil.getLoginIdAsLong();
        String handlerName = resolveAdminName(handlerId);
        String finalStatus = approve ? BanAppeal.STATUS_APPROVED : BanAppeal.STATUS_REJECTED;
        banAppealMapper.handle(id, finalStatus, reply.isEmpty() ? null : reply, handlerId, handlerName);

        notifyAppellant(appeal, approve, reply);

        ApiResponse<BanAppeal> response = ApiResponse.success(banAppealMapper.findById(id));
        response.setMessage(approve ? "已通过申诉并解封该账号" + unbanNote : "已驳回该申诉");
        return response;
    }

    /** 处理人展示名：优先真实姓名，其次后台登录账号 */
    private String resolveAdminName(Long handlerId) {
        SysUser admin = sysUserMapper.findById(handlerId);
        if (admin == null) {
            return "";
        }
        String realName = admin.getRealName() == null ? "" : admin.getRealName().trim();
        return realName.isEmpty() ? admin.getUsername() : realName;
    }

    /** 处理结果邮件通知申诉人（发送失败不影响处理结果，仅记日志） */
    private void notifyAppellant(BanAppeal appeal, boolean approve, String reply) {
        String email = appeal.getEmail() == null ? "" : appeal.getEmail().trim();
        if (email.isEmpty()) {
            return;
        }
        StringBuilder content = new StringBuilder();
        content.append("你的封禁申诉已处理完成。\n\n")
                .append("账号：").append(appeal.getUsername()).append('\n')
                .append("处理结果：").append(approve ? "申诉通过，账号已解封" : "申诉驳回").append('\n');
        if (!reply.isEmpty()) {
            content.append("管理员回复：").append(reply).append('\n');
        }
        content.append('\n');
        if (approve) {
            content.append("你现在可以使用原密码登录，请遵守平台使用规范。\n");
        } else {
            content.append("如对处理结果有疑问，可再次提交申诉或联系平台客服。\n");
        }
        emailService.sendNotificationEmail(email, "封禁申诉处理结果", content.toString());
    }
}