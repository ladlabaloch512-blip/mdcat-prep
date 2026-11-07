-- MDCAT Prep - extras migration (Daily Reward, WhatsApp Channel,
-- Analytics support, Question of the Day, personal study goals).
-- Run ONCE on an existing database created from the original schema.sql.
-- Safe on MySQL/MariaDB shared hosting: statements are additive only;
-- no existing data is changed or removed.
SET NAMES utf8mb4;

ALTER TABLE users ADD COLUMN coins INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN personal_goal INT NULL;

CREATE TABLE IF NOT EXISTS reward_claims (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id INT UNSIGNED NOT NULL,
  claim_date DATE NOT NULL,
  coins INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reward_day (user_id, claim_date),
  KEY idx_reward_user (user_id),
  CONSTRAINT fk_reward_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO settings (`key`, `value`) VALUES
('daily_reward_coins', '20'),
('rewards_enabled', '1'),
('whatsapp_channel_url', '')
ON DUPLICATE KEY UPDATE `key` = `key`;
