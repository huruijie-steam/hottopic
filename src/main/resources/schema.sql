-- HotTopic 热点监控工具建表脚本(MySQL 8)
-- 对应需求文档 §8 数据模型;application.yml 中 spring.sql.init.mode=always 自动执行

CREATE TABLE IF NOT EXISTS keyword (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    text        VARCHAR(255) NOT NULL,
    category    VARCHAR(64),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_keyword_text (text)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS hotspot (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    title             VARCHAR(512) NOT NULL,
    content           TEXT,
    url               VARCHAR(1024) NOT NULL,
    source            VARCHAR(32)  NOT NULL,
    source_id         VARCHAR(128),
    is_real           TINYINT(1)   NOT NULL DEFAULT 1,
    relevance         INT          NOT NULL DEFAULT 0,
    relevance_reason  VARCHAR(512),
    keyword_mentioned TINYINT(1),
    importance        VARCHAR(16)  NOT NULL DEFAULT 'low',
    summary           VARCHAR(512),
    view_count        BIGINT,
    like_count        BIGINT,
    retweet_count     BIGINT,
    reply_count       BIGINT,
    comment_count     BIGINT,
    quote_count       BIGINT,
    danmaku_count     BIGINT,
    author_name       VARCHAR(255),
    author_username   VARCHAR(255),
    author_avatar     VARCHAR(1024),
    author_followers  BIGINT,
    author_verified   TINYINT(1),
    published_at      DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    keyword_id        VARCHAR(36),
    UNIQUE KEY uk_hotspot_url_source (url(512), source),
    KEY idx_hotspot_created (created_at),
    KEY idx_hotspot_keyword (keyword_id),
    KEY idx_hotspot_source (source),
    KEY idx_hotspot_importance (importance)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS notification (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    type       VARCHAR(32) NOT NULL,
    title      VARCHAR(512) NOT NULL,
    content    TEXT,
    is_read    TINYINT(1)  NOT NULL DEFAULT 0,
    hotspot_id VARCHAR(36),
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notification_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS setting (
    id    VARCHAR(36) NOT NULL PRIMARY KEY,
    `key` VARCHAR(128) NOT NULL,
    value TEXT,
    UNIQUE KEY uk_setting_key (`key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
