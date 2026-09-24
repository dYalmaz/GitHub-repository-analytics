package com.example.gitactivity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisConnectionTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void shouldConnectToRedis(){

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String response = connection.ping();
            assertThat(response).isEqualTo("PONG");
        }
    }

}
