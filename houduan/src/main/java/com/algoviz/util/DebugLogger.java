package com.algoviz.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 调试日志工具类
 * 同时输出到控制台和日志文件
 * 
 * 注意：Windows 环境下控制台使用 GBK 编码，文件使用 UTF-8 编码
 */
public class DebugLogger {
    
    private static final String LOG_DIR = "D:/rizi";
    private static final String LOG_FILE = LOG_DIR + "/DEBUG.txt";
    private static final Charset CONSOLE_CHARSET = Charset.forName("GBK");  // Windows 控制台编码
    private static final Charset FILE_CHARSET = Charset.forName("UTF-8");   // 文件编码
    private static PrintStream fileStream = null;
    private static PrintStream consoleStream = null;
    private static boolean initialized = false;
    
    /**
     * 初始化日志系统（应用启动时调用）
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        
        try {
            // 确保日志目录存在
            Path dirPath = Paths.get(LOG_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("[DebugLogger] 创建日志目录: " + LOG_DIR);
            }
            
            // 清空并创建日志文件
            Path filePath = Paths.get(LOG_FILE);
            if (Files.exists(filePath)) {
                Files.write(filePath, new byte[0]);  // 清空文件
                System.out.println("[DebugLogger] 清空日志文件: " + LOG_FILE);
            }
            
            // 创建新的文件输出流（UTF-8）
            fileStream = new PrintStream(new FileOutputStream(LOG_FILE, false), true, FILE_CHARSET);
            
            // 创建控制台输出流（GBK，适配 Windows 控制台）
            consoleStream = new PrintStream(System.out, true, CONSOLE_CHARSET);
            
            initialized = true;
            
            log("========================================");
            log("调试日志系统启动");
            log("启动时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            log("日志文件: " + LOG_FILE);
            log("控制台编码: " + CONSOLE_CHARSET.name());
            log("文件编码: " + FILE_CHARSET.name());
            log("========================================");
            
        } catch (Exception e) {
            System.err.println("[DebugLogger] 初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭日志系统（应用关闭时调用）
     */
    public static synchronized void close() {
        if (fileStream != null || consoleStream != null) {
            try {
                log("========================================");
                log("调试日志系统关闭");
                log("关闭时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                log("========================================");
                if (fileStream != null) {
                    fileStream.close();
                    fileStream = null;
                }
                if (consoleStream != null) {
                    consoleStream.close();
                    consoleStream = null;
                }
                initialized = false;
                System.out.println("[DebugLogger] 日志系统已关闭");
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 打印调试日志
     */
    public static void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String logLine = "[" + timestamp + "] " + message;
        
        // 输出到控制台（使用 GBK 编码适配 Windows）
        if (consoleStream != null) {
            consoleStream.println(logLine);
        } else {
            System.out.println(logLine);
        }
        
        // 输出到文件（使用 UTF-8 编码）
        if (fileStream != null) {
            fileStream.println(logLine);
            fileStream.flush();
        }
    }
    
    /**
     * 打印带标签的调试日志
     */
    public static void log(String tag, String message) {
        log("[" + tag + "] " + message);
    }
    
    /**
     * 打印分隔线
     */
    public static void separator() {
        log("----------------------------------------");
    }
    
    /**
     * 打印空行
     */
    public static void newline() {
        log("");
    }
}
