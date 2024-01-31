package com.nowcoder.community.service;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;

import java.util.List;
import java.util.Map;

public interface MessageService {
    List<Map<String, Object>> findConversations(Integer offset, Integer limit);

    Integer findConversationCount();

    List<Map<String, Object>> findLetters(String conversationId, Integer offset, Integer limit);

    Integer findLetterCount(String conversationId);

    Integer findLetterUnreadCount(String conversationId);

    User getLetterTarget(String conversationId);

    String addMessage(String targetUsername, String content);

    void addSystemMessage(Message message);

    List<Map<String, Object>> findLatestNoticeList();

    Integer findNoticeCount(String topic);

    Integer findNoticeUnreadCount(String topic);

    List<Map<String, Object>> findNotices(String topic, Integer offset, Integer limit);
}
