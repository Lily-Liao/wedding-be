-- V6: Store LINE user profiles (fetched on first message)
CREATE TABLE IF NOT EXISTS line_users (
    line_user_id VARCHAR(100)  NOT NULL,
    display_name VARCHAR(255),
    picture_url  VARCHAR(500),
    language     VARCHAR(10),
    created_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (line_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
