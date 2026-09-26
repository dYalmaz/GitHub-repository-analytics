package com.example.gitactivity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldWriteAndReadFromRedis() {

        String key = "test:key";
        String value = "hello redis";

        redisTemplate.opsForValue().set(key, value);

        String result = redisTemplate.opsForValue().get(key);

        assertThat(result).isEqualTo(value);

        redisTemplate.delete(key);

    }

    @Test
    void shouldSetExpirationOnRedisKey() {

        String key = "test:ttl";
        String value = "expires";

        redisTemplate.opsForValue().set(
                key,
                value,
                10,
                TimeUnit.SECONDS
        );

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        assertThat(ttl).isBetween(1L, 10L);

        redisTemplate.delete(key);
    }


}
