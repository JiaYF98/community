package com.nowcoder.community.config;

import com.nowcoder.community.enumeration.AuthorityType;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String COOKIE_TICKET_KEY = "ticket";

    @Autowired
    private UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 构建用户认证的结果,并存入SecurityContext,以便于Security进行授权
                // 确保在authorizeHttpRequests之前SecurityContext中有认证对象和权限
                .addFilterBefore((request, response, chain) -> {
                    String ticket = CookieUtil.getValue((HttpServletRequest) request, COOKIE_TICKET_KEY);
                    if (!StringUtils.isBlank(ticket)) {
                        // 从redis中获取userId
                        Integer userId = userService.getLoginUserId(ticket);
                        // 如果userId为null，则登录凭证过期，不添加权限
                        if (userId != null) {
                            SecurityContext context = SecurityContextHolder.getContext();
                            Authentication authentication = context.getAuthentication();
                            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                                    authentication.getPrincipal(),
                                    authentication.getCredentials(),
                                    userService.getAuthorities(userId)
                            ));
                        }
                    }
                    chain.doFilter(request, response);
                }, AuthorizationFilter.class)
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers(
                                "/user/setting",
                                "/user/upload",
                                "/discuss/add",
                                "/comment/add/**",
                                "/message/**",
                                "/like",
                                "/follow/**"
                        ).authenticated()
                        .requestMatchers(
                                "/discuss/top",
                                "/discuss/essence"
                        ).hasAnyAuthority(
                                AuthorityType.MODERATOR.getName()
                        )
                        .requestMatchers(
                                "/discuss/delete",
                                "/data/**",
                                "/actuator/**"
                        ).hasAnyAuthority(
                                AuthorityType.ADMIN.getName()
                        )
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                        // 没有登录
                        .authenticationEntryPoint((request, response, authenticationException) ->
                                feedback(request, response, "你还没有登录哦！", "/login"))
                        // 权限不足
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                feedback(request, response, "你没有访问此功能的权限", "/denied"))
                )
                // Security底层默认会拦截/logout请求,进行退出处理
                .logout(logoutConfigurer -> logoutConfigurer
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            userService.logout(CookieUtil.getValue(request, COOKIE_TICKET_KEY));
                            response.sendRedirect(request.getContextPath() + "/index");
                        }))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private void feedback(HttpServletRequest request, HttpServletResponse response, String msg, String pageUrl) throws IOException {
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(403, msg));
        } else {
            response.sendRedirect(request.getContextPath() + pageUrl);
        }
    }
}
