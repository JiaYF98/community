package com.nowcoder.community.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.enumeration.TopicType;
import com.nowcoder.community.mapper.MessageMapper;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.nowcoder.community.constant.MessageConstant.READ_MESSAGE;
import static com.nowcoder.community.constant.MessageConstant.UNREAD_MESSAGE;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Override
    public List<Map<String, Object>> findConversations(Integer offset, Integer limit) {
        User user = hostHolder.getUser();
        List<Map<String, Object>> conversationsMapList = new ArrayList<>();

        List<Message> conversations = messageMapper.selectConversations(user.getId(), offset, limit);
        if (conversations != null) {
            conversations.forEach(conversation -> {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", conversation);
                map.put("letterCount", messageMapper.selectLetterCount(conversation.getConversationId()));
                // 本条会话未读消息数量
                map.put("unreadCount", messageMapper.selectLetterUnreadCount(user.getId(), conversation.getConversationId()));
                Integer targetId = user.getId().equals(conversation.getToId()) ? conversation.getFromId() : user.getId();
                map.put("target", userService.getUserById(targetId));
                conversationsMapList.add(map);
            });
        }

        return conversationsMapList;
    }

    @Override
    public Integer findConversationCount() {
        return messageMapper.selectConversationCount(hostHolder.getUser().getId());
    }

    @Override
    public List<Map<String, Object>> findLetters(String conversationId, Integer offset, Integer limit) {
        // 私信列表
        List<Map<String, Object>> lettersMapList = new ArrayList<>();

        List<Message> letters = messageMapper.selectLetters(conversationId, offset, limit);
        if (letters != null) {
            letters.forEach(letter -> {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", letter);
                map.put("fromUser", userMapper.selectById(letter.getFromId()));
                lettersMapList.add(map);
            });

            // 设置已读
            List<Integer> ids = letters.stream().filter(message -> message.getStatus().equals(UNREAD_MESSAGE))
                    .map(Message::getId).toList();

            if (!ids.isEmpty()) {
                messageMapper.updateMessageStatus(ids, READ_MESSAGE);
            }
        }


        return lettersMapList;
    }

    @Override
    public Integer findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    @Override
    public Integer findLetterUnreadCount(String conversationId) {
        return messageMapper.selectLetterUnreadCount(hostHolder.getUser().getId(), conversationId);
    }

    @Override
    public User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        return hostHolder.getUser().getId().equals(id0) ? userMapper.selectById(id1) : userMapper.selectById(id0);
    }

    @Override
    public String addMessage(String targetUsername, String content) {
        User targetUser = userMapper.selectByName(targetUsername);
        if (targetUser == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在！");
        }

        Integer fromId = hostHolder.getUser().getId();
        Integer toId = targetUser.getId();
        String conversationId = fromId < toId ? fromId + "_" + toId : toId + "_" + fromId;
        content = HtmlUtils.htmlEscape(content);
        content = sensitiveFilter.filter(content);

        Message message = new Message();
        message.setFromId(fromId);
        message.setToId(toId);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setStatus(UNREAD_MESSAGE);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insertMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    @Override
    public void addSystemMessage(Message message) {
        messageMapper.insertMessage(message);
    }

    @Override
    public List<Map<String, Object>> findLatestNoticeList() {
        Integer userId = hostHolder.getUser().getId();
        List<Message> latestNoticeList = new ArrayList<>();

        // 查找每个主题最新一条消息，如果不为空则添加到latestNoticeList
        Arrays.stream(TopicType.values()).forEach(topics -> {
                    Message message = messageMapper.selectLatestNotice(userId, topics.getTypeName());
                    if (message != null) {
                        latestNoticeList.add(message);
                    }
                }
        );

        // 查询其它页面需要的数据
        List<Map<String, Object>> noticesMapList = new ArrayList<>();
        latestNoticeList.forEach(latestNotice -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> data = objectMapper.readValue(latestNotice.getContent(), new TypeReference<>() {
                });
                Map<String, Object> notice = new HashMap<>(data);
                String topic = latestNotice.getConversationId();
                notice.put("topic", topic);
                notice.put("message", latestNotice);
                notice.put("user", userMapper.selectById((Integer) data.get("userId")));
                notice.put("count", messageMapper.selectNoticeCount(userId, topic));
                notice.put("unreadCount", messageMapper.selectNoticeUnreadCount(userId, topic));
                noticesMapList.add(notice);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return noticesMapList;
    }

    @Override
    public Integer findNoticeCount(String topic) {
        Integer userId = hostHolder.getUser().getId();
        return messageMapper.selectNoticeCount(userId, topic);
    }

    @Override
    public Integer findNoticeUnreadCount(String topic) {
        Integer userId = hostHolder.getUser().getId();
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    @Transactional
    @Override
    public List<Map<String, Object>> findNotices(String topic, Integer offset, Integer limit) {
        List<Map<String, Object>> noticesMapList = new ArrayList<>();

        Integer userId = hostHolder.getUser().getId();
        List<Message> messages = messageMapper.selectNotices(userId, topic, offset, limit);

        // 查询页面所需要的信息
        Optional.ofNullable(messages).orElse(new ArrayList<>())
                .forEach(message -> {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> data = objectMapper.readValue(message.getContent(), new TypeReference<>() {
                        });
                        Map<String, Object> notice = new HashMap<>(data);
                        // todo 处理注销的账号
                        User user = userMapper.selectById((Integer) data.get("userId"));
                        if (user == null) {
                            user = new User();
                            user.setUsername("账户已注销");
                            user.setHeaderUrl("");
                        }

                        User fromUser = userMapper.selectById(message.getFromId());

                        notice.put("user", user);
                        notice.put("fromUser", fromUser);
                        notice.put("message", message);
                        noticesMapList.add(notice);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        // 标记已读
        List<Integer> ids = Optional.ofNullable(messages).orElse(new ArrayList<>())
                .stream()
                .filter(message -> message.getStatus().equals(UNREAD_MESSAGE))
                .map(Message::getId)
                .toList();

        if (!ids.isEmpty()) {
            messageMapper.updateMessageStatus(ids, READ_MESSAGE);
        }

        return noticesMapList;
    }

}
