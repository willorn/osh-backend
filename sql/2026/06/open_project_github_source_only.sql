-- 开源项目改为仅从绑定的 GitHub 数据源同步。
-- 不再保留人工新增、删除、审核流程，历史待审核/拒绝状态统一为可见。

UPDATE `osh_open_project`
SET `status` = 1,
    `reject_reason` = NULL
WHERE `delete_flag` = 0
  AND (`status` IS NULL OR `status` <> 1);
