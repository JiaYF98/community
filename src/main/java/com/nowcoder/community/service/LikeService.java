package com.nowcoder.community.service;

public interface LikeService {
    void like(String entityType, Integer entityId, Integer entityUserId, Integer postId);

    Long findEntityLikeCount(String entityType, Integer entityId);

    Integer findEntityLikeStatus(String entityType, Integer entityId);

    Integer getUserLikeCount(Integer userId);
}
