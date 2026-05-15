CREATE TABLE user_debts (
    user_id                     VARCHAR(32) PRIMARY KEY,
    overdue_days                INTEGER NOT NULL DEFAULT 0 CHECK (overdue_days >= 0),
    has_active_payment_method   BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
);
