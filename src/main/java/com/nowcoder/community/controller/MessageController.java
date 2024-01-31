package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page page) {
        // 分页信息
        page.setPath("/message/letter/list");
        page.setLimit(5);
        page.setRows(messageService.findConversationCount());

        // 会话管理
        List<Map<String, Object>> conversations = messageService.findConversations(page.getOffset(), page.getLimit());
        // 查询未读消息
        Integer letterUnreadCount = messageService.findLetterUnreadCount(null);
        // 查询未读系统消息
        Integer noticeUnreadCount = messageService.findNoticeUnreadCount(null);

        model.addAttribute("conversations", conversations);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, Page page) {
        // 分页信息
        page.setPath("/message/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        page.setLimit(5);

        // 私信列表
        List<Map<String, Object>> letters = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        // 私信目标
        User target = messageService.getLetterTarget(conversationId);

        model.addAttribute("letters", letters);
        model.addAttribute("target", target);

        return "/site/letter-detail";
    }

    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter(String targetUsername, String content) {
        if (StringUtils.isBlank(targetUsername)) {
            return CommunityUtil.getJSONString(1, "请输入目标用户！");
        }
        if (StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(1, "请输入私信内容！");
        }

        return messageService.addMessage(targetUsername, content);
    }

    @GetMapping("/notice/list")
    public String getNoticeList(Model model) {
        List<Map<String, Object>> noticesMapList = messageService.findLatestNoticeList();

        // 查询未读消息
        Integer letterUnreadCount = messageService.findLetterUnreadCount(null);
        // 查询未读系统消息
        Integer noticeUnreadCount = messageService.findNoticeUnreadCount(null);

        model.addAttribute("notices", noticesMapList);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @GetMapping("/notice/detail/{topic}")
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        Integer noticeCount = messageService.findNoticeCount(topic);

        page.setLimit(5);
        page.setRows(noticeCount);
        page.setPath("/message/notice/detail/" + topic);

        List<Map<String, Object>> notices = messageService.findNotices(topic, page.getOffset(), page.getLimit());
        model.addAttribute("notices", notices);

        return "/site/notice-detail";
    }

}
