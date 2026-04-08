create table if not exists payment_instrument (
    id bigserial primary key,
    customer_id bigint not null,
    payment_method_code varchar(64) not null,
    provider varchar(32) not null,
    provider_token_encrypted text not null,
    provider_token_fingerprint varchar(128) not null,
    display_label varchar(255),
    brand varchar(64),
    last4 varchar(4),
    expiry_month integer,
    expiry_year integer,
    gateway_customer_reference varchar(255),
    active boolean not null default true,
    default_instrument boolean not null default false,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

create unique index if not exists uk_payment_instrument_customer_fingerprint
    on payment_instrument(customer_id, provider_token_fingerprint);

create index if not exists idx_payment_instrument_customer
    on payment_instrument(customer_id);
