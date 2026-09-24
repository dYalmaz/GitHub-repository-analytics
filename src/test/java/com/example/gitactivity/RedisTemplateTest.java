package com.example.gitactivity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void shouldWriteAndReadFromRedis() {

        String key = "test:key";
        String value = "hello redis";

        redisTemplate.opsForValue().set(key, value);

        Object result = redisTemplate.opsForValue().get(key);

        assertThat(result).isEqualTo(value);

        redisTemplate.delete(key);

    }


}
