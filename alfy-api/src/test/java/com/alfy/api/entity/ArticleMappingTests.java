package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleMappingTests {

    @Test
    void homeSlotCanBeExplicitlyCleared() throws NoSuchFieldException {
        TableField mapping = Article.class.getDeclaredField("homeSlot").getAnnotation(TableField.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }
}
