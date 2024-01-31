package com.nowcoder.community.service;

import com.nowcoder.community.entity.Comment;

import java.util.List;

public interface CommentService {
    List<Comment> selectCommentsByEntity(Integer entityType, Integer entityId, Integer offset, Integer limit);

    Integer selectCountByEntity(Integer entityType, Integer entityId);

    Integer addComment(Integer discussPostId, Comment comment);

    Comment findCommentById(Integer entityId);
}
