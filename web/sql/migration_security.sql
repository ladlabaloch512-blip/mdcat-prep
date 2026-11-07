-- MDCAT Prep security migration: password reset + sign-in throttling.
-- Run ONCE on an existing database. Additive only, safe with live data.
CREATE TABLE IF NOT EXISTS password_resets (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    code_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reset_user (user_id),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_throttle (
    id_key VARCHAR(191) NOT NULL,
    fail_count INT NOT NULL DEFAULT 0,
    last_fail_at DATETIME NULL,
    locked_until DATETIME NULL,
    PRIMARY KEY (id_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
