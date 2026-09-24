package com.example.gitactivity;

import com.example.gitactivity.service.RepositoryCacheKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RepositoryCacheKeyTest {

    @Test
    void shouldCreateRepositoryCacheKey() {

        String key= RepositoryCacheKey.create("spring-projects", "spring-boot");

        assertThat(key).isEqualTo("repository:spring-projects:spring-boot");

    }

    @Test
    void shouldUseTenMinuteCacheTtl(){

        assertThat(RepositoryCacheKey.TTL_MINUTES).isEqualTo(10);

    }

}
