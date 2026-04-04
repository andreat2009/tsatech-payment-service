ALTER TABLE payment ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(15,4) NOT NULL DEFAULT 0;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS last_reconciled_at TIMESTAMPTZ;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS last_webhook_at TIMESTAMPTZ;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS last_provider_sync_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    event_source VARCHAR(64) NOT NULL,
    status VARCHAR(64),
    provider_status VARCHAR(128),
    provider_reference VARCHAR(255),
    amount NUMERIC(15,4),
    currency VARCHAR(3),
    failure_code VARCHAR(128),
    failure_reason VARCHAR(1000),
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_payment ON payment_transaction(payment_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_order ON payment_transaction(order_id, id DESC);
