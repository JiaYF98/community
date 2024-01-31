package com.nowcoder.community.util;


import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class SpringBootTests {

    private static final Logger log = LoggerFactory.getLogger(SpringBootTests.class);

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeAll
    public static void beforeAll() {
        log.info("beforeAll");
    }

    @AfterAll
    public static void afterAll() {
        log.info("afterAll");
    }

    @BeforeEach
    public void beforeEach() {
        log.info("beforeEach");

        // 初始化测试数据
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("Test title");
        data.setContent("Test Content");
        data.setCreateTime(LocalDateTime.parse("2023-12-18T00:00:00"));
        discussPostService.addDiscussPost(data);

    }

    @AfterEach
    public void afterEach() {
        log.info("afterEach");

        discussPostService.deletePost(data.getId());
    }

    @Test
    public void testFindById() {
        DiscussPost discussPost = discussPostService.findDiscussPostById(data.getId());
        Assertions.assertNotNull(discussPost);
        Assertions.assertEquals(data, discussPost);
    }

    @Test
    public void testUpdateScore() {
        discussPostService.updateScore(data.getId(), 2000.00);
        DiscussPost discussPost = discussPostService.findDiscussPostById(data.getId());
        Assertions.assertEquals(2000.00, discussPost.getScore(), 2);
    }

}
