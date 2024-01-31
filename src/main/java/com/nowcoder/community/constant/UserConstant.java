package com.nowcoder.community.constant;

public class UserConstant {
    public static final Integer DEFAULT_USER_ID = 0;
    public static final Integer USER_ACTIVATED = 1;
    public static final Integer USER_INACTIVE = 0;
    public static final Integer MINIMUM_PASSWORD_LENGTH = 8;
    public static final String DELETED_USERNAME = "该用户已注销";
    public static final String DELETED_HEADER_URL = "";
    public static final String PREFIX_USER_KEY = "user";
    public static Integer QUERY_CACHE_USER_EXPIRE_SECOND = 3600 * 24 * 3;
    public static Integer ACTIVATION_CACHE_USER_EXPIRE_SECOND = 60;
}
