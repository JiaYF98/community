package com.nowcoder.community.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.repository.DiscussPostRepository;
import com.nowcoder.community.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Override
    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    @Override
    public void deleteDiscussPost(Integer id) {
        discussPostRepository.deleteById(id);
    }

    @Override
    public List<DiscussPost> searchDiscussPosts(String keyword, Integer current, Integer limit) {
        // 查询语句
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(MultiMatchQuery.of(builder -> builder.fields("title", "content").query(keyword))._toQuery())
                .withSort(Sort.by("type", "score", "createTime").descending())
                .withPageable(PageRequest.of(current, limit))
                .withHighlightQuery(
                        new HighlightQuery(
                                new Highlight(
                                        HighlightParameters.builder().withPreTags("<em>").withPostTags("</em>").build(),
                                        Arrays.asList(new HighlightField("content"), new HighlightField("title"))
                                ),
                                DiscussPost.class
                        )
                )
                .build();

        // 查询结果
        SearchHits<DiscussPost> searchHits = elasticsearchTemplate.search(
                query,
                DiscussPost.class,
                IndexCoordinates.of("discuss_post")
        );

        // 修改查询到的结果，使其高亮显示匹配的标题和内容，放入返回数据列表中
        List<DiscussPost> discussPosts = new ArrayList<>();
        searchHits.forEach(searchHint -> {
            DiscussPost discussPost = searchHint.getContent();
            List<String> title = searchHint.getHighlightField("title");
            List<String> content = searchHint.getHighlightField("content");
            if (!title.isEmpty()) {
                discussPost.setTitle(title.get(0));
            }
            if (!content.isEmpty()) {
                discussPost.setContent(content.get(0));
            }

            discussPosts.add(discussPost);
        });

        return discussPosts;
    }
}
