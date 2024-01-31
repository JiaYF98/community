package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> selectCommentsByEntity(Integer entityType, Integer entityId, Integer offset, Integer limit);

    Integer selectCountByEntity(Integer entityType, Integer entityId);

    Integer insertComment(Comment comment);

    Comment selectCommentById(Integer id);
}
