CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(100) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `avatar` VARCHAR(500) DEFAULT '',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-active',
  `balance` DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
  `total_quota` BIGINT NOT NULL DEFAULT 0 COMMENT 'total token quota',
  `used_quota` BIGINT NOT NULL DEFAULT 0 COMMENT 'used token quota',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  INDEX idx_username (`username`),
  INDEX idx_email (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `key_value` VARCHAR(100) NOT NULL UNIQUE,
  `name` VARCHAR(100) NOT NULL DEFAULT 'default',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-active',
  `rate_limit_rpm` INT NOT NULL DEFAULT 60,
  `rate_limit_tpm` INT NOT NULL DEFAULT 100000,
  `total_quota` BIGINT NOT NULL DEFAULT -1 COMMENT '-1 means unlimited',
  `used_quota` BIGINT NOT NULL DEFAULT 0,
  `expires_at` DATETIME DEFAULT NULL,
  `last_used_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  INDEX idx_user_id (`user_id`),
  INDEX idx_key_value (`key_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `channel` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(50) NOT NULL COMMENT 'openai,claude,gemini,zhipu,qwen,wenxin,deepseek etc',
  `base_url` VARCHAR(500) NOT NULL,
  `api_key` VARCHAR(500) NOT NULL,
  `models` TEXT NOT NULL COMMENT 'JSON array of supported model IDs',
  `priority` INT NOT NULL DEFAULT 0 COMMENT 'higher=more priority',
  `weight` INT NOT NULL DEFAULT 1 COMMENT 'for weighted load balancing',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-active, 2-error',
  `max_retries` INT NOT NULL DEFAULT 3,
  `timeout_ms` INT NOT NULL DEFAULT 30000,
  `response_time_ms` INT DEFAULT 0,
  `success_rate` DECIMAL(5,2) DEFAULT 100.00,
  `total_requests` BIGINT NOT NULL DEFAULT 0,
  `failed_requests` BIGINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  INDEX idx_type (`type`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `model_price` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `model_id` VARCHAR(100) NOT NULL UNIQUE COMMENT 'e.g. gpt-4, claude-3-opus',
  `model_name` VARCHAR(200) NOT NULL COMMENT 'display name',
  `input_price` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT 'price per 1K tokens (CNY)',
  `output_price` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT 'price per 1K tokens (CNY)',
  `price_multiplier` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT 'markup multiplier',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_model_id (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plan` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT '',
  `price` DECIMAL(10,2) NOT NULL,
  `duration_days` INT NOT NULL COMMENT 'subscription duration in days',
  `token_quota` BIGINT NOT NULL DEFAULT -1 COMMENT 'token quota, -1=unlimited',
  `rate_limit_rpm` INT NOT NULL DEFAULT 60,
  `rate_limit_tpm` INT NOT NULL DEFAULT 100000,
  `max_api_keys` INT NOT NULL DEFAULT 5,
  `features` TEXT COMMENT 'JSON array of feature strings',
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `subscription` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active,expired,cancelled',
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `auto_renew` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(30) NOT NULL COMMENT 'recharge,subscription',
  `amount` DECIMAL(12,4) NOT NULL,
  `original_amount` DECIMAL(12,4) NOT NULL,
  `discount_amount` DECIMAL(12,4) NOT NULL DEFAULT 0,
  `payment_method` VARCHAR(30) COMMENT 'alipay,wechat',
  `payment_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending,paid,failed,refunded',
  `payment_no` VARCHAR(100) DEFAULT NULL COMMENT 'third-party payment number',
  `paid_at` DATETIME DEFAULT NULL,
  `coupon_id` BIGINT DEFAULT NULL,
  `plan_id` BIGINT DEFAULT NULL COMMENT 'plan id for subscription orders',
  `description` VARCHAR(500) DEFAULT '',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_order_no (`order_no`),
  INDEX idx_payment_status (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `balance_transaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(30) NOT NULL COMMENT 'recharge,consume,refund,gift,coupon',
  `amount` DECIMAL(12,4) NOT NULL COMMENT 'positive=credit, negative=debit',
  `balance_before` DECIMAL(12,4) NOT NULL,
  `balance_after` DECIMAL(12,4) NOT NULL,
  `description` VARCHAR(500) DEFAULT '',
  `order_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_type (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'fixed,percent',
  `value` DECIMAL(10,2) NOT NULL COMMENT 'fixed amount or percentage',
  `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'minimum order amount',
  `max_discount` DECIMAL(10,2) DEFAULT NULL COMMENT 'max discount for percent type',
  `total_count` INT NOT NULL DEFAULT -1 COMMENT '-1=unlimited',
  `used_count` INT NOT NULL DEFAULT 0,
  `per_user_limit` INT NOT NULL DEFAULT 1,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  INDEX idx_code (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coupon_id` BIGINT NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'unused' COMMENT 'unused,used,expired',
  `used_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_coupon_id (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `request_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `api_key_id` BIGINT DEFAULT NULL,
  `channel_id` BIGINT DEFAULT NULL,
  `model` VARCHAR(100) NOT NULL,
  `request_type` VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT 'chat,completion,embedding,image',
  `prompt_tokens` INT NOT NULL DEFAULT 0,
  `completion_tokens` INT NOT NULL DEFAULT 0,
  `total_tokens` INT NOT NULL DEFAULT 0,
  `cost` DECIMAL(12,6) NOT NULL DEFAULT 0,
  `latency_ms` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT 'success,fail,timeout',
  `error_message` TEXT DEFAULT NULL,
  `ip_address` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (`user_id`),
  INDEX idx_model (`model`),
  INDEX idx_created_at (`created_at`),
  INDEX idx_channel_id (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `user` (`username`, `email`, `password`, `role`, `status`, `balance`) VALUES
('admin', 'admin@airelay.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 1, 0);

INSERT IGNORE INTO `plan` (`name`, `description`, `price`, `duration_days`, `token_quota`, `rate_limit_rpm`, `rate_limit_tpm`, `max_api_keys`, `features`, `is_default`, `sort_order`, `status`) VALUES
('免费版', '体验基础模型能力', 0.00, 365, 100000, 10, 10000, 1, '["基础模型访问","10RPM限速"]', 1, 0, 1),
('基础版', '适合个人开发者', 29.90, 30, 5000000, 60, 100000, 5, '["全部模型访问","60RPM限速","优先渠道"]', 0, 1, 1),
('专业版', '适合团队和企业', 99.90, 30, -1, 120, 200000, 20, '["全部模型访问","120RPM限速","优先渠道","专属客服"]', 0, 2, 1),
('企业版', '大规模商业使用', 299.90, 30, -1, 300, 500000, 100, '["全部模型访问","300RPM限速","最高优先级","专属客服","定制模型"]', 0, 3, 1);
