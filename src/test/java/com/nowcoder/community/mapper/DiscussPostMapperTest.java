package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DiscussPostMapperTest {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPostsByUserId(149, 0, 10, 0);
        list.forEach(System.out::println);

        Integer rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }
}
