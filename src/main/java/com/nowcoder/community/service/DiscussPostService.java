package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;

import java.util.List;

public interface DiscussPostService {
    List<DiscussPost> findDiscussPostsByUserId(Integer userId, Integer offset, Integer limit, Integer orderMode);

    Integer findDiscussPostRows(Integer userId);

    Integer addDiscussPost(String title, String content);

    Integer addDiscussPost(DiscussPost discussPost);

    DiscussPost findDiscussPostById(Integer id);

    Integer updateCommentCount(Integer id, Integer commentCount);

    void deleteRedisCache(Integer id);

    void setTop(Integer id);

    void setEssence(Integer id);

    void deletePost(Integer id);

    String getDiscussPostScoreKey();

    void saveToScoreKey(Integer discussPostId);

    void updateScore(Integer postId, Double score);
}
