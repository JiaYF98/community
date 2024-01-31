package com.nowcoder.community.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

import static com.nowcoder.community.constant.DiscussPostConstant.*;

@Data
@Document(indexName = "discuss_post")
public class DiscussPost {
    @Id
    private Integer id;
    @Field(type = FieldType.Integer)
    private Integer userId;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;
    @Field(type = FieldType.Integer)
    private Integer type = COMMON_TYPE_DISCUSS_POST;
    @Field(type = FieldType.Integer)
    private Integer status = NORMAL_STATUS_DISCUSS_POST;
    // Use format = {} to disable built-in date formats in the Field annotation.
    // If you want to use only a custom date format pattern, you must set the format property to empty {}.
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @Field(type = FieldType.Integer)
    private Integer commentCount = DEFAULT_COMMENT_COUNT;
    @Field(type = FieldType.Double)
    private Double score = DEFAULT_SCORE;
}
