package com.nowcoder.community.service.impl;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.nowcoder.community.constant.RedisConstant.*;

@Service
public class DataServiceImpl implements DataService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    private String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    private String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    private String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    // 将指定的Ip计入UV
    @Override
    public void recordUV(String ip) {
        String uvKey = getUVKey(dateTimeFormatter.format(LocalDate.now()));
        redisTemplate.opsForHyperLogLog().add(uvKey, ip);
    }

    @Override
    public Long calculateUV(LocalDate start, LocalDate end) {
        String uvKey = getUVKey(dateTimeFormatter.format(start), dateTimeFormatter.format(end));

        // 整理该日期范围内的key
        List<String> keys = new ArrayList<>();
        while (!start.isAfter(end)) {
            keys.add(getUVKey(dateTimeFormatter.format(start)));
            start = start.plusDays(1);
        }

        // 合并数据
        redisTemplate.opsForHyperLogLog().union(uvKey, keys.toArray(String[]::new));

        // 返回统计结果
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    // 将指定用户计入DAU
    @Override
    public void recordDAU(Integer userId) {
        String dauKey = getDAUKey(dateTimeFormatter.format(LocalDate.now()));
        redisTemplate.opsForValue().setBit(dauKey, userId, true);
    }

    // 统计指定日期范围内的DAU
    @Override
    public Long calculateDAU(LocalDate start, LocalDate end) {
        String dauKey = getDAUKey(dateTimeFormatter.format(start), dateTimeFormatter.format(end));

        // 该日期范围之内的key
        List<byte[]> keys = new ArrayList<>();
        while (!start.isAfter(end)) {
            keys.add(getDAUKey(dateTimeFormatter.format(start)).getBytes());
            start = start.plusDays(1);
        }

        // 进行OR运算
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
                    connection.stringCommands().bitOp(
                            RedisStringCommands.BitOperation.OR,
                            dauKey.getBytes(),
                            keys.toArray(byte[][]::new)
                    );
                    return connection.stringCommands().bitCount(dauKey.getBytes());
                }
        );
    }
}
