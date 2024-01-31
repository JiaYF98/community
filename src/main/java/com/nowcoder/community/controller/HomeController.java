package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nowcoder.community.constant.UserConstant.DEFAULT_USER_ID;

@Controller
public class HomeController {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @GetMapping("/")
    public String root() {
        return "forward:/index";
    }

    @GetMapping("/index")
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") Integer orderMode) {
        // todo 将查询的帖子放入redis中缓存
        // 方法调用前，SpringMVC会自动实例化Model和Page，并将Page注入Model
        // 所以，在thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.findDiscussPostRows(DEFAULT_USER_ID));
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> discussPosts =
                discussPostService.findDiscussPostsByUserId(DEFAULT_USER_ID, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> newDiscussPosts = new ArrayList<>();
        discussPosts.forEach(discussPost -> {
            Map<String, Object> map = new HashMap<>();
            map.put("post", discussPost);
            map.put("user", userService.getUserById(discussPost.getUserId()));
            map.put("likeCount", likeService.findEntityLikeCount(EntityType.POST.getName(), discussPost.getId()));
            newDiscussPosts.add(map);
        });

        model.addAttribute("discussPosts", newDiscussPosts);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }
}
