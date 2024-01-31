package com.nowcoder.community.service.impl;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.enumeration.TopicType;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.mapper.CommentMapper;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.SensitiveFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nowcoder.community.constant.DiscussPostConstant.COMMENT_TYPE;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private EventProducer eventProducer;

    @Override
    public List<Comment> selectCommentsByEntity(Integer entityType, Integer entityId, Integer offset, Integer limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    @Override
    public Integer selectCountByEntity(Integer entityType, Integer entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer addComment(Integer discussPostId, Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数错误");
        }

        // 判断评论内容是否为空
        String content = comment.getContent();
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("回复内容不能为空");
        }

        Integer userId = hostHolder.getUser().getId();
        Integer entityType = comment.getEntityType();
        Integer entityId = comment.getEntityId();

        // 处理评论内容
        content = HtmlUtils.htmlEscape(content);
        content = sensitiveFilter.filter(content);

        // 设置参数
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setStatus(0);
        comment.setCreateTime(LocalDateTime.now());

        // 添加评论
        Integer rows = commentMapper.insertComment(comment);

        // 更新帖子评论数量
        if (entityType.equals(COMMENT_TYPE)) {
            discussPostService.updateCommentCount(comment.getId(), selectCountByEntity(entityType, entityId));
        }

        Integer entityUserId = null;
        // 如果目标实体类型是帖子，则entityUser为帖子的所有者
        if (entityType.equals(EntityType.POST.getNumber())) {
            // todo 可以考虑AOP实现
            discussPostService.deleteRedisCache(discussPostId);
            discussPostService.saveToScoreKey(discussPostId);
            DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
            entityUserId = discussPost.getUserId();
        } else if (entityType.equals(EntityType.COMMENT.getNumber())) {
            // 如果目标实体类是回复，则entityUser为回复的所有者
            entityUserId = comment.getTargetId();
        }

        // 如果是自己给自己发的评论，则不需要系统通知
        if (userId.equals(entityUserId)) {
            return rows;
        }

        // 系统消息
        Event event = Event.builder()
                .topic(TopicType.TOPIC_COMMENT.getTypeName())
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .entityUserId(entityUserId)
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("postId", discussPostId);
        event.setData(data);
        eventProducer.fireEvent(event);


        return rows;
    }

    @Override
    public Comment findCommentById(Integer id) {
        return commentMapper.selectCommentById(id);
    }
}
