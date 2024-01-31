package com.nowcoder.community.enumeration;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum AuthorityType {
    USER(0, "user"),
    ADMIN(1, "admin"),
    MODERATOR(2, "moderator");

    private final Integer number;
    private final String name;

    AuthorityType(Integer number, String name) {
        this.number = number;
        this.name = name;
    }

    public static String getNameFromNumber(Integer number) {
        AuthorityType[] values = AuthorityType.values();
        for (AuthorityType value : values) {
            if (value.getNumber().equals(number)) {
                return value.getName();
            }
        }
        return null;
    }
}
