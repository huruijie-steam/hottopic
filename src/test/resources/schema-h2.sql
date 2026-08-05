-- H2 内存库建表(测试用,与 MySQL schema.sql 对应)
CREATE TABLE IF NOT EXISTS keyword (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    text        VARCHAR(255) NOT NULL,
    category    VARCHAR(64),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_keyword_text UNIQUE (text)
);

CREATE TABLE IF NOT EXISTS hotspot (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    title             VARCHAR(512) NOT NULL,
    content           CLOB,
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
    published_at      TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    keyword_id        VARCHAR(36),
    CONSTRAINT uk_hotspot_url_source UNIQUE (url, source)
);

CREATE TABLE IF NOT EXISTS notification (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    type       VARCHAR(32) NOT NULL,
    title      VARCHAR(512) NOT NULL,
    content    CLOB,
    is_read    TINYINT(1)  NOT NULL DEFAULT 0,
    hotspot_id VARCHAR(36),
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS setting (
    id    VARCHAR(36) NOT NULL PRIMARY KEY,
    `key` VARCHAR(128) NOT NULL,
    value CLOB,
    CONSTRAINT uk_setting_key UNIQUE (`key`)
);
