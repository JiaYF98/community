package com.nowcoder.community.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Integer id;
    private Integer userId;
    private Integer entityType;
    private Integer entityId;
    private Integer targetId = 0;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
