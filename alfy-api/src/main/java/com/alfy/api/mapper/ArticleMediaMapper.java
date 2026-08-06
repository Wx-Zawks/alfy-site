package com.alfy.api.mapper;

import com.alfy.api.entity.ArticleMedia;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

/**
 * 文章媒体关联数据访问。
 */
public interface ArticleMediaMapper extends BaseMapper<ArticleMedia> {

    /**
     * 编辑正文时物理替换行内素材，避免逻辑删除记录与唯一索引冲突。
     * 视频和附件等非 INLINE 关联不受影响。
     */
    @Delete("DELETE FROM article_media WHERE article_id = #{articleId} AND usage_type = 'INLINE'")
    int deleteInlineByArticleId(Long articleId);
}
