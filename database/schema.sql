CREATE DATABASE IF NOT EXISTS simple_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'simple_user'@'localhost' IDENTIFIED BY 'simple_password';
GRANT ALL PRIVILEGES ON simple_db.* TO 'simple_user'@'localhost';
FLUSH PRIVILEGES;
USE simple_db;

-- Development schema. For an existing installation, the backend's JPA update
-- setting adds the new alert columns/table without requiring data deletion.
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  age INT NULL,
  disability VARCHAR(120) NULL,
  education VARCHAR(160) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL
);

CREATE TABLE IF NOT EXISTS alerts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(100) NOT NULL,
  level VARCHAR(30) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_at DATETIME NULL,
  updated_by BIGINT NULL,
  activated_at DATETIME NULL,
  resolved_at DATETIME NULL,
  expires_at DATETIME NULL,
  CONSTRAINT fk_alert_user FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_alert_updated_user FOREIGN KEY (updated_by) REFERENCES users(id),
  INDEX idx_alert_status (status),
  INDEX idx_alert_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS alert_status_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alert_id BIGINT NOT NULL,
  previous_status VARCHAR(30) NULL,
  new_status VARCHAR(30) NOT NULL,
  changed_by BIGINT NOT NULL,
  comment VARCHAR(500) NOT NULL DEFAULT '',
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_history_alert FOREIGN KEY (alert_id) REFERENCES alerts(id),
  CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(id),
  INDEX idx_alert_history_alert_id (alert_id),
  INDEX idx_alert_history_changed_at (changed_at)
);
