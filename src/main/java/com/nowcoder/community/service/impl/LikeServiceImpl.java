package com.nowcoder.community.service.impl;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nowcoder.community.constant.KafkaConstant.TOPIC_LIKE;
import static com.nowcoder.community.constant.LikeConstant.LIKED;
import static com.nowcoder.community.constant.LikeConstant.NOT_LIKED;
import static com.nowcoder.community.constant.RedisConstant.*;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    private String getEntityLikeKey(String entityType, Integer entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    private String getUserLikeKey(Integer userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    @Override
    public void like(String entityType, Integer entityId, Integer entityUserId, Integer postId) {
        String entityLikeKey = getEntityLikeKey(entityType, entityId);
        String userLikeKey = getUserLikeKey(entityUserId);
        Integer userId = hostHolder.getUser().getId();

        redisTemplate.execute(new SessionCallback<>() {
            public List<Object> execute(@NonNull RedisOperations operations) throws DataAccessException {
                SetOperations setOperations = operations.opsForSet();
                ValueOperations valueOperations = operations.opsForValue();
                Boolean isMember = setOperations.isMember(entityLikeKey, userId);

                // 开启事务
                operations.multi();

                // 事务开启过程中，查询结果返回值为null
                if (Boolean.TRUE.equals(isMember)) {
                    setOperations.remove(entityLikeKey, userId);
                    valueOperations.decrement(userLikeKey);
                } else {
                    setOperations.add(entityLikeKey, userId);
                    valueOperations.increment(userLikeKey);
                }

                // 如果是点赞并且不是给自己点赞则发送系统消息
                if (Boolean.FALSE.equals(isMember) && !entityUserId.equals(userId)) {
                    Event event = Event.builder()
                            .topic(TOPIC_LIKE)
                            .userId(userId)
                            .entityType(EntityType.getNumberByName(entityType))
                            .entityId(entityId)
                            .entityUserId(entityUserId)
                            .build();
                    Map<String, Object> data = new HashMap<>();
                    data.put("postId", postId);
                    event.setData(data);
                    eventProducer.fireEvent(event);
                }

                // 如果点赞的是帖子，则存入变更帖子id中
                if (EntityType.POST.getName().equals(entityType)) {
                    discussPostService.saveToScoreKey(entityId);
                }

                return operations.exec();
            }
        });
    }

    @Override
    public Long findEntityLikeCount(String entityType, Integer entityId) {
        return redisTemplate.opsForSet().size(getEntityLikeKey(entityType, entityId));
    }

    @Override
    public Integer findEntityLikeStatus(String entityType, Integer entityId) {
        return hostHolder.getUser() == null ||
                Boolean.FALSE.equals(redisTemplate.opsForSet().isMember(getEntityLikeKey(entityType, entityId),
                        hostHolder.getUser().getId())) ? NOT_LIKED : LIKED;
    }

    @Override
    public Integer getUserLikeCount(Integer userId) {
        Integer userLikeCount = (Integer) redisTemplate.opsForValue().get(getUserLikeKey(userId));
        return userLikeCount == null ? 0 : userLikeCount;
    }
}
