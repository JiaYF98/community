package com.nowcoder.community.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.MessageMapper;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        User user = hostHolder.getUser();
        if (user == null || modelAndView == null) {
            return;
        }

        Integer userId = user.getId();
        Integer letterUnreadCount = messageMapper.selectLetterUnreadCount(userId, null);
        Integer noticeUnreadCount = messageMapper.selectNoticeUnreadCount(userId, null);
        modelAndView.addObject("messageUnreadCount", letterUnreadCount + noticeUnreadCount);
    }
}
