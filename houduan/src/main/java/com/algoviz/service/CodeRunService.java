package com.algoviz.service;

import java.util.Map;

public interface CodeRunService {
    
    /**
     * 运行代码
     * @param code 代码内容
     * @param language 编程语言 (java, python, cpp, javascript)
     * @param input 输入数据
     * @return 运行结果
     */
    Map<String, Object> runCode(String code, String language, String input);
    
    /**
     * 提交代码（判题）
     * @param code 代码内容
     * @param language 编程语言
     * @param problemNo 题目编号
     * @return 判题结果
     */
    Map<String, Object> submitCode(String code, String language, String problemNo);
}