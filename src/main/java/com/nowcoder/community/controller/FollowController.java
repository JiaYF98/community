package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.nowcoder.community.constant.RedisConstant.PREFIX_FOLLOWEE;
import static com.nowcoder.community.constant.RedisConstant.PREFIX_FOLLOWER;

@Controller
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @PostMapping("/user")
    @ResponseBody
    public String follow(String entityType, Integer entityId, Boolean isFollow) {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId不能为空！");
        }

        followService.follow(entityType, entityId, isFollow);
        return CommunityUtil.getJSONString(0);
    }

    @GetMapping("/followee/{userId}")
    public String getFolloweePage(@PathVariable("userId") Integer userId, Page page, Model model) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在！");
        }

        // 设置分页信息
        page.setPath("/follow/followee/" + userId);
        page.setRows(followService.getFolloweeUserCount(userId).intValue());
        page.setLimit(5);

        // 查询关注的人的信息
        List<Map<String, Object>> followeeUsers = followService.getFollowedUsers(PREFIX_FOLLOWEE, userId, page.getOffset(), page.getLimit());
        model.addAttribute("followeeUsers", followeeUsers);
        model.addAttribute("user", user);

        return "/site/followee";
    }

    @GetMapping("/follower/{userId}")
    public String getFollowerPage(@PathVariable("userId") Integer userId, Page page, Model model) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在！");
        }

        // 设置分页信息
        page.setPath("/follow/follower/" + userId);
        page.setRows(followService.getFollowerUserCount(userId).intValue());
        page.setLimit(5);

        // 查询粉丝信息
        List<Map<String, Object>> followerUsers = followService.getFollowedUsers(PREFIX_FOLLOWER, userId, page.getOffset(), page.getLimit());
        model.addAttribute("followerUsers", followerUsers);
        model.addAttribute("user", user);

        return "/site/follower";
    }
}
