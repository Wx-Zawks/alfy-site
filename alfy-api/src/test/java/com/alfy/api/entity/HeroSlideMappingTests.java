package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeroSlideMappingTests {

    @Test
    void backgroundActionTargetCanBeExplicitlyCleared() throws NoSuchFieldException {
        TableField mapping = HeroSlide.class.getDeclaredField("backgroundActionTarget")
                .getAnnotation(TableField.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }
}
