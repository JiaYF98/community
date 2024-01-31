package com.nowcoder.community.constant;

public class LoginConstant {
    // 默认超时时间
    public static final Long DEFAULT_EXPIRED_SECONDS = 3600L * 12L;
    // 记住超时时间
    public static final Long REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100L;
    public static final Integer LOGIN_TICKET_VALID = 0;
    public static final Integer LOGIN_TICKET_INVALID = 1;
    public static final Integer KAPTCHA_EXPIRED_SECONDS = 60;
    public static final String PREFIX_LOGIN_TICKET = "loginTicket";
    public static String PREFIX_LOGIN_USER_KEY = "login:user";
    public static final Integer LOGIN_USER_KEY_EXPIRED_SECONDS = 60;
}
