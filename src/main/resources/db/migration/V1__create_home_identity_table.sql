CREATE TABLE IF NOT EXISTS home_identity_verification (
    home_identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    home_id BIGINT NOT NULL,
    user_id BIGINT,
    name VARCHAR(20),
    birth_date DATE NOT NULL,
    identity_verified_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_home_id (home_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;