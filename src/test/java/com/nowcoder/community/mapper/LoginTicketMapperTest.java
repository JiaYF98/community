package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.LoginTicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static com.nowcoder.community.constant.LoginConstant.LOGIN_TICKET_INVALID;

@SpringBootTest
public class LoginTicketMapperTest {
    @Autowired
    private LoginTicketMapper loginTicketMapper;

    // @Test
    // void testInsert() {
    //     LoginTicket loginTicket = new LoginTicket(null, 152, "abc", 0, LocalDateTime.now().plusDays(1L));
    //     System.out.println(loginTicket);
    //     loginTicketMapper.insertLoginTicket(loginTicket);
    // }

    @Test
    void testUpdate() {
        loginTicketMapper.updateLoginTicketStatus("abc", LOGIN_TICKET_INVALID);
    }

    @Test
    void testSelect() {
        System.out.println(loginTicketMapper.selectLoginTicketByTicket("abc"));
    }
}
