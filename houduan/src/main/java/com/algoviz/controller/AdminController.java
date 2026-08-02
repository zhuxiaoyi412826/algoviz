package com.algoviz.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated 已拆分至多个独立控制器：
 * - {@link AdminAuthController}
 * - {@link AdminContentController}
 * - {@link AdminUserController}
 * - {@link AdminSystemController}
 * - {@link AdminExtensionController}
 * - {@link AdminPaymentController}
 * - {@link AdminFileController}
 * - {@link AdminMonitorController}
 * - {@link AdminExportController}
 */
@Deprecated
@RestController
@RequestMapping("/api")
public class AdminController {
}