package com.alfy.api.mapper;

import com.alfy.api.entity.ArticleCategoryRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

/**
 * 文章多分类关联数据访问。
 */
public interface ArticleCategoryRelationMapper extends BaseMapper<ArticleCategoryRelation> {

    /** 编辑分类时物理替换关联，避免逻辑删除记录与唯一索引冲突。 */
    @Delete("DELETE FROM article_category_relation WHERE article_id = #{articleId}")
    int deleteByArticleId(Long articleId);
}
