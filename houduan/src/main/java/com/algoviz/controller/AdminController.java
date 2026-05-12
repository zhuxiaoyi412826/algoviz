package com.algoviz.controller;

import com.algoviz.dto.AdminLoginRequest;
import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.*;
import com.algoviz.mapper.*;
import com.algoviz.util.ExportUtil;
import com.algoviz.util.FileUploadUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AdminController {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private DataStructureMapper dataStructureMapper;

    @Autowired
    private AlgorithmMapper algorithmMapper;

    @Autowired
    private OJProblemMapper ojProblemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private AIPromptMapper aiPromptMapper;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private FileStorageMapper fileStorageMapper;

    @Autowired
    private ApiLogMapper apiLogMapper;

    // ==================== 登录模块 ====================
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@RequestBody AdminLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String device = httpRequest.getHeader("User-Agent");

        Admin admin = adminMapper.findByUsername(request.getUsername());
        if (admin == null) {
            // 记录登录失败日志
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId("unknown");
            failLog.setUsername(request.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("用户名不存在");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("用户名或密码错误");
        }

        // 简单密码校验（实际项目应使用BCrypt）
        if (!"admin123".equals(request.getPassword())) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(admin.getId());
            failLog.setUsername(admin.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("密码错误");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("用户名或密码错误");
        }

        if ("disabled".equals(admin.getStatus())) {
            LoginLog failLog = new LoginLog();
            failLog.setId(UUID.randomUUID().toString());
            failLog.setUserId(admin.getId());
            failLog.setUsername(admin.getUsername());
            failLog.setIp(ip);
            failLog.setDevice(device);
            failLog.setStatus("failed");
            failLog.setFailReason("账号已禁用");
            loginLogMapper.insert(failLog);
            return ApiResponse.error("账号已被禁用");
        }

        // 登录成功，记录日志
        adminMapper.updateLastLogin(admin.getId(), ip);
        LoginLog successLog = new LoginLog();
        successLog.setId(UUID.randomUUID().toString());
        successLog.setUserId(admin.getId());
        successLog.setUsername(admin.getUsername());
        successLog.setIp(ip);
        successLog.setDevice(device);
        successLog.setStatus("success");
        loginLogMapper.insert(successLog);

        // 生成token
        String token = UUID.randomUUID().toString();
        admin.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", admin);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/info")
    public ApiResponse<Admin> getAdminInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 简化处理，返回默认管理员信息
        Admin admin = adminMapper.findById("1");
        if (admin != null) {
            admin.setPassword(null);
        }
        return ApiResponse.success(admin);
    }

    @PostMapping("/admin/logout")
    public ApiResponse<Void> adminLogout() {
        return ApiResponse.success(null);
    }

    // ==================== 系统管理 - 管理员管理 ====================
    @GetMapping("/system/admin")
    public ApiResponse<Map<String, Object>> getAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Admin> list = adminMapper.findByPage(offset, pageSize);
        list.forEach(admin -> admin.setPassword(null));
        int total = adminMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/system/admin")
    public ApiResponse<Admin> createAdmin(@RequestBody Admin admin) {
        admin.setId(UUID.randomUUID().toString());
        admin.setPassword("admin123");
        admin.setStatus("active");
        adminMapper.insert(admin);

        Admin created = adminMapper.findById(admin.getId());
        if (created != null) {
            created.setPassword(null);
        }
        return ApiResponse.success(created);
    }

    @PutMapping("/system/admin/{id}")
    public ApiResponse<Admin> updateAdmin(@PathVariable String id, @RequestBody Admin admin) {
        admin.setId(id);
        adminMapper.update(admin);

        Admin updated = adminMapper.findById(id);
        if (updated != null) {
            updated.setPassword(null);
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/system/admin/{id}")
    public ApiResponse<Void> deleteAdmin(@PathVariable String id) {
        boolean success = adminMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @PostMapping("/system/admin/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable String id) {
        boolean success = adminMapper.updatePassword(id, "admin123") > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("重置密码失败");
    }

    @PutMapping("/system/admin/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        boolean success = adminMapper.updateStatus(id, body.get("status")) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("状态变更失败");
    }

    // ==================== 系统管理 - 登录日志 ====================
    @GetMapping("/system/login-log")
    public ApiResponse<Map<String, Object>> getLoginLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<LoginLog> list = loginLogMapper.findByPage(offset, pageSize);
        int total = loginLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/login-log/stats")
    public ApiResponse<Map<String, Object>> getLoginLogStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayCount", loginLogMapper.countToday());
        result.put("weekCount", loginLogMapper.countWeek());
        result.put("failCount", loginLogMapper.countFailed());
        return ApiResponse.success(result);
    }

    // ==================== 系统管理 - 操作日志 ====================
    @GetMapping("/system/operation-log")
    public ApiResponse<Map<String, Object>> getOperationLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OperationLog> list = operationLogMapper.findByPage(offset, pageSize);
        int total = operationLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/system/operation-log/{id}")
    public ApiResponse<OperationLog> getOperationLogDetail(@PathVariable String id) {
        return ApiResponse.success(operationLogMapper.findById(id));
    }

    // ==================== 系统管理 - 系统配置 ====================
    @GetMapping("/system/config")
    public ApiResponse<List<SystemConfig>> getSystemConfig() {
        return ApiResponse.success(systemConfigMapper.findAll());
    }

    @PutMapping("/system/config")
    public ApiResponse<Void> updateSystemConfig(@RequestBody Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            SystemConfig config = systemConfigMapper.findByKey(entry.getKey());
            if (config != null) {
                config.setValue(entry.getValue().toString());
                systemConfigMapper.insertOrUpdate(config);
            }
        }
        return ApiResponse.success(null);
    }

    // ==================== 内容管理 - 数据结构 ====================
    @GetMapping("/content/data-structure")
    public ApiResponse<Map<String, Object>> getDataStructureList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<DataStructure> list = dataStructureMapper.findByPage(offset, pageSize);
        int total = dataStructureMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/data-structure")
    public ApiResponse<DataStructure> createDataStructure(@RequestBody DataStructure dataStructure) {
        dataStructure.setId(UUID.randomUUID().toString());
        dataStructure.setStatus("enabled");
        dataStructureMapper.insert(dataStructure);
        return ApiResponse.success(dataStructureMapper.findById(dataStructure.getId()));
    }

    @PutMapping("/content/data-structure/{id}")
    public ApiResponse<DataStructure> updateDataStructure(@PathVariable String id, @RequestBody DataStructure dataStructure) {
        dataStructure.setId(id);
        dataStructureMapper.update(dataStructure);
        return ApiResponse.success(dataStructureMapper.findById(id));
    }

    @DeleteMapping("/content/data-structure/{id}")
    public ApiResponse<Void> deleteDataStructure(@PathVariable String id) {
        boolean success = dataStructureMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 内容管理 - 算法 ====================
    @GetMapping("/content/algorithm")
    public ApiResponse<Map<String, Object>> getAlgorithmList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Algorithm> list = algorithmMapper.findByPage(offset, pageSize);
        int total = algorithmMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/algorithm")
    public ApiResponse<Algorithm> createAlgorithm(@RequestBody Algorithm algorithm) {
        algorithm.setId(UUID.randomUUID().toString());
        algorithm.setStatus("enabled");
        algorithmMapper.insert(algorithm);
        return ApiResponse.success(algorithmMapper.findById(algorithm.getId()));
    }

    @PutMapping("/content/algorithm/{id}")
    public ApiResponse<Algorithm> updateAlgorithm(@PathVariable String id, @RequestBody Algorithm algorithm) {
        algorithm.setId(id);
        algorithmMapper.update(algorithm);
        return ApiResponse.success(algorithmMapper.findById(id));
    }

    @DeleteMapping("/content/algorithm/{id}")
    public ApiResponse<Void> deleteAlgorithm(@PathVariable String id) {
        boolean success = algorithmMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 内容管理 - OJ题目 ====================
    @GetMapping("/content/oj-problem")
    public ApiResponse<Map<String, Object>> getOJProblemList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OJProblem> list = ojProblemMapper.findByPage(offset, pageSize);
        int total = ojProblemMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/oj-problem")
    public ApiResponse<OJProblem> createOJProblem(@RequestBody OJProblem problem) {
        problem.setId(UUID.randomUUID().toString());
        problem.setStatus("online");
        problem.setSubmissionCount(0);
        problem.setAcRate(0.0);
        ojProblemMapper.insert(problem);
        return ApiResponse.success(ojProblemMapper.findById(problem.getId()));
    }

    @PutMapping("/content/oj-problem/{id}")
    public ApiResponse<OJProblem> updateOJProblem(@PathVariable String id, @RequestBody OJProblem problem) {
        problem.setId(id);
        ojProblemMapper.update(problem);
        return ApiResponse.success(ojProblemMapper.findById(id));
    }

    @DeleteMapping("/content/oj-problem/{id}")
    public ApiResponse<Void> deleteOJProblem(@PathVariable String id) {
        // 删除关联的测试用例
        testCaseMapper.deleteByProblemId(id);
        boolean success = ojProblemMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 内容管理 - 测试用例 ====================
    @GetMapping("/content/test-case/{problemId}")
    public ApiResponse<List<TestCase>> getTestCaseList(@PathVariable String problemId) {
        return ApiResponse.success(testCaseMapper.findByProblemId(problemId));
    }

    @PostMapping("/content/test-case")
    public ApiResponse<TestCase> createTestCase(@RequestBody TestCase testCase) {
        testCase.setId(UUID.randomUUID().toString());
        testCaseMapper.insert(testCase);
        return ApiResponse.success(testCaseMapper.findById(testCase.getId()));
    }

    @PutMapping("/content/test-case/{id}")
    public ApiResponse<TestCase> updateTestCase(@PathVariable String id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseMapper.update(testCase);
        return ApiResponse.success(testCaseMapper.findById(id));
    }

    @DeleteMapping("/content/test-case/{id}")
    public ApiResponse<Void> deleteTestCase(@PathVariable String id) {
        boolean success = testCaseMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 内容管理 - AI提示词 ====================
    @GetMapping("/content/ai-prompt")
    public ApiResponse<Map<String, Object>> getAIPromptList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AIPrompt> list = aiPromptMapper.findByPage(offset, pageSize);
        int total = aiPromptMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/content/ai-prompt")
    public ApiResponse<AIPrompt> createAIPrompt(@RequestBody AIPrompt prompt) {
        prompt.setId(UUID.randomUUID().toString());
        prompt.setUsageCount(0);
        prompt.setStatus("enabled");
        aiPromptMapper.insert(prompt);
        return ApiResponse.success(aiPromptMapper.findById(prompt.getId()));
    }

    @PutMapping("/content/ai-prompt/{id}")
    public ApiResponse<AIPrompt> updateAIPrompt(@PathVariable String id, @RequestBody AIPrompt prompt) {
        prompt.setId(id);
        aiPromptMapper.update(prompt);
        return ApiResponse.success(aiPromptMapper.findById(id));
    }

    @DeleteMapping("/content/ai-prompt/{id}")
    public ApiResponse<Void> deleteAIPrompt(@PathVariable String id) {
        boolean success = aiPromptMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 用户管理 ====================
    @GetMapping("/admin/user/list")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AppUser> list = appUserMapper.findByPage(offset, pageSize);
        int total = appUserMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/user/{id}")
    public ApiResponse<AppUser> getUserById(@PathVariable String id) {
        return ApiResponse.success(appUserMapper.findById(id));
    }

    @PutMapping("/admin/user/{id}")
    public ApiResponse<AppUser> updateUser(@PathVariable String id, @RequestBody AppUser user) {
        user.setId(id);
        appUserMapper.update(user);
        return ApiResponse.success(appUserMapper.findById(id));
    }
// 1
    @DeleteMapping("/admin/user/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        boolean success = appUserMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 数据统计 ====================
    @GetMapping("/statistics/trend")
    public ApiResponse<Map<String, Object>> getStatisticsTrend() {
        List<Statistics> stats = statisticsMapper.findRecent(30);
        Map<String, Object> result = new HashMap<>();
        result.put("dates", stats.stream().map(Statistics::getDate).toList());
        result.put("dau", stats.stream().map(Statistics::getDau).toList());
        result.put("dsVisits", stats.stream().map(Statistics::getDsVisits).toList());
        result.put("algoVisits", stats.stream().map(Statistics::getAlgoVisits).toList());
        result.put("ojSubmissions", stats.stream().map(Statistics::getOjSubmissions).toList());
        result.put("aiDialogues", stats.stream().map(Statistics::getAiDialogues).toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics/summary")
    public ApiResponse<Map<String, Object>> getStatisticsSummary() {
        Map<String, Object> result = new HashMap<>();
        result.put("dau", statisticsMapper.countToday());
        result.put("wau", statisticsMapper.sumDauWeek());
        result.put("mau", statisticsMapper.sumDauMonth());
        result.put("totalUsers", appUserMapper.count());
        result.put("totalSubmissions", 0);
        result.put("totalDialogues", 0);
        return ApiResponse.success(result);
    }

    // ==================== 运维监控 ====================
    @GetMapping("/monitor/service")
    public ApiResponse<List<Map<String, Object>>> getServiceStatus() {
        List<Map<String, Object>> services = List.of(
            Map.of("name", "后端服务", "status", "running", "cpu", 10, "memory", 256, "disk", 10, "uptime", "00:00:00"),
            Map.of("name", "前端服务", "status", "running", "cpu", 5, "memory", 128, "disk", 5, "uptime", "00:00:00"),
            Map.of("name", "数据库", "status", "running", "cpu", 2, "memory", 64, "disk", 20, "uptime", "00:00:00")
        );
        return ApiResponse.success(services);
    }

    @GetMapping("/monitor/alerts")
    public ApiResponse<List<Map<String, Object>>> getAlerts() {
        return ApiResponse.success(List.of());
    }

    // ==================== 扩展功能 - 公告 ====================
    @GetMapping("/extension/announcement")
    public ApiResponse<Map<String, Object>> getAnnouncementList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Announcement> list = announcementMapper.findByPage(offset, pageSize);
        int total = announcementMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/extension/announcement")
    public ApiResponse<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        announcement.setStatus("draft");
        announcementMapper.insert(announcement);
        return ApiResponse.success(announcement);
    }

    @PutMapping("/extension/announcement/{id}")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementMapper.update(announcement);
        return ApiResponse.success(announcementMapper.findById(String.valueOf(id)));
    }

    @DeleteMapping("/extension/announcement/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        boolean success = announcementMapper.deleteById(String.valueOf(id)) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 扩展功能 - 反馈 ====================
    @GetMapping("/extension/feedback")
    public ApiResponse<Map<String, Object>> getFeedbackList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Feedback> list = feedbackMapper.findByPage(offset, pageSize);
        int total = feedbackMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/extension/feedback/{id}")
    public ApiResponse<Feedback> getFeedbackById(@PathVariable String id) {
        return ApiResponse.success(feedbackMapper.findById(id));
    }

    @PutMapping("/extension/feedback/{id}")
    public ApiResponse<Feedback> updateFeedback(@PathVariable Long id, @RequestBody Feedback feedback) {
        feedback.setId(id);
        feedbackMapper.update(feedback);
        return ApiResponse.success(feedbackMapper.findById(String.valueOf(id)));
    }

    @DeleteMapping("/extension/feedback/{id}")
    public ApiResponse<Void> deleteFeedback(@PathVariable String id) {
        boolean success = feedbackMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    // ==================== 支付记录管理 ====================
    @GetMapping("/payment/records")
    public ApiResponse<Map<String, Object>> getPaymentRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PaymentRecord> list = paymentRecordMapper.findByPage(offset, pageSize);
        int total = paymentRecordMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/records/{id}")
    public ApiResponse<PaymentRecord> getPaymentRecord(@PathVariable String id) {
        return ApiResponse.success(paymentRecordMapper.findById(id));
    }

    @GetMapping("/payment/refunds")
    public ApiResponse<Map<String, Object>> getRefundRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PaymentRecord> list = paymentRecordMapper.findRefundedRecords(offset, pageSize);
        int total = paymentRecordMapper.countRefunded();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/stats")
    public ApiResponse<Map<String, Object>> getPaymentStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", paymentRecordMapper.sumTotalIncome());
        result.put("todayIncome", paymentRecordMapper.sumTodayIncome());
        result.put("weekIncome", paymentRecordMapper.sumWeekIncome());
        result.put("monthIncome", paymentRecordMapper.sumMonthIncome());
        result.put("totalRefund", paymentRecordMapper.sumTotalRefund());
        result.put("totalOrders", paymentRecordMapper.count());
        return ApiResponse.success(result);
    }

    @GetMapping("/payment/trend")
    public ApiResponse<List<PaymentTrend>> getPaymentTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(paymentRecordMapper.getIncomeTrend(days));
    }

    // ==================== 文件上传管理 ====================
    @PostMapping("/file/upload")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                       @RequestHeader(value = "X-User-Id", required = false) String userId) throws IOException {
        String filePath = FileUploadUtil.saveFile(file);
        String fileType = FileUploadUtil.getFileExtension(file.getOriginalFilename());
        
        FileStorage fileStorage = new FileStorage();
        fileStorage.setId(UUID.randomUUID().toString());
        fileStorage.setFileName(filePath.substring(filePath.lastIndexOf("/") + 1));
        fileStorage.setOriginalName(file.getOriginalFilename());
        fileStorage.setFileType(fileType);
        fileStorage.setFileSize(file.getSize());
        fileStorage.setFilePath(filePath);
        fileStorage.setStorageType("local");
        fileStorage.setUploaderId(userId != null ? userId : "admin");
        fileStorage.setUploaderName(userId != null ? "用户" : "管理员");
        fileStorage.setCreateTime(java.time.LocalDateTime.now().toString());
        
        fileStorageMapper.insert(fileStorage);

        Map<String, Object> result = new HashMap<>();
        result.put("id", fileStorage.getId());
        result.put("fileName", fileStorage.getFileName());
        result.put("downloadUrl", "/api/file/download/" + fileStorage.getId());
        return ApiResponse.success(result);
    }

    @GetMapping("/file/list")
    public ApiResponse<Map<String, Object>> getFileList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<FileStorage> list = fileStorageMapper.findByPage(offset, pageSize);
        int total = fileStorageMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/file/download/{id}")
    public void downloadFile(@PathVariable String id, HttpServletResponse response) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            java.io.File f = new java.io.File(file.getFilePath());
            if (f.exists()) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + 
                        new String(file.getOriginalName().getBytes("UTF-8"), "ISO-8859-1"));
                java.io.FileInputStream fis = new java.io.FileInputStream(f);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                fis.close();
            }
        }
    }

    @DeleteMapping("/file/{id}")
    public ApiResponse<Void> deleteFile(@PathVariable String id) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            FileUploadUtil.deleteFile(file.getFilePath());
        }
        boolean success = fileStorageMapper.delete(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/file/stats")
    public ApiResponse<Map<String, Object>> getFileStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", fileStorageMapper.count());
        result.put("totalSize", fileStorageMapper.sumFileSize());
        return ApiResponse.success(result);
    }

    // ==================== 数据导出 ====================
    @GetMapping("/export/users")
    public void exportUsers(@RequestParam(defaultValue = "excel") String format,
                            HttpServletResponse response) throws IOException {
        List<AppUser> users = appUserMapper.findAll();
        List<String> headers = List.of("ID", "OpenID", "昵称", "头像", "绑定时间", "状态");
        List<List<Object>> data = users.stream().map(u -> {
            List<Object> row = new java.util.ArrayList<>();
            row.add(u.getId());
            row.add(u.getOpenid());
            row.add(u.getNickname());
            row.add(u.getAvatar());
            row.add(u.getBindTime());
            row.add(u.getStatus());
            return row;
        }).toList();

        exportData(format, "users", headers, data, response);
    }

    @GetMapping("/export/logs")
    public void exportLogs(@RequestParam(defaultValue = "excel") String format,
                           HttpServletResponse response) throws IOException {
        List<LoginLog> logs = loginLogMapper.findAll();
        List<String> headers = List.of("ID", "用户ID", "用户名", "IP", "设备", "登录时间", "状态", "失败原因");
        List<List<Object>> data = logs.stream().map(l -> {
            List<Object> row = new java.util.ArrayList<>();
            row.add(l.getId());
            row.add(l.getUserId());
            row.add(l.getUsername());
            row.add(l.getIp());
            row.add(l.getDevice());
            row.add(l.getLoginTime());
            row.add(l.getStatus());
            row.add(l.getFailReason());
            return row;
        }).toList();

        exportData(format, "login_logs", headers, data, response);
    }

    private void exportData(String format, String filename, List<String> headers, 
                           List<List<Object>> data, HttpServletResponse response) throws IOException {
        byte[] content;
        String contentType;
        String extension;

        switch (format.toLowerCase()) {
            case "csv":
                content = ExportUtil.exportToCsv(headers, data);
                contentType = "text/csv";
                extension = ".csv";
                break;
            case "json":
                content = ExportUtil.exportToJson(headers, data);
                contentType = "application/json";
                extension = ".json";
                break;
            case "excel":
            default:
                content = ExportUtil.exportToExcel(headers, data);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
                break;
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" + filename + extension);
        response.getOutputStream().write(content);
    }

    // ==================== 接口性能监控 ====================
    @GetMapping("/monitor/api")
    public ApiResponse<Map<String, Object>> getApiStats(
            @RequestParam(defaultValue = "20") int limit) {
        List<ApiLogMapper.ApiStatistics> stats = apiLogMapper.getApiStatistics(limit);
        int todayRequests = apiLogMapper.countToday();
        Long avgResponseTime = apiLogMapper.avgResponseTimeToday();
        int errorCount = apiLogMapper.countErrorToday();

        Map<String, Object> result = new HashMap<>();
        result.put("apiList", stats);
        result.put("todayRequests", todayRequests);
        result.put("avgResponseTime", avgResponseTime);
        result.put("errorCount", errorCount);
        result.put("successRate", todayRequests > 0 ? 
                (todayRequests - errorCount) * 100.0 / todayRequests : 100.0);
        return ApiResponse.success(result);
    }

    @GetMapping("/monitor/api/hourly")
    public ApiResponse<List<ApiLogMapper.ApiHourlyStats>> getApiHourlyStats() {
        return ApiResponse.success(apiLogMapper.getHourlyStatsToday());
    }

    @GetMapping("/monitor/api/daily")
    public ApiResponse<List<ApiLogMapper.ApiDailyStats>> getApiDailyStats(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(apiLogMapper.getDailyStats(days));
    }

    @GetMapping("/monitor/api/logs")
    public ApiResponse<Map<String, Object>> getApiLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ApiLog> list = apiLogMapper.findByPage(offset, pageSize);
        int total = apiLogMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/monitor/api/logs/clean")
    public ApiResponse<Void> cleanOldApiLogs() {
        int deleted = apiLogMapper.cleanOldLogs();
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.success(null);
    }
}
