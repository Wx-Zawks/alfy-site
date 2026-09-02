package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageHeroMappingTests {

    @Test
    void optionalHeroFieldsCanBeExplicitlyCleared() throws NoSuchFieldException {
        List<String> optionalFields = List.of(
                "eyebrow",
                "highlightText",
                "summary",
                "backgroundMediaId",
                "mobileBackgroundMediaId",
                "primaryActionLabel",
                "primaryActionTarget",
                "secondaryActionLabel",
                "secondaryActionTarget"
        );

        for (String fieldName : optionalFields) {
            TableField mapping = PageHero.class.getDeclaredField(fieldName).getAnnotation(TableField.class);
            assertThat(mapping)
                    .as("mapping for %s", fieldName)
                    .isNotNull();
            assertThat(mapping.updateStrategy())
                    .as("update strategy for %s", fieldName)
                    .isEqualTo(FieldStrategy.ALWAYS);
        }
    }
}
