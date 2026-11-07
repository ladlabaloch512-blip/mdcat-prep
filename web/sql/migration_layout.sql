-- MDCAT Prep layout-round migration: per-user notification read state.
-- Run ONCE on an existing database. Additive only, safe with live data.
CREATE TABLE IF NOT EXISTS notification_reads (
    user_id INT UNSIGNED NOT NULL,
    notification_id INT UNSIGNED NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, notification_id),
    CONSTRAINT fk_nread_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nread_notif FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
