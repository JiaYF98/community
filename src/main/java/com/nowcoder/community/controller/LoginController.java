package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.nowcoder.community.constant.ActivationStatusConstant.*;
import static com.nowcoder.community.constant.LoginConstant.*;
import static com.nowcoder.community.constant.RedisConstant.PREFIX_KAPTCHA;
import static com.nowcoder.community.constant.RedisConstant.SPLIT;

@Controller
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/register")
    public String getRegisterPage() {
        return "/site/register";
    }

    @PostMapping("/register")
    public String register(Model model, User user) {
        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封邮件，请您尽快激活");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            map.forEach(model::addAttribute);
            return "/site/register";
        }
    }

    @GetMapping("/activation/{id}/{activationCode}")
    public String activation(Model model,
                             @PathVariable("id") Integer id,
                             @PathVariable("activationCode") String activationCode) {
        Integer activationStatus = userService.activation(id, activationCode);

        if (activationStatus.equals(ACTIVATION_SUCCESS)) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (activationStatus.equals(ACTIVATION_REPEAT)) {
            model.addAttribute("msg", "已经激活，不需要重复激活");
            model.addAttribute("target", "/index");
        } else if (activationStatus.equals(ACTIVATION_FAILURE)) {
            model.addAttribute("msg", "激活失败，您提供的激活码错误！");
            model.addAttribute("target", "/index");
        } else if (activationStatus.equals(INVALID_ACTIVATION)) {
            model.addAttribute("msg", "激活失败，待激活用户不存在");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    private String getKaptchaKey(String kaptcha) {
        return PREFIX_KAPTCHA + SPLIT + kaptcha;
    }

    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // // 将验证码存入session
        // session.setAttribute("kaptcha", text);

        String kaptcha = CommunityUtil.generateUUID();

        // 将uuid存入cookie做为凭证
        Cookie cookie = new Cookie("kaptcha", kaptcha);
        cookie.setPath(contextPath);
        cookie.setMaxAge(KAPTCHA_EXPIRED_SECONDS);
        response.addCookie(cookie);

        // 将验证码存入redis中
        String kaptchaKey = getKaptchaKey(kaptcha);
        redisTemplate.opsForValue().set(kaptchaKey, text, KAPTCHA_EXPIRED_SECONDS, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("/image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            log.error("响应验证码失败：" + e.getMessage());
        }
    }

    // todo 修改login
    @PostMapping("/login")
    public String login(String username, String password, String code, boolean rememberMe,
                        Model model, HttpServletRequest request, HttpServletResponse response) {
        // 验证用户名、密码和验证码是否为空
        if (StringUtils.isBlank(username)) {
            model.addAttribute("usernameMsg", "用户名不能为空！");
            return "/site/login";
        }

        if (StringUtils.isBlank(password)) {
            model.addAttribute("passwordMsg", "密码不能为空！");
            return "/site/login";
        }

        if (StringUtils.isBlank(code)) {
            model.addAttribute("codeMsg", "请输入验证码！");
            return "/site/login";
        }

        // // 检查验证码
        // String kaptcha = (String) session.getAttribute("kaptcha");
        // if (StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
        //     model.addAttribute("codeMsg", "验证码不正确！");
        //     return "/site/login";
        // }

        // 检查验证码是否过期
        String kaptcha = CookieUtil.getValue(request, "kaptcha");
        if (StringUtils.isBlank(kaptcha)) {
            model.addAttribute("codeMsg", "验证码过期，请刷新重试！");
            return "/site/login";
        }

        if (!code.equalsIgnoreCase((String) redisTemplate.opsForValue().get(getKaptchaKey(kaptcha)))) {
            model.addAttribute("codeMsg", "验证码错误！");
            return "/site/login";
        }

        // 检查账号密码
        Long expire = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expire);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expire.intValue());
            response.addCookie(cookie);

            return "redirect:/index";
        } else {
            map.forEach(model::addAttribute);
            return "/site/login";
        }
    }

    // @GetMapping("/logout")
    // public String logout(@CookieValue("ticket") String ticket) {
    //     userService.logout(ticket);
    //     return "redirect:/login";
    // }
}
