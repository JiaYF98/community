package com.nowcoder.community.controller;

import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController {

    @Autowired
    private LikeService likeService;

    @PostMapping("/like")
    @ResponseBody
    public String Like(String entityType, Integer entityId, Integer entityUserId, Integer postId) {
        likeService.like(entityType, entityId, entityUserId, postId);
        Long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        Integer likeStatus = likeService.findEntityLikeStatus(entityType, entityId);
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        return CommunityUtil.getJSONString(0, null, map);
    }
}
