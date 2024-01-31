package com.nowcoder.community.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.DiscussPostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class DiscussPostRepositoryTest {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    void testInsert() {
        // discussPostRepository.save(discussPostMapper.selectDiscussPostById(143));
        // discussPostRepository.save(discussPostMapper.selectDiscussPostById(144));
        // discussPostRepository.save(discussPostMapper.selectDiscussPostById(145));

        elasticsearchTemplate.save(discussPostMapper.selectDiscussPostById(275));
    }

    @Test
    void testInsertList() {
        List<Integer> userIdList = Arrays.asList(11, 101, 102, 103, 111, 112, 131, 132, 133, 134, 138, 145, 146, 149, 155);
        userIdList.forEach(userId ->
                discussPostRepository.saveAll(discussPostMapper.selectDiscussPostsByUserId(userId, 0, 100, 0)));
    }

    @Test
    void testUpdate() {
        DiscussPost discussPost = discussPostMapper.selectDiscussPostById(143);
        discussPost.setContent("修改的内容");
        discussPostRepository.save(discussPost);
    }

    @Test
    void testDelete() {
        // discussPostRepository.deleteById(144);\
        // discussPostRepository.deleteAll();
        elasticsearchTemplate.delete("275", DiscussPost.class);
    }

    @Test
    void testSearchByTemplate() {
        List<HighlightField> fields = Arrays.asList(new HighlightField("title"), new HighlightField("content"));
        HighlightParameters parameters = HighlightParameters.builder().withPreTags("<em>").withPostTags("</em>").build();
        NativeQuery query = new NativeQueryBuilder()
                // .withQuery(MultiMatchQuery.of(builder -> builder.fields("title", "content").query("互联网寒冬"))._toQuery())
                .withQuery(MultiMatchQuery.of(builder -> builder.fields("title").query("互联网寒冬"))._toQuery())
                .withQuery(queryBuilder -> queryBuilder.multiMatch(mb -> mb.fields("title", "content").query("互联网寒冬")))
                // .withQuery(queryBuilder -> queryBuilder.multiMatch(mb -> mb.fields("title").query("互联网寒冬")))
                // .withQuery(QueryBuilders.multiMatch(m -> m.query("互联网寒冬")))
                .withSort(Sort.by("type", "score", "createTime").descending())
                .withPageable(PageRequest.of(0, 20))
                .withHighlightQuery(new HighlightQuery(new Highlight(parameters, fields), DiscussPost.class))
                .build();

        SearchHits<DiscussPost> searchHits = elasticsearchTemplate.search(query, DiscussPost.class, IndexCoordinates.of("discuss_post"));

        searchHits.forEach(searchHit -> {
            // 高亮结果处理
            List<String> title = searchHit.getHighlightField("title");
            List<String> content = searchHit.getHighlightField("content");

            DiscussPost discussPost = searchHit.getContent();
            if (!title.isEmpty()) {
                discussPost.setTitle(title.get(0));
            }
            if (!content.isEmpty()) {
                discussPost.setContent(content.get(0));
            }

            System.out.println(discussPost);
        });
    }
}
