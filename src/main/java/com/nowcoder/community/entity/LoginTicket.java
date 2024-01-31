package com.nowcoder.community.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginTicket {
    Integer id;
    Integer userId;
    String ticket;
    Integer status;
    LocalDateTime expired;
}
