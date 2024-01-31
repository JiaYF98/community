package com.nowcoder.community.enumeration;

import lombok.Getter;

@Getter
public enum TopicType {
    TOPIC_COMMENT("comment"),
    TOPIC_LIKE("like"),
    TOPIC_FOLLOW("follow"),
    TOPIC_PUBLISH("publish"),
    TOPIC_DELETE("delete");

    private final String typeName;

    TopicType(String typeName) {
        this.typeName = typeName;
    }
}
