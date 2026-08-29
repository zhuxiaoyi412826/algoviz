package com.algoviz.know.qrdant.embedding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BERT WordPiece Tokenizer（bge-large-zh-v1.5 的 BertTokenizer 兼容实现）。
 * 读取 vocab.txt；中文按单字、英文/数字按词 + ## 子词切分。
 */
public class WordPieceTokenizer {

    public static final int PAD_ID = 0;
    public static final int UNK_ID = 100;
    public static final int CLS_ID = 101;
    public static final int SEP_ID = 102;

    private final Map<String, Integer> vocab;

    private WordPieceTokenizer(Map<String, Integer> vocab) {
        this.vocab = vocab;
    }

    /** 从 vocab.txt 加载 */
    public static WordPieceTokenizer fromFile(Path vocabFile) throws IOException {
        Map<String, Integer> vocab = new HashMap<>();
        try (InputStream in = Files.newInputStream(vocabFile);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int id = 0;
            while ((line = br.readLine()) != null) {
                String token = line.trim();
                if (!token.isEmpty()) {
                    vocab.put(token, id++);
                }
            }
        }
        return new WordPieceTokenizer(vocab);
    }

    /** 对整句做 Basic + WordPiece 切分，输出 token 列表（不含 [CLS]/[SEP]） */
    private List<String> splitToTokens(String text) {
        List<String> words = basicTokenize(text);
        List<String> tokens = new ArrayList<>();
        for (String w : words) {
            if (vocab.containsKey(w)) {
                tokens.add(w);
                continue;
            }
            // WordPiece：最长前缀匹配 + ## 子词
            boolean matched = false;
            String cur = w;
            while (!cur.isEmpty()) {
                String longest = null;
                for (int i = cur.length(); i > 0; i--) {
                    String candidate = i == cur.length() ? cur : "##" + cur.substring(0, i);
                    if (vocab.containsKey(candidate)) {
                        longest = candidate;
                        break;
                    }
                }
                if (longest == null) {
                    tokens.add("[UNK]");
                    matched = true;
                    break;
                }
                tokens.add(longest);
                cur = cur.substring(longest.startsWith("##") ? longest.length() - 2 : longest.length());
                matched = true;
            }
            if (!matched) {
                tokens.add("[UNK]");
            }
        }
        return tokens;
    }

    /**
     * BasicTokenizer：中文单字 / 英文数字按词 / 标点分隔。
     * bge-large-zh-v1.5 使用 do_lower_case=false，保留大小写。
     */
    private List<String> basicTokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                flushWord(out, word);
                out.add(String.valueOf(c));
            } else if (Character.isWhitespace(c)) {
                flushWord(out, word);
            } else if (isPunct(c)) {
                flushWord(out, word);
                out.add(String.valueOf(c));
            } else {
                word.append(c);
            }
        }
        flushWord(out, word);
        return out;
    }

    private void flushWord(List<String> out, StringBuilder word) {
        if (word.length() > 0) {
            out.add(word.toString());
            word.setLength(0);
        }
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)
                || (c >= 0x20000 && c <= 0x2A6DF) || (c >= 0x3000 && c <= 0x303F);
    }

    private static boolean isPunct(char c) {
        return !Character.isLetterOrDigit(c);
    }

    /**
     * 编码：返回 [inputIds, attentionMask, tokenTypeIds]（长度 = maxLen，含 [CLS]/[SEP]，不足补 [PAD]）。
     */
    public int[][] encode(String text, int maxLen) {
        int effectiveMax = Math.max(2, maxLen);
        List<String> tokens = splitToTokens(text);
        int realLen = Math.min(tokens.size(), effectiveMax - 2);

        int[] inputIds = new int[effectiveMax];
        int[] attentionMask = new int[effectiveMax];
        int[] tokenTypeIds = new int[effectiveMax];

        inputIds[0] = CLS_ID;
        attentionMask[0] = 1;
        for (int i = 0; i < realLen; i++) {
            int id = vocab.getOrDefault(tokens.get(i), UNK_ID);
            inputIds[i + 1] = id;
            attentionMask[i + 1] = 1;
        }
        inputIds[realLen + 1] = SEP_ID;
        attentionMask[realLen + 1] = 1;

        return new int[][]{inputIds, attentionMask, tokenTypeIds};
    }
}
