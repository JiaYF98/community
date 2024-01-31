package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {
    // 查询当前用户的会话列表，针对每个会话只返回一条最新的私信
    List<Message> selectConversations(Integer userId, Integer offset, Integer limit);

    // 查询当前用户的会话数量
    Integer selectConversationCount(Integer userId);

    // 查询某个会话所包含的私信列表
    List<Message> selectLetters(String conversationId, Integer offset, Integer limit);

    // 查询某个会话所包含的私信数量
    Integer selectLetterCount(String conversationId);

    // 查询未读私信的数量
    Integer selectLetterUnreadCount(Integer userId, String conversationId);

    // 插入私信
    Integer insertMessage(Message message);

    // 更新私信状态
    Integer updateMessageStatus(List<Integer> ids, Integer status);

    // 查询某个主题下最新的通知
    Message selectLatestNotice(Integer userId, String topic);

    // 查询某个主题所包含的通知数量
    Integer selectNoticeCount(Integer userId, String topic);

    // 查询未读通知的数量
    Integer selectNoticeUnreadCount(Integer userId, String topic);

    List<Message> selectNotices(Integer userId, String topic, Integer offset, Integer limit);
}
