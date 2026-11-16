-- MarkNote 初始化脚本（MySQL 8）
-- 执行方式：mysql -uroot -p < init.sql

CREATE DATABASE IF NOT EXISTS marknote
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE marknote;

CREATE TABLE IF NOT EXISTS `user` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    varchar(50)  NOT NULL COMMENT '账号',
  `password`    varchar(100) NOT NULL COMMENT '密码（bcrypt 或 明文，兼容演示）',
  `nickname`    varchar(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      varchar(255) DEFAULT NULL COMMENT '头像',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)   DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `note` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `user_id`     bigint       NOT NULL COMMENT '所属用户id',
  `title`       varchar(200) NOT NULL DEFAULT '无标题笔记',
  `content`     longtext     COMMENT 'markdown 原文',
  `is_deleted`  tinyint(1)   DEFAULT '0' COMMENT '0正常 1回收站',
  `version`     int          DEFAULT 1 COMMENT '版本号',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `note_history` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `note_id`     bigint       NOT NULL COMMENT '笔记id',
  `user_id`     bigint       NOT NULL,
  `title`       varchar(200) DEFAULT NULL,
  `content`     longtext     COMMENT '快照内容',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `image` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `user_id`     bigint       NOT NULL,
  `url`         varchar(255) NOT NULL COMMENT '图片访问地址',
  `bucket`      varchar(100) NOT NULL,
  `object_name` varchar(255) NOT NULL,
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 演示账号：admin / 123456（password 以 $2a$ 开头走 bcrypt 校验，否则走明文比较）
INSERT IGNORE INTO `user`(`username`,`password`,`nickname`)
VALUES ('admin','123456','管理员');

INSERT IGNORE INTO `note`(`id`,`user_id`,`title`,`content`,`is_deleted`,`version`)
VALUES (1,1,'欢迎使用 MarkNote',
'# 欢迎使用 MarkNote

这是一个仿 **mdnice** 的 Markdown 笔记系统。

## 支持的能力

- 左右分栏实时预览、同步滚动
- 代码高亮（支持 java / js / sql / python 等）
- 数学公式，例如 $E = mc^2$
- 表格、引用、列表、图片

> 引用块样式与公众号排版一致

| 功能 | 状态 |
| --- | --- |
| 自动保存 | 已支持 |
| 历史版本 | 已支持 |

1. 左侧写 Markdown
2. 右侧看效果
3. 点右上角「复制公众号 HTML」粘到公众号后台
',0,1);
