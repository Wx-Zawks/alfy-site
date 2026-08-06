package com.alfy.api.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MediaAssetMapperTests {

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Test
    void countsReferencesAcrossAllMediaConsumers() {
        long references = mediaAssetMapper.countActiveReferences(Long.MAX_VALUE);

        assertThat(references).isZero();
    }
}
