CREATE TABLE case_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    slug        VARCHAR(100)    NOT NULL,
    summary     VARCHAR(1000)   NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    status      TINYINT         NOT NULL DEFAULT 1,
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_case_category_slug (slug),
    KEY idx_case_category_display (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE case_project
    ADD COLUMN category_id BIGINT UNSIGNED NULL AFTER id,
    MODIFY COLUMN scene_id BIGINT UNSIGNED NULL,
    ADD KEY idx_case_project_category (category_id, status, sort_order, published_at);

INSERT INTO case_category (name, slug, summary, sort_order, status, created_at, updated_at, deleted)
SELECT scene.name,
       scene.slug,
       scene.summary,
       scene.sort_order,
       CASE WHEN scene.status = 'PUBLISHED' THEN 1 ELSE 0 END,
       scene.created_at,
       scene.updated_at,
       scene.deleted
FROM application_scene scene;

UPDATE case_project project
    INNER JOIN application_scene scene ON scene.id = project.scene_id
    INNER JOIN case_category category ON category.slug = scene.slug
SET project.category_id = category.id;

CREATE TABLE case_scene_rel
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    case_id    BIGINT UNSIGNED NOT NULL,
    scene_id   BIGINT UNSIGNED NOT NULL,
    sort_order INT             NOT NULL DEFAULT 0,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_case_scene (case_id, scene_id),
    KEY idx_case_scene_scene (scene_id, sort_order),
    KEY idx_case_scene_case (case_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO case_scene_rel (case_id, scene_id, sort_order)
SELECT id, scene_id, 0
FROM case_project
WHERE scene_id IS NOT NULL;
