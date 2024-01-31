package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.domain}")
    private String domain;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @GetMapping("/setting")
    public String getSettingPage() {
        return "/site/setting";
    }

    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        if (filename == null) {
            model.addAttribute("error", "文件的格式不正确！");
            return "/site/setting";
        }

        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确！");
            return "/site/setting";
        }

        // 生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;

        // 确定文件存放的位置
        File dest = new File(uploadPath + "/" + filename);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("文件上传失败，服务器发生异常！");
        }

        // 更新当前用户的头像路径（web访问路径）
        // http://localhost:8080/community/user/header/xxx.png
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        User user = hostHolder.getUser();
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }


    @GetMapping("/header/{filename}")
    public void getHeader(@PathVariable("filename") String filename,
                          HttpServletResponse response) {
        // 服务器存放路径
        String filePath = uploadPath + "/" + filename;
        // 文件后缀
        String suffix = filename.substring(filename.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);

        try (
                ServletOutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(filePath)
        ) {
            byte[] buffer = new byte[1024];
            int b;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取头像失败：" + e.getMessage());
        }
    }

    @PostMapping("/password")
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user, oldPassword, newPassword);

        if (map.isEmpty()) {
            return "redirect:/index";
        } else {
            map.forEach(model::addAttribute);
            return "/site/setting";
        }
    }

    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") Integer userId, Model model) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("该用户不存在");
        }

        Integer userLikeCount = likeService.getUserLikeCount(userId);
        Long followeeUserCount = followService.getFolloweeUserCount(userId);
        Long followerUserCount = followService.getFollowerUserCount(userId);
        Boolean hasFollowed = followService.hasFollowed(userId);

        model.addAttribute("user", user);
        model.addAttribute("userLikeCount", userLikeCount);
        model.addAttribute("followeeUserCount", followeeUserCount);
        model.addAttribute("followerUserCount", followerUserCount);
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
}
