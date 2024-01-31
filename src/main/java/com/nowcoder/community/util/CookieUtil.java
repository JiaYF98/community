package com.nowcoder.community.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public class CookieUtil {
    public static String getValue(HttpServletRequest request, String name) {
        if (request == null) {
            throw new IllegalArgumentException("request值为空");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("cookie名称值为空");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
