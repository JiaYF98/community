package com.nowcoder.community.service;

import java.util.List;
import java.util.Map;

public interface FollowService {
    void follow(String entityType, Integer entityId, Boolean isFollow);

    Long getFolloweeUserCount(Integer userId);

    Long getFollowerUserCount(Integer userId);

    Boolean hasFollowed(Integer userId);

    List<Map<String, Object>> getFollowedUsers(String followedType, Integer userId, Integer offset, Integer limit);
}
