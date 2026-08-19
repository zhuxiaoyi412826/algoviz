package com.algoviz.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DFA 前缀树（敏感词多模式匹配）
 * 一次构建，多线程只读匹配。
 *
 * 模糊匹配 / 变体对抗（核心思路：遇干扰字符不回退 root、不消耗 trie 状态）：
 *   1. 插入时从词中剥离「噪声字符」（标点 / 符号 / 空白 / Emoji / 控制字符），保持 trie 只存有效字符。
 *   2. 匹配时遍历原始文本（不修改原串）：
 *        - root 下的首字符必须非噪声（交给外层 i 推进）；
 *        - 进入某前缀（cur != root）后遇到噪声，直接跳过：j++、cur 不变、不回退 root；
 *        - 连续噪声超过 MAX_CONSECUTIVE_NOISE 则终止该前缀（防止 "赌 ...1000 字...博" 的误命中）；
 *        - 遇到有效字符按 DFA 常规推进，命中 end 即记录。
 *   3. 命中的 Hit.start 始终是原始文本里匹配段的首个字符下标（保留原始下标）。
 *   4. Hit.word 是干净词形（噪声已被剥离），保证与 wordMeta 中 clean 键对齐。
 *
 * 说明：当前全局 FUZZY；若后续要严格按敏感词的 match_mode=EXACT/FUZZY 区分，需拆分两个 DfaTrie 实例：
 *   exactTrie  —— 建 trie 时不剥噪声、匹配时不跳噪声；
 *   fuzzyTrie  —— 按当前行为；
 *   matchAll 跑两份合并并在 Hit 上附带 match_mode 来源即可。
 */
public class DfaTrie {

    /** 噪声字符：字母 / 数字（含全角）/ CJK 统一表意文字以外的字符 */
    public static final int MAX_CONSECUTIVE_NOISE = 3;

    private static class Node {
        boolean end;
        Map<Character, Node> next = new HashMap<>();
    }

    private final Node root = new Node();
    private int size;

    // ================ 构建 ================

    /** 插入词（按当前 FUZZY 策略：先剥离噪声后入 trie；剥完为空则忽略） */
    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        String clean = stripNoise(word);
        if (clean.isEmpty()) return;
        Node cur = root;
        for (int i = 0; i < clean.length(); i++) {
            cur = cur.next.computeIfAbsent(clean.charAt(i), k -> new Node());
        }
        if (!cur.end) {
            cur.end = true;
            size++;
        }
    }

    public int size() {
        return size;
    }

    // ================ 匹配 ================

    /**
     * @return 命中列表；同一词多次出现只报一次（seen 去重）。
     *         Hit.start 是原文本里匹配前缀首字符的下标；Hit.word 是干净词形。
     */
    public List<Hit> matchAll(String text) {
        List<Hit> hits = new ArrayList<>();
        if (text == null || text.isEmpty() || size == 0) return hits;
        java.util.Set<String> seen = new java.util.HashSet<>();
        final int n = text.length();
        for (int i = 0; i < n; i++) {
            Node cur = root;
            int j = i;
            // 累积匹配到的有效字符，构成命中时的干净词典词（直接用于 wordMeta 查找）
            StringBuilder matched = new StringBuilder();
            int noiseRun = 0;
            while (j < n) {
                char ch = text.charAt(j);
                if (cur != root && isNoise(ch)) {
                    // 前缀中途遇噪声：跳过，不回退 root、不消耗 trie 状态、不写 matched
                    noiseRun++;
                    j++;
                    if (noiseRun > MAX_CONSECUTIVE_NOISE) break;
                    continue;
                }
                // root 下首轮若 ch 是噪声则会立即 break，交给外层 i 推进
                Node nxt = cur.next.get(ch);
                if (nxt == null) break;
                cur = nxt;
                matched.append(ch);
                noiseRun = 0;
                j++;
                if (cur.end) {
                    String cleanWord = matched.toString();
                    if (seen.add(cleanWord)) {
                        hits.add(new Hit(cleanWord, i));
                    }
                }
            }
        }
        return hits;
    }

    public record Hit(String word, int start) {}

    // ================ 工具：噪声判定 ================

    /** 字母 / 数字（含全角）/ CJK 汉字 → 非噪声；其余（标点/符号/空白/Emoji/控制符）→ 噪声 */
    public static boolean isNoise(char c) {
        return !Character.isLetterOrDigit(c);
    }

    /** 从字符串中剥离噪声字符（建 trie / wordMeta clean 键 两处使用，保持语义一致） */
    public static String stripNoise(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isNoise(c)) sb.append(c);
        }
        return sb.toString();
    }
}
