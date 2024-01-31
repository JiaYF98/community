package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public interface UserService {
    User getUserById(Integer id);

    Map<String, Object> register(User user);

    Integer activation(Integer id, String activationCode);

    Map<String, Object> login(String username, String password, Long expired);

    void logout(String ticket);

    Integer getLoginUserId(String ticket);

    Integer updateHeader(Integer userId, String headerUrl);

    Map<String, Object> updatePassword(User user, String oldPassword, String newPassword);

    Collection<? extends GrantedAuthority> getAuthorities(Integer userId);
}
