ALTER TABLE `osh_user`
    ADD COLUMN `github_account` varchar(100) DEFAULT NULL COMMENT 'GitHub账号' AFTER `introduction`,
    ADD COLUMN `wechat_name` varchar(100) DEFAULT NULL COMMENT '微信名称（用户自行填写）' AFTER `github_account`;
