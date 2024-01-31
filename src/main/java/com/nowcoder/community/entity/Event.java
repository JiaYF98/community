package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    // Kafka主题
    private String topic;
    // 产生事件的用户
    private Integer userId;
    // 目标实体类型
    private Integer entityType;
    // 目标实体id
    private Integer entityId;
    // 目标实体拥有者id
    private Integer entityUserId;
    // 其它数据
    private Map<String, Object> data;
}
