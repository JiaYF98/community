package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> selectDiscussPostsByUserId(Integer userId, Integer offset, Integer limit, Integer orderMode);

    // @Param注解用于给参数取别名
    // 如果只有一个参数，并且在<if>里使用，则必须加别名
    Integer selectDiscussPostRows(@Param("userId") Integer userId);

    Integer insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(Integer id);

    Integer updateCommentCount(Integer id, Integer commentCount);

    void updateType(Integer id, Integer type);

    void updateStatus(Integer id, Integer status);

    void updateScore(Integer id, Double score);
}
