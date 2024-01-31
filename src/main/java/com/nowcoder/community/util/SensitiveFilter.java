package com.nowcoder.community.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger log = LoggerFactory.getLogger(SensitiveFilter.class);

    private static final String REPLACEMENT = "****";

    private final TireNode rootNode = new TireNode();

    private static class TireNode {
        private boolean isKeywordEnd = false;

        private final Map<Character, TireNode> subNodes = new HashMap<>();

        private void setIsKeywordEnd() {
            this.isKeywordEnd = true;
        }

        private boolean getIsKeywordEnd() {
            return this.isKeywordEnd;
        }

        private void addSubNode(Character c) {
            if (!subNodes.containsKey(c)) {
                subNodes.put(c, new TireNode());
            }
        }

        private TireNode getSubNode(Character c) {
            return subNodes.get(c);
        }

        private static void addKeyword(TireNode rootNode, String keyword) {
            TireNode tempNode = rootNode;
            for (int i = 0; i < keyword.length(); i++) {
                char c = keyword.charAt(i);
                tempNode.addSubNode(c);
                tempNode = tempNode.getSubNode(c);
            }

            tempNode.setIsKeywordEnd();
        }
    }

    @PostConstruct
    private void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))

        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                TireNode.addKeyword(rootNode, keyword);
            }
        } catch (IOException e) {
            log.error("加载敏感词文件失败！" + e.getMessage());
        }
    }

    public String filter(String text) {
        StringBuilder sb = new StringBuilder();

        TireNode indexNode = rootNode;

        int start = 0;
        int end = 0;

        while (end < text.length()) {
            char c = text.charAt(end);
            // 跳过符号
            if (isSymbol(c)) {
                if (indexNode == rootNode) {
                    sb.append(c);
                    start++;
                }
                end++;
                continue;
            }

            TireNode subNode = indexNode.getSubNode(c);

            // 如果subNode是敏感词的结束位置，则加入替换符
            if (subNode != null && subNode.getIsKeywordEnd()) {
                sb.append(REPLACEMENT);
                start = ++end;
            } else {
                // 如果不是敏感词
                if (subNode == null) {
                    sb.append(text.charAt(start));
                    end = ++start;
                } else {
                    // 继续判断下一个字符是不是敏感词
                    indexNode = subNode;
                    end++;
                    continue;
                }
            }
            indexNode = rootNode;
        }

        if (start < text.length()) {
            sb.append(text.substring(start));
        }

        return sb.toString();
    }

    // 判断是否是符号
    private boolean isSymbol(Character character) {
        return !CharUtils.isAsciiAlphanumeric(character) && (character < 0x2E80 || character > 0x9FFF);
    }
}
