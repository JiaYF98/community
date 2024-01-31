package com.nowcoder.community.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.MessageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.nowcoder.community.constant.KafkaConstant.*;
import static com.nowcoder.community.constant.MessageConstant.SYSTEM_USER_ID;
import static com.nowcoder.community.constant.MessageConstant.UNREAD_MESSAGE;

@Component
public class EventConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord<String, Object> record) throws JsonProcessingException {
        Event event = getEvent(record);

        if (event == null) return;

        // todo 应当判断是否已经存在该消息
        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        Map<String, Object> data = event.getData();
        if (data != null) {
            content.putAll(data);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        message.setContent(objectMapper.writeValueAsString(content));
        message.setStatus(UNREAD_MESSAGE);
        message.setCreateTime(LocalDateTime.now());

        messageService.addSystemMessage(message);
    }

    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord<String, Object> record) throws JsonProcessingException {
        Event event = getEvent(record);

        if (event == null) return;

        // todo 通知关注的人发帖消息

        // 将发布的帖子保存到es中
        Integer discussPostId = event.getEntityId();
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        elasticsearchTemplate.save(discussPost);
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord<String, Object> record) throws JsonProcessingException {
        Event event = getEvent(record);

        if (event == null) return;

        Integer discussPostId = event.getEntityId();
        elasticsearchTemplate.delete(discussPostId.toString(), DiscussPost.class);
    }

    private Event getEvent(ConsumerRecord<String, Object> record) throws JsonProcessingException {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空！");
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Event event = objectMapper.readValue(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误！");
        }
        return event;
    }
}
