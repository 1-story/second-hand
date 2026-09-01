-- ============================================================
-- 校园二手交易平台（第二手 SecondHand AI） 数据库建表脚本
-- 设计人：田博（后端）  适用：MySQL 8.x
-- 字符集：utf8mb4  引擎：InnoDB  主键：BIGINT 自增  逻辑删除：deleted
-- 说明：密码/权限字段由登录模块（陈思瀚）扩展，本脚本只含基础用户表
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `second_hand`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE `second_hand`;

-- ------------------------------------------------------------
-- 1. 用户表（基础字段；登录/权限由登录模块扩展）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `student_no`  VARCHAR(20)     NOT NULL                COMMENT '学号（实名认证）',
  `username`    VARCHAR(50)     NOT NULL                COMMENT '登录名',
  `nickname`    VARCHAR(50)     DEFAULT NULL            COMMENT '昵称',
  `avatar`      VARCHAR(255)    DEFAULT NULL            COMMENT '头像URL',
  `phone`       VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100)    DEFAULT NULL            COMMENT '邮箱',
  `credit_score` INT            NOT NULL DEFAULT 100    COMMENT '信用分（100起，冗余汇总）',
  `is_student`  TINYINT         NOT NULL DEFAULT 1      COMMENT '是否在校学生 0否 1是',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0禁用 1正常',
  `deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 信用分记录表（互评/交易行为增减，T1 重点表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_credit`;
CREATE TABLE `user_credit` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`        BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `change_amount`  INT             NOT NULL                COMMENT '变动分值（正加负减）',
  `reason_type`    TINYINT         NOT NULL                COMMENT '类型 1交易成功 2互评好评 3互评差评 4违规 5举报成立 6申诉恢复 7签到',
  `reason`         VARCHAR(200)    DEFAULT NULL            COMMENT '原因描述',
  `balance_after`  INT             NOT NULL                COMMENT '变动后余额',
  `related_order_id` BIGINT UNSIGNED DEFAULT NULL          COMMENT '关联订单ID',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `created_at`),
  KEY `idx_order` (`related_order_id`)
) ENGINE=InnoDB COMMENT='信用分变动记录表';

-- ------------------------------------------------------------
-- 3. 商品分类表（含 AI 估价规则参数）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id`        BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '父分类ID，0为一级',
  `name`             VARCHAR(50)     NOT NULL                COMMENT '分类名称',
  `icon`             VARCHAR(255)    DEFAULT NULL            COMMENT '分类图标URL',
  `base_price`       DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT 'AI估价基准价（该品类9成新基准）',
  `depreciation_rate` DECIMAL(5,4)   NOT NULL DEFAULT 0.1500 COMMENT '年折旧率（默认15%/年）',
  `heat_weight`      DECIMAL(4,3)    NOT NULL DEFAULT 1.000  COMMENT '市场热度系数（0.5~2.0）',
  `sort_order`       INT             NOT NULL DEFAULT 0      COMMENT '排序',
  `status`           TINYINT         NOT NULL DEFAULT 1      COMMENT '状态 0停用 1启用',
  `deleted`          TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB COMMENT='商品分类表（含AI估价参数）';

-- ------------------------------------------------------------
-- 4. 商品主表（核心业务表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `seller_id`       BIGINT UNSIGNED NOT NULL                COMMENT '卖家ID',
  `category_id`     BIGINT UNSIGNED NOT NULL                COMMENT '分类ID',
  `title`           VARCHAR(100)    NOT NULL                COMMENT '标题',
  `description`     TEXT                                    COMMENT '详细描述',
  `price`           DECIMAL(10,2)   NOT NULL                COMMENT '期望售价',
  `estimated_price` DECIMAL(10,2)   DEFAULT NULL            COMMENT 'AI估价（可空）',
  `final_price`     DECIMAL(10,2)   DEFAULT NULL            COMMENT '实际成交价',
  `condition_level` TINYINT         NOT NULL DEFAULT 7      COMMENT '成色等级 1~10（10=全新）',
  `condition_desc`  VARCHAR(200)    DEFAULT NULL            COMMENT '成色文字描述',
  `tags`            VARCHAR(255)    DEFAULT NULL            COMMENT '标签，逗号分隔',
  `location`        VARCHAR(100)    DEFAULT NULL            COMMENT '交易地点/校区',
  `cover_image`     VARCHAR(255)    DEFAULT NULL            COMMENT '封面图URL',
  `status`          TINYINT         NOT NULL DEFAULT 0      COMMENT '状态 0草稿 1在售 2已下架 3已售出 4审核中 5审核驳回',
  `view_count`      INT             NOT NULL DEFAULT 0      COMMENT '浏览量',
  `favorite_count`  INT             NOT NULL DEFAULT 0      COMMENT '收藏数',
  `deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_seller` (`seller_id`, `status`),
  KEY `idx_category` (`category_id`, `status`),
  KEY `idx_status_time` (`status`, `created_at`),
  KEY `idx_price` (`price`),
  FULLTEXT KEY `ft_title_desc` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='商品主表';

-- ------------------------------------------------------------
-- 5. 商品图片表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id`  BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
  `url`         VARCHAR(255)    NOT NULL                COMMENT '图片URL',
  `sort_order`  INT             NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品图片表';

-- ------------------------------------------------------------
-- 6. 收藏表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `product_id`  BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='收藏表';

-- ------------------------------------------------------------
-- 7. 浏览记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `product_id`  BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
  `view_count`  INT             NOT NULL DEFAULT 1      COMMENT '累计浏览次数',
  `last_view_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_time` (`user_id`, `last_view_at`)
) ENGINE=InnoDB COMMENT='浏览记录表';

-- ------------------------------------------------------------
-- 8. 担保交易订单表（V1.0，T1 重点表，先建表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`    VARCHAR(32)     NOT NULL                COMMENT '订单号',
  `product_id`  BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
  `buyer_id`    BIGINT UNSIGNED NOT NULL                COMMENT '买家ID',
  `seller_id`   BIGINT UNSIGNED NOT NULL                COMMENT '卖家ID',
  `amount`      DECIMAL(10,2)   NOT NULL                COMMENT '交易金额',
  `status`      TINYINT         NOT NULL DEFAULT 0      COMMENT '状态 0待付款 1待发货 2待收货 3已完成 4已取消 5纠纷中',
  `pay_type`    TINYINT         NOT NULL DEFAULT 1      COMMENT '支付方式 1线上担保',
  `contact`     VARCHAR(100)    DEFAULT NULL            COMMENT '联系方式',
  `address`     VARCHAR(255)    DEFAULT NULL            COMMENT '收货/面交地址',
  `remark`      VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
  `paid_at`     DATETIME        DEFAULT NULL            COMMENT '付款时间',
  `finished_at` DATETIME        DEFAULT NULL            COMMENT '完成时间',
  `deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer` (`buyer_id`, `status`),
  KEY `idx_seller` (`seller_id`, `status`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='担保交易订单表';

-- ------------------------------------------------------------
-- 9. 买卖互评表（信用分依据，V1.0）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id`    BIGINT UNSIGNED NOT NULL                COMMENT '订单ID',
  `reviewer_id` BIGINT UNSIGNED NOT NULL                COMMENT '评价人ID',
  `reviewee_id` BIGINT UNSIGNED NOT NULL                COMMENT '被评人ID',
  `rating`      TINYINT         NOT NULL DEFAULT 5      COMMENT '评分 1~5',
  `content`     VARCHAR(500)    DEFAULT NULL            COMMENT '评价内容',
  `type`        TINYINT         NOT NULL DEFAULT 1      COMMENT '类型 1买家评卖家 2卖家评买家',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_reviewee` (`reviewee_id`)
) ENGINE=InnoDB COMMENT='买卖互评表';

-- ------------------------------------------------------------
-- 10. 站内私信表（林天楚模块，先建表统一字段）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id` BIGINT UNSIGNED NOT NULL               COMMENT '发送人ID',
  `to_user_id`  BIGINT UNSIGNED NOT NULL                COMMENT '接收人ID',
  `content`     VARCHAR(1000)   NOT NULL                COMMENT '内容',
  `type`        TINYINT         NOT NULL DEFAULT 1      COMMENT '类型 1私信 2系统通知',
  `is_read`     TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已读 0否 1是',
  `deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`, `is_read`, `created_at`),
  KEY `idx_from_user` (`from_user_id`, `created_at`)
) ENGINE=InnoDB COMMENT='站内私信表';

-- ------------------------------------------------------------
-- 11. 减碳记录表（V1.0，T1 重点表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `carbon_record`;
CREATE TABLE `carbon_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `record_type`   TINYINT         NOT NULL                COMMENT '类型 1卖出（避免新品生产） 2买入（代替新品）',
  `product_id`    BIGINT UNSIGNED DEFAULT NULL            COMMENT '关联商品ID',
  `carbon_amount` DECIMAL(10,3)   NOT NULL DEFAULT 0.000  COMMENT '减碳量（kg CO2e）',
  `description`   VARCHAR(255)    DEFAULT NULL            COMMENT '说明',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `created_at`)
) ENGINE=InnoDB COMMENT='减碳记录表';

-- ------------------------------------------------------------
-- 12. 环保积分账户表（V1.0）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `carbon_credit`;
CREATE TABLE `carbon_credit` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `total_carbon`  DECIMAL(12,3)   NOT NULL DEFAULT 0.000  COMMENT '累计减碳总量（kg CO2e）',
  `total_points`  INT             NOT NULL DEFAULT 0      COMMENT '环保积分（1kg=10积分示例）',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB COMMENT='环保积分账户表';

-- ------------------------------------------------------------
-- 13. AI 估价记录表（可追溯/可审计）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_estimate_log`;
CREATE TABLE `ai_estimate_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`        BIGINT UNSIGNED DEFAULT NULL            COMMENT '发起用户ID',
  `product_id`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '关联商品ID',
  `category_id`    BIGINT UNSIGNED DEFAULT NULL            COMMENT '分类ID',
  `input_desc`     VARCHAR(1000)   DEFAULT NULL            COMMENT '估价输入描述',
  `base_price`     DECIMAL(10,2)   DEFAULT NULL            COMMENT '基准价',
  `condition_score` DECIMAL(4,2)   DEFAULT NULL            COMMENT '成色分 1~10',
  `age_months`     INT             DEFAULT NULL            COMMENT '使用月数',
  `heat_factor`    DECIMAL(4,3)    DEFAULT NULL            COMMENT '热度系数',
  `estimated_min`  DECIMAL(10,2)   NOT NULL                COMMENT '最低估价',
  `estimated_rec`  DECIMAL(10,2)   NOT NULL                COMMENT '推荐估价',
  `estimated_max`  DECIMAL(10,2)   NOT NULL                COMMENT '最高估价',
  `detail_json`    JSON            DEFAULT NULL            COMMENT '系数明细(JSON)',
  `source`         TINYINT         NOT NULL DEFAULT 1      COMMENT '来源 1规则引擎 2大模型 3混合',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `created_at`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='AI估价记录表';

-- ------------------------------------------------------------
-- 14. AI 自动填表草稿表（一键发布链路）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_publish_draft`;
CREATE TABLE `ai_publish_draft` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '草稿ID',
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '用户ID',
  `draft_json`  JSON            NOT NULL                COMMENT '草稿数据（识别出的标题/描述/分类/成色/图片/估价等）',
  `status`      TINYINT         NOT NULL DEFAULT 0      COMMENT '状态 0待确认 1已发布 2已过期 3已放弃',
  `product_id`  BIGINT UNSIGNED DEFAULT NULL            COMMENT '发布后商品ID',
  `deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`, `created_at`)
) ENGINE=InnoDB COMMENT='AI自动填表草稿表';

-- ------------------------------------------------------------
-- 种子数据：分类（含 AI 估价参数）+ 测试用户
-- ------------------------------------------------------------
INSERT INTO `category` (`id`, `parent_id`, `name`, `icon`, `base_price`, `depreciation_rate`, `heat_weight`, `sort_order`) VALUES
(1, 0, '数码电子', NULL, 3000.00, 0.1500, 1.200, 1),
(2, 0, '图书教材', NULL, 60.00,  0.3000, 0.800, 2),
(3, 0, '生活用品', NULL, 150.00, 0.2000, 1.000, 3),
(4, 0, '运动户外', NULL, 400.00, 0.1800, 0.900, 4),
(5, 0, '服饰鞋包', NULL, 200.00, 0.2500, 1.000, 5),
(6, 0, '美妆个护', NULL, 180.00, 0.2800, 0.850, 6),
(7, 0, '乐器',     NULL, 1200.00, 0.1200, 0.750, 7),
(8, 0, '其他',     NULL, 100.00, 0.2000, 0.800, 8),
(11, 1, '手机',    NULL, 3000.00, 0.1800, 1.300, 11),
(12, 1, '笔记本电脑', NULL, 4500.00, 0.1400, 1.100, 12),
(13, 1, '平板',    NULL, 2500.00, 0.1600, 1.000, 13),
(14, 1, '耳机/音响', NULL, 500.00, 0.2000, 1.100, 14),
(21, 2, '专业课教材', NULL, 60.00, 0.3000, 0.900, 21),
(22, 2, '考研/考证', NULL, 80.00, 0.2500, 0.950, 22),
(31, 3, '宿舍小家电', NULL, 200.00, 0.2200, 1.000, 31),
(32, 3, '家具收纳', NULL, 120.00, 0.2500, 0.900, 32);

INSERT INTO `user` (`id`, `student_no`, `username`, `nickname`, `credit_score`) VALUES
(1, '24320209', 'tianbo', '田博', 100),
(2, '24320201', 'chensihan', '陈思瀚', 100),
(3, '24320202', 'fanshengzhou', '范胜洲', 100),
(4, '24320203', 'xujiakai', '徐家凯', 100),
(5, '24320204', 'lintianchu', '林天楚', 100);

SET FOREIGN_KEY_CHECKS = 1;
