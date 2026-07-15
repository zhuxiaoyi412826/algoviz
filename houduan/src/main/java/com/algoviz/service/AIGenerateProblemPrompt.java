package com.algoviz.service;

/**
 * AI 生成题目专用提示词
 * 支持 4 种语言 + 3 种风格的组合
 *
 * @author AlgoViz Team
 */
public class AIGenerateProblemPrompt {

    /** 系统提示词：约束 AI 严格输出 JSON */
    public static final String SYSTEM_PROMPT =
        "你是一个资深的算法竞赛出题专家，专注于为 AlgoViz 在线判题平台出题。\n" +
        "你的输出必须严格遵守以下规则：\n" +
        "1. 严格使用 JSON 格式输出，不要包含任何 Markdown 代码块标记（如 ```json）。\n" +
        "2. 题目描述（description）使用 HTML 片段，可使用 <p>、<strong>、<code>、<ul>、<li>、<pre> 标签。\n" +
        "3. 数学公式可用 LaTeX 包裹在 $...$ 中。\n" +
        "4. 样例输入输出必须真实可执行，给出 1~2 组即可，不要超过 3 组。\n" +
        "5. 模板代码（template）必须能直接被用户提交，包含必要的 import 和主类/方法签名。\n" +
        "6. 标签（tags）从给定的候选集合中选取最相关的 1~4 个，用英文逗号分隔。\n" +
        "7. 难度必须严格匹配用户指定的 easy/medium/hhard 值。\n" +
        "8. 题目内容必须原创、严谨、避免歧义。\n" +
        "9. 输出根 JSON 必须是 {\"problems\": [...]} 形式。";

    /**
     * 用户提示词模板（占位符：{language} {difficulty} {style} {knowledgePoints} {count} {additionalRequirements}）
     */
    public static final String USER_PROMPT_TEMPLATE =
        "请为我生成 {count} 道 {difficulty} 难度的算法题目，要求如下：\n" +
        "\n" +
        "【知识点 / 标签】\n" +
        "{knowledgePoints}\n" +
        "\n" +
        "【语言 / 题型】\n" +
        "{language}\n" +
        "\n" +
        "【题目风格】\n" +
        "{style}\n" +
        "\n" +
        "{additionalRequirementsSection}\n" +
        "\n" +
        "【输出要求】\n" +
        "1. 每道题的 problemNo 推荐一个 4 位数字（按 1xxx=简单 / 2xxx=中等 / 3xxx=困难 区间分配），但允许冲突，后续会手动调整。\n" +
        "2. description 中明确给出：问题描述、约束条件（如数据范围）、示例说明。\n" +
        "3. inputFormat / outputFormat 用纯文本描述输入输出格式。\n" +
        "4. sampleInput / sampleOutput 中只放数据，不要包含「输入：」「输出：」前缀。\n" +
        "5. hint 字段给一段解题思路提示（不直接给完整代码）。\n" +
        "6. tags 字段使用中文标签，多个标签用英文逗号分隔。\n" +
        "7. template 字段给出用户可以直接提交的完整代码框架（包含类名、方法签名、必要 import、注释提示「在此处编写你的代码」）。\n" +
        "\n" +
        "【严格 JSON 输出格式】\n" +
        "{\n" +
        "  \"problems\": [\n" +
        "    {\n" +
        "      \"problemNo\": \"2001\",\n" +
        "      \"title\": \"买卖股票最佳时机\",\n" +
        "      \"difficulty\": \"medium\",\n" +
        "      \"tags\": \"数组,动态规划\",\n" +
        "      \"description\": \"<p>给定一个数组 <code>prices</code>...</p>\",\n" +
        "      \"inputFormat\": \"第一行...\",\n" +
        "      \"outputFormat\": \"输出最大利润\",\n" +
        "      \"sampleInput\": \"7\\n1 5 3 6 4\",\n" +
        "      \"sampleOutput\": \"5\",\n" +
        "      \"hint\": \"维护一个变量记录历史最低价...\",\n" +
        "      \"template\": \"class Solution {\\n    public int maxProfit(int[] prices) {\\n        // 在此处编写你的代码\\n        return 0;\\n    }\\n}\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    /**
     * 语言描述映射
     */
    public static String getLanguageDescription(String language) {
        if (language == null) return "通用编程题（不限定语言）";
        switch (language.toLowerCase()) {
            case "java":        return "Java 专项题（模板用 class Solution 形式，含 import java.util.*）";
            case "python":      return "Python 专项题（模板用 class Solution: def method(self, ...)）";
            case "cpp":
            case "c++":         return "C++ 专项题（模板用 class Solution { public: ... }; 含 #include <bits/stdc++.h>）";
            case "javascript":
            case "js":          return "JavaScript 专项题（模板用 class Solution { method(...) {} }）";
            case "general":
            default:            return "通用编程题（不限定语言，模板用最常见的 class Solution）";
        }
    }

    /**
     * 风格描述映射
     */
    public static String getStyleDescription(String style) {
        if (style == null) return "常规题（直接考察算法/数据结构）";
        switch (style.toLowerCase()) {
            case "variant":     return "变式题（在经典题目基础上做小改动，如改条件、改返回值、加入特殊数据）";
            case "scenario":    return "场景应用题（结合实际业务场景，如电商、社交、金融、游戏等，给出真实背景故事）";
            case "standard":
            default:            return "常规题（直接考察算法/数据结构）";
        }
    }

    /**
     * 难度描述映射
     */
    public static String getDifficultyDescription(String difficulty) {
        if (difficulty == null) return "中等";
        switch (difficulty.toLowerCase()) {
            case "easy":   return "简单（直接应用基础数据结构，5-10 行代码）";
            case "hard":   return "困难（需要高级算法或综合运用，30+ 行代码）";
            case "medium":
            default:       return "中等（需要一定的算法设计，15-25 行代码）";
        }
    }

    /**
     * 默认代码模板（当 AI 未返回或返回空时兜底）
     */
    public static String defaultTemplate(String language) {
        if (language == null) language = "general";
        switch (language.toLowerCase()) {
            case "java":
                return "import java.util.*;\n\nclass Solution {\n    public void solve() {\n        // 在此处编写你的代码\n    }\n}\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 读取输入... \n        Solution sol = new Solution();\n        sol.solve();\n    }\n}";
            case "python":
                return "from typing import List\n\nclass Solution:\n    def solve(self) -> None:\n        # 在此处编写你的代码\n        pass\n\nif __name__ == '__main__':\n    sol = Solution()\n    sol.solve()";
            case "cpp":
            case "c++":
                return "#include <bits/stdc++.h>\nusing namespace std;\n\nclass Solution {\npublic:\n    void solve() {\n        // 在此处编写你的代码\n    }\n};\n\nint main() {\n    Solution sol;\n    sol.solve();\n    return 0;\n}";
            case "javascript":
            case "js":
                return "const readline = require('readline');\nconst rl = readline.createInterface({ input: process.stdin });\n\nclass Solution {\n    solve() {\n        // 在此处编写你的代码\n    }\n}\n\nconst sol = new Solution();\nsol.solve();";
            default:
                return "class Solution {\n    // 在此处编写你的代码\n}";
        }
    }
}
