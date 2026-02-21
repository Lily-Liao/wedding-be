-- Wedding Backend Database Schema
-- V1: Initial Schema (MySQL 8.0+)

CREATE TABLE IF NOT EXISTS messages (
    id           CHAR(36)     NOT NULL,
    line_user_id VARCHAR(100),
    name         VARCHAR(255),
    content      TEXT         NOT NULL,
    created_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_messages_created_at   (created_at DESC),
    INDEX idx_messages_line_user_id (line_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS voting_sessions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    status     VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    started_at DATETIME(6),
    closed_at  DATETIME(6),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_voting_status CHECK (status IN ('WAITING', 'START', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO voting_sessions (status) VALUES ('WAITING');

CREATE TABLE IF NOT EXISTS votes (
    id                CHAR(36)     NOT NULL,
    voting_session_id BIGINT       NOT NULL,
    line_user_id      VARCHAR(100) NOT NULL,
    line_display_name VARCHAR(255),
    option_key        CHAR(1)      NOT NULL,
    created_at        DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE  uq_vote_per_session  (voting_session_id, line_user_id),
    INDEX   idx_votes_session_id  (voting_session_id),
    INDEX   idx_votes_option_key  (option_key),
    INDEX   idx_votes_line_user_id (line_user_id),
    CONSTRAINT chk_option_key   CHECK (option_key IN ('A', 'B', 'C', 'D')),
    CONSTRAINT fk_votes_session FOREIGN KEY (voting_session_id) REFERENCES voting_sessions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS winners (
    id                CHAR(36)     NOT NULL,
    vote_id           CHAR(36)     NOT NULL,
    line_user_id      VARCHAR(100) NOT NULL,
    line_display_name VARCHAR(255),
    option_key        CHAR(1)      NOT NULL,
    drawn_at          DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    cancelled_at      DATETIME(6),
    is_active         TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_winners_vote_id      (vote_id),
    INDEX idx_winners_line_user_id (line_user_id),
    INDEX idx_winners_is_active    (is_active),
    CONSTRAINT fk_winners_vote FOREIGN KEY (vote_id) REFERENCES votes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS media_schemes (
    id         CHAR(36)     NOT NULL,
    name       VARCHAR(255) NOT NULL,
    is_live    TINYINT(1)   NOT NULL DEFAULT 0,
    is_pinned  TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_media_schemes_is_live   (is_live),
    INDEX idx_media_schemes_is_pinned (is_pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS media_items (
    id           CHAR(36)     NOT NULL,
    scheme_id    CHAR(36)     NOT NULL,
    file_key     VARCHAR(500) NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size    BIGINT,
    sort_order   INT          NOT NULL DEFAULT 0,
    is_visible   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_media_items_scheme_id  (scheme_id),
    INDEX idx_media_items_sort_order (scheme_id, sort_order),
    CONSTRAINT fk_media_items_scheme FOREIGN KEY (scheme_id) REFERENCES media_schemes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS display_settings (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT,
    updated_at    DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE uq_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO display_settings (setting_key, setting_value) VALUES
    ('show_message_wall', 'true'),
    ('show_voting',       'true'),
    ('background_color',  '#FFFFFF'),
    ('font_color',        '#000000');

CREATE TABLE IF NOT EXISTS line_user_states (
    line_user_id  VARCHAR(100) NOT NULL,
    current_state VARCHAR(50)  NOT NULL DEFAULT 'IDLE',
    state_data    JSON,
    updated_at    DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (line_user_id),
    CONSTRAINT chk_user_state CHECK (current_state IN ('IDLE', 'AWAITING_MESSAGE', 'AWAITING_VOTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
