CREATE TABLE invoices (
    id          UUID PRIMARY KEY,
    session_id  VARCHAR(64)   NOT NULL UNIQUE,
    user_id     VARCHAR(32)   NOT NULL,
    amount      NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency    VARCHAR(3)    NOT NULL DEFAULT 'COP',
    status      VARCHAR(16)   NOT NULL CHECK (status IN ('PENDING','PAID','OVERDUE')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    due_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at     TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_invoices_user_status ON invoices (user_id, status);
CREATE INDEX idx_invoices_due_status  ON invoices (due_at, status);
