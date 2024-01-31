package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.enumeration.EntityType;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.SearchService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static com.nowcoder.community.constant.DiscussPostConstant.ESSENCE_STATUS_DISCUSS_POST;

@Component
public class PostScoreRefreshJob implements Job {
    private final Logger log = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private SearchService searchService;

    // 牛客纪元
    private static final LocalDate epoch = LocalDate.of(2014, 8, 1);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String discussPostScoreKey = discussPostService.getDiscussPostScoreKey();
        BoundSetOperations<String, Object> setOperations = redisTemplate.boundSetOps(discussPostScoreKey);

        Set<Object> members = setOperations.members();
        if (members == null || members.isEmpty()) {
            log.info("[任务取消] 没有需要刷新的帖子");
            return;
        }

        log.info("[任务开始] 正在刷新帖子分数：" + members.size());
        setOperations.remove(members.toArray());
        members.forEach(member -> this.refresh((Integer) member));

        log.info("[任务结束] 帖子分数刷新完毕！");
    }

    private void refresh(Integer postId) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        if (discussPost == null) {
            log.error("该帖子不存在: id = " + postId);
            return;
        }

        // 是否精华
        boolean essence = ESSENCE_STATUS_DISCUSS_POST.equals(discussPost.getStatus());
        // 评论数量
        int commentCount = discussPost.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(EntityType.POST.getName(), postId);

        // 计算权重
        double w = (essence ? 75 : 0) + commentCount * 10L + likeCount * 2;
        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1)) + epoch.until(discussPost.getCreateTime(), ChronoUnit.DAYS);

        // 更新帖子分数
        discussPostService.updateScore(postId, score);

        // 同步搜索数据
        discussPost.setScore(score);
        searchService.saveDiscussPost(discussPost);
    }
}