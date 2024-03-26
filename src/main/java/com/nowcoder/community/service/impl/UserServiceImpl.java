package com.nowcoder.community.service.impl;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.enumeration.AuthorityType;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.nowcoder.community.constant.ActivationStatusConstant.*;
import static com.nowcoder.community.constant.LoginConstant.*;
import static com.nowcoder.community.constant.RedisConstant.SPLIT;
import static com.nowcoder.community.constant.UserConstant.*;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    private String getUserKey(Integer userId) {
        return PREFIX_USER_KEY + SPLIT + userId;
    }

    private String getLoginTicketKey(String ticket) {
        return PREFIX_LOGIN_TICKET + SPLIT + ticket;
    }

    /**
     * @param username 用户名
     * @return login:user:[username]
     */
    private String getLoginUserKey(String username) {
        return PREFIX_LOGIN_USER_KEY + SPLIT + username;
    }

    @Override
    public User getUserById(Integer id) {
        String userKey = getUserKey(id);
        User user = (User) redisTemplate.opsForValue().get(userKey);
        // 如果redis中没有user的缓存，则去数据库查询，并缓存进redis
        if (user == null) {
            user = userMapper.selectById(id);
            redisTemplate.opsForValue().set(userKey, user, QUERY_CACHE_USER_EXPIRE_SECOND, TimeUnit.SECONDS);
        } else {
            // 访问redis中的user的缓存则更新过期时间
            redisTemplate.expire(userKey, QUERY_CACHE_USER_EXPIRE_SECOND, TimeUnit.SECONDS);
        }
        return user;
    }

    private User getUserByUsername(String username) {
        return userMapper.selectByName(username);
    }

    @Override
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        String username = user.getUsername();
        String password = user.getPassword();
        String email = user.getEmail();

        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "用户名不能为空");
        }

        if (StringUtils.isBlank(password) || password.length() < 8) {
            map.put("passwordMsg", "密码必须为大于8位的非空值");
        }

        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空");
        }

        // 验证账号
        if (getUserByUsername(username) != null) {
            map.put("usernameMsg", "该用户名已存在");
        }

        // 验证邮箱
        if (userMapper.selectByEmail(email) != null) {
            map.put("emailMsg", "该邮箱已被注册");
        }

        if (!map.isEmpty()) {
            return map;
        }

        // 注册用户
        user.setType(0);
        user.setStatus(0);
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(password + user.getSalt()));
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", email);
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(email, "激活账号", content);

        // 删除redis中用户的缓存
        redisTemplate.delete(getLoginUserKey(username));

        return map;
    }

    @Override
    public Integer activation(Integer id, String activationCode) {
        // user:[userId]
        String userKey = getUserKey(id);
        User user = (User) redisTemplate.opsForValue().get(userKey);
        if (user == null) {
            user = userMapper.selectById(id);
            if (user == null) {
                // todo userKey中不能放入完整User类
                // 没有该用户，则在redis中添加一个空用户的缓存
                redisTemplate.opsForValue().set(userKey, new User(), ACTIVATION_CACHE_USER_EXPIRE_SECOND, TimeUnit.SECONDS);
                return INVALID_ACTIVATION;
            } else {
                // 有该用户，在redis中添加该用户的缓存
                redisTemplate.opsForValue().set(userKey, user, ACTIVATION_CACHE_USER_EXPIRE_SECOND, TimeUnit.SECONDS);
            }
        } else {
            // 更新缓存的时间
            redisTemplate.expire(userKey, ACTIVATION_CACHE_USER_EXPIRE_SECOND, TimeUnit.SECONDS);
        }

        if (!user.getActivationCode().equals(activationCode)) {
            return ACTIVATION_FAILURE;
        }

        if (user.getStatus().equals(USER_ACTIVATED)) {
            return ACTIVATION_REPEAT;
        } else {
            userMapper.updateStatus(user.getId(), USER_ACTIVATED);
            // 删除login:user:[username]
            redisTemplate.delete(getLoginUserKey(user.getUsername()));
            // 删除user:[userid]
            redisTemplate.delete(userKey);
            return ACTIVATION_SUCCESS;
        }
    }

    @Override
    public Map<String, Object> login(String username, String password, Long expired) {
        Map<String, Object> map = new HashMap<>();

        // User user = getUserByUsername(username);

        // 先从缓存中查询redis，如果没有再从数据库中查询，并存入缓存
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String loginUserKey = getLoginUserKey(username);
        User user = (User) valueOperations.get(loginUserKey);
        if (user == null) {
            user = getUserByUsername(username);
            // 如果数据库中不存在，则在将空user存入缓存
            if (user == null) {
                user = new User();
            }
            // todo loginUserKey中不能放入完整User类
            valueOperations.set(loginUserKey, user, LOGIN_USER_KEY_EXPIRED_SECONDS, TimeUnit.SECONDS);
        } else {
            redisTemplate.expire(loginUserKey, LOGIN_USER_KEY_EXPIRED_SECONDS, TimeUnit.SECONDS);
        }

        // 验证账号
        // if (user == null) {
        //     map.put("usernameMsg", "该用户不存在");
        //     return map;
        // }
        if (user.getUsername() == null) {
            map.put("usernameMsg", "该用户不存在");
            return map;
        }

        // 验证状态
        if (USER_INACTIVE.equals(user.getStatus())) {
            map.put("usernameMsg", "该用户未激活");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确");
            return map;
        }

        // // 插入 loginTicket
        // LoginTicket loginTicket = new LoginTicket(null, user.getId(), CommunityUtil.generateUUID(), LOGIN_TICKET_VALID, LocalDateTime.now().plusSeconds(expired));
        // loginTicketMapper.insertLoginTicket(loginTicket);
        // map.put("ticket", loginTicket.getTicket());

        // 在redis中添加loginTicket
        String ticket = CommunityUtil.generateUUID();
        String loginTicketKey = getLoginTicketKey(ticket);
        // 格式为 loginTicket:uuid->userId
        valueOperations.set(loginTicketKey, user.getId(), expired, TimeUnit.SECONDS);
        map.put("ticket", ticket);

        return map;
    }

    @Override
    public void logout(String ticket) {
        // loginTicketMapper.updateLoginTicketStatus(ticket, LOGIN_TICKET_INVALID);
        String loginTicketKey = getLoginTicketKey(ticket);
        redisTemplate.delete(loginTicketKey);
    }

    @Override
    public Integer getLoginUserId(String ticket) {
        // return loginTicketMapper.selectLoginTicketByTicket(ticket);
        assert ticket != null;
        return (Integer) redisTemplate.opsForValue().get(getLoginTicketKey(ticket));
    }

    @Override
    public Integer updateHeader(Integer userId, String headerUrl) {
        // 数据库更新，删除redis中的user缓存
        String userKey = getUserKey(userId);
        Integer rows = userMapper.updateHeader(userId, headerUrl);
        redisTemplate.delete(userKey);
        return rows;
    }

    @Override
    public Map<String, Object> updatePassword(User user, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        // 校验原密码
        if (StringUtils.isBlank(oldPassword)
                || !user.getPassword().equals(CommunityUtil.md5(oldPassword + user.getSalt()))) {
            map.put("oldPasswordMsg", "原密码错误");
            return map;
        }

        // 校验新密码
        if (StringUtils.isBlank(newPassword) || newPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            map.put("newPasswordMsg", "密码的长度至少为8位！");
            return map;
        }
        if (newPassword.equals(oldPassword)) {
            map.put("newPasswordMsg", "新旧密码不能相同！");
            return map;
        }

        userMapper.updatePassword(user.getId(), newPassword);
        // 删除redis中的缓存
        // login:user:[username]
        redisTemplate.delete(getLoginUserKey(user.getUsername()));
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());

        return map;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(Integer userId) {
        User user = this.getUserById(userId);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(() -> AuthorityType.getNameFromNumber(user.getType()));
        return authorities;
    }
}
