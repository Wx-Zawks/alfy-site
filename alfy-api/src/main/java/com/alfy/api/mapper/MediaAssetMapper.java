package com.alfy.api.mapper;

import com.alfy.api.entity.MediaAsset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * 媒体资源数据访问。
 */
public interface MediaAssetMapper extends BaseMapper<MediaAsset> {

    /**
     * 统计仍在使用该素材的有效内容。素材表没有外键约束，因此删除前需要显式保护引用。
     */
    @Select("""
            SELECT COALESCE(SUM(reference_count), 0)
            FROM (
                SELECT COUNT(*) AS reference_count FROM article
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM article_media am
                    INNER JOIN article a ON a.id = am.article_id AND a.deleted = 0
                    WHERE am.deleted = 0 AND am.media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM product_category
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM product
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM application_scene
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM case_project
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM hero_slide
                    WHERE deleted = 0
                      AND (desktop_media_id = #{mediaId} OR mobile_media_id = #{mediaId})
                UNION ALL
                SELECT COUNT(*) FROM technology_page
                    WHERE deleted = 0 AND hero_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM site_setting
                    WHERE deleted = 0
                      AND (logo_media_id = #{mediaId} OR wechat_qr_media_id = #{mediaId})
                UNION ALL
                SELECT COUNT(*) FROM page_hero
                    WHERE deleted = 0
                      AND (background_media_id = #{mediaId}
                           OR mobile_background_media_id = #{mediaId})
                UNION ALL
                SELECT COUNT(*) FROM partner
                    WHERE deleted = 0 AND logo_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM content_page
                    WHERE deleted = 0 AND cover_media_id = #{mediaId}
                UNION ALL
                SELECT COUNT(*) FROM home_section
                    WHERE deleted = 0
                      AND (image_media_id = #{mediaId} OR mobile_media_id = #{mediaId})
            ) media_references
            """)
    long countActiveReferences(Long mediaId);
}
