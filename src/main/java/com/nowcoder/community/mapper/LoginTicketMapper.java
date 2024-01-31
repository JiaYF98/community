package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Deprecated
@Mapper
public interface LoginTicketMapper {

    @Insert("insert into login_ticket(user_id, ticket, status, expired) " +
            "value (#{userId}, #{ticket}, #{status}, #{expired})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertLoginTicket(LoginTicket loginTicket);

    @Select("select * from login_ticket " +
            "where ticket = #{ticket}")
    LoginTicket selectLoginTicketByTicket(String ticket);

    @Update("update login_ticket set status = #{status} where ticket = #{ticket}")
    void updateLoginTicketStatus(String ticket, Integer status);

}
