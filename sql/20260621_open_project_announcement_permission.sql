-- Open project announcement adaptation and permission split support.
-- No new project-module tables are needed here; module/component/resource tables
-- were already introduced by 20260617_open_project_module_component.sql.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_as_cs;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_announcement'
    AND index_name = 'idx_openproject_announcement'
);
SET @sql := IF(
  @idx_exists = 0,
  'ALTER TABLE osh_announcement ADD INDEX idx_openproject_announcement (source_module, channel, status, delete_flag, create_time)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO osh_announcement (
  title, link, icon_code, channel, resource_type, resource_id,
  sort, status, source, source_module, delete_flag, create_by, create_time, update_by, update_time
)
SELECT
  CONCAT('最新同步开源项目：', p.project_name),
  '/openproject/list',
  'publish',
  1,
  'openproject',
  p.id,
  0,
  4,
  'system',
  'openproject',
  0,
  'system',
  COALESCE(p.last_sync_time, p.create_time, NOW()),
  'system',
  NOW()
FROM osh_open_project p
WHERE p.delete_flag = 0
  AND p.status = 1
  AND NOT EXISTS (
    SELECT 1
    FROM osh_announcement a
    WHERE a.delete_flag = 0
      AND a.source_module = 'openproject'
      AND a.channel = 1
      AND a.resource_type = 'openproject'
      AND a.resource_id = p.id
  );

INSERT INTO osh_announcement (
  title, link, icon_code, channel, resource_type, resource_id,
  sort, status, source, source_module, delete_flag, create_by, create_time, update_by, update_time
)
SELECT
  CONCAT('新配置 GitHub 数据源：', s.github_url),
  s.github_url,
  'online',
  2,
  'openproject_source',
  s.id,
  0,
  4,
  'system',
  'openproject',
  0,
  'system',
  COALESCE(s.create_time, NOW()),
  'system',
  NOW()
FROM osh_open_project_source s
WHERE s.delete_flag = 0
  AND NOT EXISTS (
    SELECT 1
    FROM osh_announcement a
    WHERE a.delete_flag = 0
      AND a.source_module = 'openproject'
      AND a.channel = 2
      AND a.resource_type = 'openproject_source'
      AND a.resource_id = s.id
  );
