CREATE TABLE charge_sessions (
    id            VARCHAR(64) PRIMARY KEY,
    user_id       VARCHAR(32) NOT NULL,
    station_id    VARCHAR(32) NOT NULL,
    status        VARCHAR(16) NOT NULL,
    kwh_consumed  DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    started_at    TIMESTAMP(6) NOT NULL,
    completed_at  TIMESTAMP(6) NULL,
    INDEX idx_charge_sessions_user (user_id),
    INDEX idx_charge_sessions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
