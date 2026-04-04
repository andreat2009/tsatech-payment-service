ALTER TABLE payment ADD COLUMN IF NOT EXISTS method_code VARCHAR(64);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS method_label VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_environment VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_status VARCHAR(64);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS redirect_url VARCHAR(2000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS approval_url VARCHAR(2000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS return_url VARCHAR(2000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS cancel_url VARCHAR(2000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS failure_code VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS lightbox_script_url VARCHAR(2000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS lightbox_shop_login VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS lightbox_payment_token VARCHAR(255);

CREATE TABLE IF NOT EXISTS payment_method (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    payment_flow VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payment_method_active_sort ON payment_method(active, sort_order, code);

INSERT INTO payment_method(code, display_name, provider, payment_flow, description, active, sort_order)
VALUES
    ('cash_on_delivery', 'Cash on delivery', 'OFFLINE', 'OFFLINE', 'Pay when the order is delivered.', TRUE, 10),
    ('bank_transfer', 'Bank transfer', 'OFFLINE', 'OFFLINE', 'Manual offline bank transfer.', TRUE, 20),
    ('paypal', 'PayPal', 'PAYPAL', 'REDIRECT', 'PayPal Checkout via Orders API.', TRUE, 30),
    ('fabrick_gateway', 'Banca Sella / Fabrick', 'FABRICK', 'LIGHTBOX', 'Hosted checkout through Fabrick Payment Orchestra.', TRUE, 40)
ON CONFLICT (code) DO NOTHING;
