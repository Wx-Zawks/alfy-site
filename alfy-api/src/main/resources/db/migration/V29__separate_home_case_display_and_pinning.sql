ALTER TABLE case_project
    ADD COLUMN home_pinned TINYINT NOT NULL DEFAULT 0 AFTER is_featured;

UPDATE case_project
SET home_pinned = 1
WHERE id = (
    SELECT id FROM (
        SELECT id
        FROM case_project
        WHERE deleted = 0
          AND status = 'PUBLISHED'
          AND is_featured = 1
        ORDER BY sort_order ASC, id ASC
        LIMIT 1
    ) AS first_home_case
);
