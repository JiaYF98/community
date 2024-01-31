package com.nowcoder.community.enumeration;

import lombok.Getter;

@Getter
public enum EntityType {
    POST(1, "post"),
    COMMENT(2, "comment"),
    USER(3, "user");

    private final Integer number;
    private final String name;

    EntityType(Integer number, String name) {
        this.number = number;
        this.name = name;
    }

    public static Integer getNumberByName(String name) {
        EntityType[] values = EntityType.values();
        for (EntityType value : values) {
            if (value.name.equals(name)) {
                return value.number;
            }
        }

        return null;
    }
}
