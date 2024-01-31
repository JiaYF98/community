package com.nowcoder.community.service.impl;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.enumeration.TopicType;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.nowcoder.community.constant.RedisConstant.*;
import static com.nowcoder.community.constant.UserConstant.DELETED_HEADER_URL;
import static com.nowcoder.community.constant.UserConstant.DELETED_USERNAME;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventProducer eventProducer;

    // 某个用户关注的实体
    private String getFolloweeKey(String entityType, Integer userId) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体拥有的粉丝
    private String getFollowerKey(String entityType, Integer entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    @Override
    public void follow(String entityType, Integer entityId, Boolean isFollow) {
        Integer userId = hostHolder.getUser().getId();
        String followeeKey = getFolloweeKey(entityType, userId);
        String followerKey = getFollowerKey(entityType, entityId);

        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                ZSetOperations zSetOperations = operations.opsForZSet();

                operations.multi();
                long currentTimeMillis = System.currentTimeMillis();
                if (Boolean.TRUE.equals(isFollow)) {
                    zSetOperations.remove(followeeKey, entityId);
                    zSetOperations.remove(followerKey, userId);
                } else {
                    zSetOperations.add(followeeKey, entityId, currentTimeMillis);
                    zSetOperations.add(followerKey, userId, currentTimeMillis);
                }

                Event event = Event.builder()
                        .topic(TopicType.TOPIC_FOLLOW.getTypeName())
                        .userId(hostHolder.getUser().getId())
                        .entityType(EntityType.getNumberByName(entityType))
                        .entityId(entityId)
                        .entityUserId(entityId)
                        .build();
                eventProducer.fireEvent(event);

                return operations.exec();
            }
        });
    }

    @Override
    public Long getFolloweeUserCount(Integer userId) {
        String followeeKey = getFolloweeKey(EntityType.USER.getName(), userId);
        Long followeeUserCount = redisTemplate.opsForZSet().size(followeeKey);
        return followeeUserCount == null ? 0 : followeeUserCount;
    }

    @Override
    public Long getFollowerUserCount(Integer userId) {
        String followerKey = getFollowerKey(EntityType.USER.getName(), userId);
        Long followerUserCount = redisTemplate.opsForZSet().size(followerKey);
        return followerUserCount == null ? 0 : followerUserCount;
    }

    @Override
    public Boolean hasFollowed(Integer userId) {
        User user = hostHolder.getUser();
        if (user == null) {
            return Boolean.FALSE;
        }

        String followeeKey = getFolloweeKey(EntityType.USER.getName(), user.getId());
        return redisTemplate.opsForZSet().score(followeeKey, userId) != null;
    }

    @Override
    public List<Map<String, Object>> getFollowedUsers(String followedType, Integer userId, Integer offset, Integer limit) {
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        String followedKey;

        if (PREFIX_FOLLOWEE.equals(followedType)) {
            followedKey = getFolloweeKey(EntityType.USER.getName(), userId);
        } else {
            followedKey = getFollowerKey(EntityType.USER.getName(), userId);
        }

        Set<Object> ids = zSetOperations.range(followedKey, offset, offset + limit - 1);

        return Optional.ofNullable(ids).orElse(new HashSet<>()).stream()
                .map(id -> {
                    Map<String, Object> map = new HashMap<>();
                    User user = userMapper.selectById((Integer) id);
                    if (user != null) {
                        map.put("followedUser", user);
                    } else {
                        User deletedUser = new User();
                        deletedUser.setUsername(DELETED_USERNAME);
                        deletedUser.setHeaderUrl(DELETED_HEADER_URL);
                        map.put("followedUser", deletedUser);
                    }
                    map.put("followedTime",
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(Objects.requireNonNull(zSetOperations.score(followedKey, id)).longValue()),
                                    ZoneId.of("+8")));
                    map.put("hasFollowed", hasFollowed((Integer) id));
                    return map;
                })
                .toList();
    }
}
