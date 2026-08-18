package com.algoviz.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DFA 前缀树（敏感词多模式匹配）
 * 一次构建，多线程只读匹配，O(n) 扫描全文。
 */
public class DfaTrie {

    private static class Node {
        boolean end;
        Map<Character, Node> next = new HashMap<>();
    }

    private final Node root = new Node();
    private int size;

    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        Node cur = root;
        for (int i = 0; i < word.length(); i++) {
            cur = cur.next.computeIfAbsent(word.charAt(i), k -> new Node());
        }
        if (!cur.end) {
            cur.end = true;
            size++;
        }
    }

    public int size() {
        return size;
    }

    /** 命中词与起始位置（同一词多处出现只报一次） */
    public List<Hit> matchAll(String text) {
        List<Hit> hits = new ArrayList<>();
        if (text == null || text.isEmpty() || size == 0) return hits;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < text.length(); i++) {
            Node cur = root;
            int j = i;
            while (j < text.length()) {
                Node nxt = cur.next.get(text.charAt(j));
                if (nxt == null) break;
                cur = nxt;
                j++;
                if (cur.end) {
                    String w = text.substring(i, j);
                    if (seen.add(w)) {
                        hits.add(new Hit(w, i));
                    }
                }
            }
        }
        return hits;
    }

    public record Hit(String word, int start) {}
}
