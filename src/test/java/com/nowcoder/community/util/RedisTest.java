package com.nowcoder.community.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void test1() {
        String redisKey = "test:count";
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        valueOperations.set(redisKey, 1);
        System.out.println(valueOperations.get(redisKey));
        System.out.println(valueOperations.increment(redisKey));
        System.out.println(valueOperations.decrement(redisKey));
    }

    // 统计20万个重复数据的独立总数
    @Test
    void testHyperLogLog() {
        String redisKey1 = "test:hll:01";
        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey1, i);
        }

        for (int i = 0; i < 100000; i++) {
            int r = (int) (Math.random() * 100000);
            redisTemplate.opsForHyperLogLog().add(redisKey1, r);
        }

        Long size = redisTemplate.opsForHyperLogLog().size(redisKey1);
        System.out.println(size);
    }

    // 将三组数据合并，再统计合并后的重复数据的独立总数
    @Test
    void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }

        String redisKey3 = "test:hll:03";
        for (int i = 10000; i < 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 15000; i < 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        System.out.println(redisTemplate.opsForHyperLogLog().size(unionKey));
    }

    // 统计一组数据的布尔值
    @Test
    void testBitMap() {
        String redisKey = "test:bm:01";

        // 记录
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        // 查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));

        // 统计
        Long size = redisTemplate.execute((RedisCallback<Long>) connection -> connection.stringCommands().bitCount(redisKey.getBytes()));
        System.out.println(size);
    }

    // 统计三组数据的布尔值，并对这三组数据做OR运算
    @Test
    void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        String redisKey = "test:bm:or";
        Long size = redisTemplate.execute((RedisCallback<Long>) connection -> {
            connection.stringCommands().bitOp(RedisStringCommands.BitOperation.OR,
                    redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
            return connection.stringCommands().bitCount(redisKey.getBytes());
        });

        System.out.println(size);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));
    }
}
