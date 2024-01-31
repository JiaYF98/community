package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;

import java.util.List;

public interface SearchService {
    void saveDiscussPost(DiscussPost discussPost);

    void deleteDiscussPost(Integer id);

    List<DiscussPost> searchDiscussPosts(String keyword, Integer current, Integer limit);
}
