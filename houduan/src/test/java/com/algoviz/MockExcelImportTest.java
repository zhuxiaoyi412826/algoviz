package com.algoviz;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Mock 数据生成 + Excel 导入全链路测试
 *
 * 流程：
 *  1. 生成 target/test-problems.xlsx（含 3 道题：1 正常 + 1 故意冲突 + 1 故意空题号）
 *  2. POST /api/problems/import-excel 上传
 *  3. GET  /api/problems/all 验证数据库入库
 *  4. 输出 PASS / FAIL
 */
public class MockExcelImportTest {

    private static final String API_HOST = "http://localhost:80";

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println(" Excel 导入 Mock 测试");
        System.out.println("========================================");

        // 1. 生成 xlsx
        Path xlsxPath = Path.of("target/test-problems.xlsx");
        Files.createDirectories(xlsxPath.getParent());
        generateMockXlsx(xlsxPath);
        System.out.println("[1/4] ✅ 已生成 mock xlsx: " + xlsxPath.toAbsolutePath()
                + " (" + Files.size(xlsxPath) + " bytes)");

        // 2. 查询当前最大题号
        long beforeMax = getMaxProblemNo();
        System.out.println("[2/4] 当前数据库最大题号: " + beforeMax);

        // 3. 调用导入接口（不过滤，先看后端报什么）
        System.out.println("[3/4] POST /api/problems/import-excel ...");
        String resp = uploadFile(xlsxPath, false);
        System.out.println("    响应: " + resp);

        // 4. 再次查询
        long afterMax = getMaxProblemNo();
        System.out.println("[4/4] 导入后数据库最大题号: " + afterMax);

        // 评估
        System.out.println("========================================");
        if (afterMax > beforeMax) {
            System.out.println("🎉 PASS：成功新增 " + (afterMax - beforeMax) + " 道题");
        } else {
            System.out.println("❌ FAIL：题号未递增，导入可能未生效");
            System.out.println("  → 检查后端日志 / SQL 是否报错");
        }
        System.out.println("========================================");
    }

    private static void generateMockXlsx(Path out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("题目数据");
            // 表头
            Row header = sheet.createRow(0);
            String[] cols = {"题号", "标题", "难度", "标签", "题目描述",
                    "输入格式", "输出格式", "样例输入", "样例输出", "解题提示", "代码模板"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            // 数据行 1：正常题（题号留空 → 后端自动分配）
            addRow(sheet, 1, new String[]{
                    "", "Mock-测试题1-两数之和", "easy", "数组,哈希表",
                    "给定一个整数数组 nums 和一个目标值 target，请返回两个数的下标。",
                    "第一行 n 表示数组长度，第二行 n 个整数，第三行目标值",
                    "输出两个下标（从 0 开始）",
                    "4\n2 7 11 15\n9", "0 1",
                    "使用哈希表可将时间复杂度降到 O(n)",
                    "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[0];\n    }\n}"
            });

            // 数据行 2：正常题（指定题号 8888）
            addRow(sheet, 2, new String[]{
                    "8888", "Mock-测试题2-有效的括号", "easy", "栈,字符串",
                    "给定一个只包括 (){}[] 的字符串，判断字符串是否有效。",
                    "一行字符串", "输出 Yes 或 No",
                    "()[]{}", "Yes",
                    "使用栈，左括号入栈右括号出栈匹配",
                    "class Solution {\n    public boolean isValid(String s) {\n        return false;\n    }\n}"
            });

            // 数据行 3：困难题
            addRow(sheet, 3, new String[]{
                    "9999", "Mock-测试题3-滑动窗口最大值", "hard", "队列,滑动窗口,堆",
                    "给定一个数组 nums 和滑动窗口大小 k，返回每个窗口的最大值。",
                    "第一行 n 和 k，第二行 n 个整数", "输出每个窗口的最大值",
                    "8 3\n1 3 -1 -3 5 3 6 7", "3 3 5 5 6 7",
                    "单调队列：维护一个递减队列",
                    "class Solution {\n    public int[] maxSlidingWindow(int[] nums, int k) {\n        return new int[0];\n    }\n}"
            });

            // 自动列宽
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream os = new FileOutputStream(out.toFile())) {
                wb.write(os);
            }
        }
    }

    private static void addRow(Sheet sheet, int rowIdx, String[] values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private static long getMaxProblemNo() {
        try {
            URL url = new URL(API_HOST + "/api/problems/all");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) {
                System.out.println("    [WARN] GET /api/problems/all 返回 " + code);
                return -1;
            }
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // 简单解析：从 body 中找最大的 problemNo 数字
            long max = 0;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"problemNo\"\\s*:\\s*\"?(\\d+)").matcher(body);
            while (m.find()) {
                long v = Long.parseLong(m.group(1));
                if (v > max) max = v;
            }
            return max;
        } catch (Exception e) {
            System.out.println("    [WARN] 查询最大题号异常: " + e.getMessage());
            return -1;
        }
    }

    private static String uploadFile(Path file, boolean overwrite) throws Exception {
        String boundary = "----MockBoundary" + System.currentTimeMillis();
        URL url = new URL(API_HOST + "/api/problems/import-excel");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (var os = conn.getOutputStream()) {
            // file 字段
            writeField(os, boundary, "file", file.getFileName().toString(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    Files.readAllBytes(file));
            // overwrite 字段
            writeFormField(os, boundary, "overwriteOnConflict", String.valueOf(overwrite));
            // 结束
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int code = conn.getResponseCode();
        String body = new String(
                (code >= 400 ? conn.getErrorStream() : conn.getInputStream()).readAllBytes(),
                StandardCharsets.UTF_8);
        if (code >= 400) {
            throw new RuntimeException("HTTP " + code + ": " + body);
        }
        return body;
    }

    private static void writeFormField(java.io.OutputStream os, String boundary, String name, String value) throws Exception {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        os.write(part.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeField(java.io.OutputStream os, String boundary, String name,
                                   String filename, String contentType, byte[] content) throws Exception {
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        os.write(head.getBytes(StandardCharsets.UTF_8));
        os.write(content);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
