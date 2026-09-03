ALTER TABLE hero_slide
    ADD COLUMN background_action_target VARCHAR(500) NULL AFTER mobile_media_id;

UPDATE site_navigation_item
SET label = '案例'
WHERE navigation_area = 'HEADER'
  AND target = '/applications'
  AND label = '应用与案例'
  AND deleted = 0;
