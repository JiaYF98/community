package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nowcoder.community.constant.DiscussPostConstant.COMMENT_TYPE;
import static com.nowcoder.community.constant.DiscussPostConstant.REPLY_TYPE;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        if (StringUtils.isBlank(title)) {
            return CommunityUtil.getJSONString(403, "标题不能为空");
        }

        if (StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(403, "内容不能为空");
        }

        discussPostService.addDiscussPost(title, content);

        // 报错的情况,将来统一处理
        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @GetMapping("/detail/{id}")
    public String getDiscussDetailPage(@PathVariable("id") Integer id, Model model, Page page) {
        if (id == null) {
            return "redirect:/index";
        }

        DiscussPost discussPost = discussPostService.findDiscussPostById(id);
        if (discussPost == null) {
            model.addAttribute("discussPostMsg", "帖子不存在");
            return "redirect:/index";
        }

        // 设置回复分页信息
        page.setPath("/discuss/detail/" + id);
        page.setRows(discussPost.getCommentCount());
        page.setLimit(5);

        // 查询作者
        User user = userService.getUserById(discussPost.getUserId());

        // 查询回复和评论
        // List<Map<String, Object>> comments = commentService.getCommentsWithReplies(discussPost.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentsMapList = new ArrayList<>();
        commentService.selectCommentsByEntity(COMMENT_TYPE, discussPost.getId(), page.getOffset(), page.getLimit())
                .forEach(comment -> {
                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("comment", comment);
                    commentMap.put("commentAuthor", userService.getUserById(comment.getUserId()));
                    commentMap.put("commentLikeCount", likeService.findEntityLikeCount(EntityType.COMMENT.getName(), comment.getId()));
                    commentMap.put("commentLikeStatus", likeService.findEntityLikeStatus(EntityType.COMMENT.getName(), comment.getId()));

                    // 查询回复的评论
                    List<Map<String, Object>> repliesMapList = new ArrayList<>();
                    commentService.selectCommentsByEntity(REPLY_TYPE, comment.getId(), 0, Integer.MAX_VALUE)
                            .forEach(reply -> {
                                Map<String, Object> replyMap = new HashMap<>();
                                replyMap.put("reply", reply);
                                replyMap.put("replyAuthor", userService.getUserById(reply.getUserId()));
                                replyMap.put("replyTargetUser", userService.getUserById(reply.getTargetId()));
                                replyMap.put("replyLikeCount", likeService.findEntityLikeCount(EntityType.COMMENT.getName(), reply.getId()));
                                replyMap.put("replyLikeStatus", likeService.findEntityLikeStatus(EntityType.COMMENT.getName(), reply.getId()));
                                repliesMapList.add(replyMap);
                            });
                    commentMap.put("replies", repliesMapList);

                    commentsMapList.add(commentMap);
                });

        // 将信息添加到model中
        model.addAttribute("user", user);
        model.addAttribute("post", discussPost);
        model.addAttribute("postLikeCount", likeService.findEntityLikeCount(EntityType.POST.getName(), discussPost.getId()));
        model.addAttribute("postLikeStatus", likeService.findEntityLikeStatus(EntityType.POST.getName(), discussPost.getId()));
        model.addAttribute("comments", commentsMapList);

        return "/site/discuss-detail";
    }

    @ResponseBody
    @PostMapping("/top")
    public String setTop(Integer id) {
        discussPostService.setTop(id);
        return CommunityUtil.getJSONString(0);
    }

    @ResponseBody
    @PostMapping("/essence")
    public String setEssence(Integer id) {
        discussPostService.setEssence(id);
        return CommunityUtil.getJSONString(0);
    }

    @ResponseBody
    @PostMapping("/delete")
    public String deletePost(Integer id) {
        discussPostService.deletePost(id);
        return CommunityUtil.getJSONString(0);
    }
}
