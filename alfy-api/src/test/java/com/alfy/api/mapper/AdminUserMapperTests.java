package com.alfy.api.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminUserMapperTests {

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Test
    void selectCountCanAccessAdminUserTable() {
        Long count = adminUserMapper.selectCount(null);

        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0L);
    }
}
