package com.nowcoder.community.service.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.enumeration.TopicType;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.SensitiveFilter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.nowcoder.community.constant.DiscussPostConstant.*;
import static com.nowcoder.community.constant.RedisConstant.PREFIX_POST;
import static com.nowcoder.community.constant.RedisConstant.SPLIT;
import static com.nowcoder.community.constant.UserConstant.DEFAULT_USER_ID;

@Service
public class DiscussPostServiceImpl implements DiscussPostService {

    private static final Logger log = LoggerFactory.getLogger(DiscussPostServiceImpl.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${caffeine.post.initial-capacity}")
    private Integer initialCapacity;

    @Value("${caffeine.post.max-size}")
    private Integer maxSize;

    @Value("${caffeine.post.expire-second}")
    private Long expireSecond;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        postListCache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maxSize)
                .expireAfterWrite(expireSecond, TimeUnit.SECONDS)
                .build(key -> {
                    // 二级缓存：redis -> mysql
                    // todo 计算完热度之后，将热帖放入，热帖最多展示1000个
                    List<Object> rawPosts = redisTemplate.opsForList().range(key, 0, -1);
                    assert rawPosts != null;
                    if (!rawPosts.isEmpty()) {
                        return rawPosts.stream().map(post -> (DiscussPost) post).toList();
                    }

                    String[] params = key.split(":");
                    int offset = Integer.parseInt(params[0]);
                    int limit = Integer.parseInt(params[1]);

                    log.debug("load post list from DB");
                    List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPostsByUserId(DEFAULT_USER_ID, offset, limit, ORDER_MODE_HOT);
                    redisTemplate.opsForList().rightPushAll(key, discussPosts.toArray());
                    redisTemplate.expire(key, 180, TimeUnit.SECONDS);
                    return discussPosts;
                });

        postRowsCache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maxSize)
                .expireAfterWrite(expireSecond, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("load post list from DB");
                    return discussPostMapper.selectDiscussPostRows(key);
                });
    }

    private String getDiscussPostKey(Integer discussPostId) {
        return PREFIX_POST + SPLIT + discussPostId;
    }

    @Override
    public void deleteRedisCache(Integer id) {
        // redis中删除缓存
        redisTemplate.delete(getDiscussPostKey(id));
    }

    @Override
    public List<DiscussPost> findDiscussPostsByUserId(Integer userId, Integer offset, Integer limit, Integer orderMode) {
        if (userId.equals(DEFAULT_USER_ID) && orderMode.equals(ORDER_MODE_HOT)) {
            log.debug("load post from cache");

            // 从caffeine中读取缓存，如果caffeine中没有，则调用CacheLoader.load方法获取值返回，并将查询结果存入caffeine缓存中
            return postListCache.get(offset + ":" + limit);
        }

        // todo 将数据存入redis中
        log.debug("load post list from DB");
        return discussPostMapper.selectDiscussPostsByUserId(userId, offset, limit, orderMode);
    }

    @Override
    public Integer findDiscussPostRows(Integer userId) {
        if (userId.equals(DEFAULT_USER_ID)) {
            log.debug("load post from cache");
            return postRowsCache.get(userId);
        }

        log.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    @Override
    public Integer addDiscussPost(String title, String content) {
        // 转义HTML标记
        title = HtmlUtils.htmlEscape(title);
        content = HtmlUtils.htmlEscape(content);

        // 过滤敏感词
        title = sensitiveFilter.filter(title);
        content = sensitiveFilter.filter(content);

        Integer userId = hostHolder.getUser().getId();
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(userId);
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(LocalDateTime.now());

        Integer rows = discussPostMapper.insertDiscussPost(discussPost);

        // 帖子发布的消息
        Event event = Event.builder()
                .topic(TopicType.TOPIC_PUBLISH.getTypeName())
                .userId(userId)
                .entityType(EntityType.POST.getNumber())
                .entityId(discussPost.getId())
                .build();
        eventProducer.fireEvent(event);

        // 将帖子存入redis
        redisTemplate.opsForValue().set(
                getDiscussPostKey(discussPost.getId()),
                discussPost,
                QUERY_CACHE_POST_EXPIRE_SECOND,
                TimeUnit.SECONDS
        );

        // 将变更的帖子Id存入redis
        saveToScoreKey(discussPost.getId());

        return rows;
    }

    // todo 注释
    @Override
    public Integer addDiscussPost(DiscussPost discussPost) {
        return discussPostMapper.insertDiscussPost(discussPost);
    }

    @Override
    public DiscussPost findDiscussPostById(Integer id) {
        DiscussPost discussPost = discussPostMapper.selectDiscussPostById(id);
        // 将查询的discussPost存入redis中
        redisTemplate.opsForValue().set(
                getDiscussPostKey(discussPost.getId()),
                discussPost,
                QUERY_CACHE_POST_EXPIRE_SECOND,
                TimeUnit.SECONDS
        );
        return discussPost;
    }

    @Override
    public Integer updateCommentCount(Integer id, Integer commentCount) {
        Integer row = discussPostMapper.updateCommentCount(id, commentCount);
        // 删除redis中缓存的数据
        deleteRedisCache(id);
        saveToScoreKey(id);
        return row;
    }

    @Override
    public void setTop(Integer id) {
        discussPostMapper.updateType(id, TOP_TYPE_DISCUSS_POST);

        // 发布事件
        Event event = Event.builder()
                .topic(TopicType.TOPIC_PUBLISH.getTypeName())
                .entityType(EntityType.POST.getNumber())
                .entityId(id)
                .build();
        eventProducer.fireEvent(event);

        deleteRedisCache(id);
        saveToScoreKey(id);
    }

    @Override
    public void setEssence(Integer id) {
        discussPostMapper.updateStatus(id, ESSENCE_STATUS_DISCUSS_POST);

        Event event = Event.builder()
                .topic(TopicType.TOPIC_PUBLISH.getTypeName())
                .entityType(EntityType.POST.getNumber())
                .entityId(id)
                .build();
        eventProducer.fireEvent(event);

        deleteRedisCache(id);
        saveToScoreKey(id);
    }

    @Override
    public void deletePost(Integer id) {
        discussPostMapper.updateStatus(id, BLOCK_STATUS_DISCUSS_POST);

        Event event = Event.builder()
                .topic(TopicType.TOPIC_DELETE.getTypeName())
                .entityType(EntityType.POST.getNumber())
                .entityId(id)
                .build();
        eventProducer.fireEvent(event);

        deleteRedisCache(id);
        saveToScoreKey(id);
    }

    @Override
    public String getDiscussPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }

    // 将变更的帖子的Id存入redis，以便计算帖子的分数
    @Override
    public void saveToScoreKey(Integer discussPostId) {
        redisTemplate.opsForSet().add(getDiscussPostScoreKey(), discussPostId);
    }

    @Override
    public void updateScore(Integer postId, Double score) {
        discussPostMapper.updateScore(postId, score);
        deleteRedisCache(postId);
    }
}
