package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.SearchService;
import com.nowcoder.community.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @GetMapping
    public String search(String keyword, Page page, Model model) {
        if (StringUtils.isBlank(keyword)) {
            return "redirect:/index";
        }

        List<DiscussPost> searchResult = searchService.searchDiscussPosts(keyword, page.getCurrent() - 1, page.getLimit());

        List<Map<String, Object>> discussPostMapList = new ArrayList<>();
        searchResult.forEach(discussPost -> {
            User discussPostAuthor = userService.getUserById(discussPost.getUserId());
            Long discussPostLikeCount = likeService.findEntityLikeCount(EntityType.POST.getName(), discussPost.getId());

            Map<String, Object> discussPostMap = new HashMap<>();
            discussPostMap.put("discussPost", discussPost);
            discussPostMap.put("discussPostAuthor", discussPostAuthor);
            discussPostMap.put("discussPostLikeCount", discussPostLikeCount);

            discussPostMapList.add(discussPostMap);
        });

        model.addAttribute("discussPostMapList", discussPostMapList);
        model.addAttribute("keyword", keyword);

        page.setRows(searchResult.size());
        page.setPath("/search?keyword=" + keyword);

        return "/site/search";
    }

}
