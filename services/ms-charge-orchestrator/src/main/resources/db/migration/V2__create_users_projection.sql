CREATE TABLE users (
    id                          VARCHAR(32) PRIMARY KEY,
    overdue_days                INT NOT NULL DEFAULT 0,
    has_active_payment_method   BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated_at             TIMESTAMP(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
