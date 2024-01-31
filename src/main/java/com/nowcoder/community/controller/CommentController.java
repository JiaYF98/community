package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/add/{id}")
    public String addComment(@PathVariable("id") Integer discussPostId, Comment comment) {
        if (discussPostId == null) {
            throw new IllegalArgumentException("参数错误");
        }

        commentService.addComment(discussPostId, comment);

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
