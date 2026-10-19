
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

CREATE TYPE user_role AS ENUM ('SME_OWNER', 'ADMIN', 'OPS');
CREATE TYPE kyc_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE kyc_document_type AS ENUM ('NATIONAL_ID', 'CERTIFICATE_OF_REGISTRATION', 'KRA_PIN_CERTIFICATE');
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'SENT', 'OVERDUE', 'PAID', 'CANCELLED');
CREATE TYPE reminder_channel AS ENUM ('SMS', 'EMAIL');
CREATE TYPE reminder_status AS ENUM ('SCHEDULED', 'SENT', 'FAILED');
CREATE TYPE transaction_type AS ENUM ('INVOICE_PAYMENT', 'ADVANCE_DISBURSEMENT', 'ADVANCE_REPAYMENT');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE advance_status AS ENUM ('REQUESTED', 'APPROVED', 'REJECTED', 'DISBURSED', 'REPAID', 'DEFAULTED');

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number      VARCHAR(20) NOT NULL UNIQUE,
    full_name         VARCHAR(150) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    role              user_role NOT NULL DEFAULT 'SME_OWNER',
    phone_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE otp_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number  VARCHAR(20) NOT NULL,
    code_hash     VARCHAR(255) NOT NULL,
    purpose       VARCHAR(40) NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    consumed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_phone ON otp_codes (phone_number, purpose);

CREATE TABLE businesses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id       UUID NOT NULL REFERENCES users (id),
    name                VARCHAR(200) NOT NULL,
    kra_pin             VARCHAR(20) NOT NULL,
    sector              VARCHAR(100),
    mpesa_shortcode     VARCHAR(20),
    kyc_status          kyc_status NOT NULL DEFAULT 'PENDING',
    kyc_reviewed_by     UUID REFERENCES users (id),
    kyc_review_reason   TEXT,
    kyc_reviewed_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_business_owner ON businesses (owner_user_id);
CREATE UNIQUE INDEX idx_business_kra_pin ON businesses (kra_pin);

CREATE TABLE kyc_documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    document_type kyc_document_type NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_kyc_business ON kyc_documents (business_id);

CREATE TABLE buyers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES businesses (id),
    name          VARCHAR(200) NOT NULL,
    phone_number  VARCHAR(20),
    email         VARCHAR(200),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_buyer_business ON buyers (business_id);

CREATE TABLE invoices (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id       UUID NOT NULL REFERENCES businesses (id),
    buyer_id          UUID NOT NULL REFERENCES buyers (id),
    invoice_number    VARCHAR(40) NOT NULL,
    status            invoice_status NOT NULL DEFAULT 'DRAFT',
    issue_date        DATE NOT NULL,
    due_date          DATE NOT NULL,
    subtotal          NUMERIC(14,2) NOT NULL,
    tax_amount        NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount      NUMERIC(14,2) NOT NULL,
    amount_paid       NUMERIC(14,2) NOT NULL DEFAULT 0,
    shareable_token   VARCHAR(64) NOT NULL,
    sent_at           TIMESTAMPTZ,
    paid_at           TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 0,  -- optimistic locking: reconciliation races
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invoice_number_per_business UNIQUE (business_id, invoice_number)
);
CREATE UNIQUE INDEX idx_invoice_share_token ON invoices (shareable_token);
CREATE INDEX idx_invoice_business ON invoices (business_id);
CREATE INDEX idx_invoice_status_due ON invoices (status, due_date);

CREATE TABLE invoice_line_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    UUID NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    description   VARCHAR(300) NOT NULL,
    quantity      NUMERIC(12,2) NOT NULL,
    unit_price    NUMERIC(14,2) NOT NULL,
    tax_rate      NUMERIC(5,4) NOT NULL DEFAULT 0,
    line_total    NUMERIC(14,2) NOT NULL
);
CREATE INDEX idx_line_item_invoice ON invoice_line_items (invoice_id);

CREATE TABLE reminders (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id     UUID NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    channel        reminder_channel NOT NULL,
    status         reminder_status NOT NULL DEFAULT 'SCHEDULED',
    scheduled_for  TIMESTAMPTZ NOT NULL,
    sent_at        TIMESTAMPTZ,
    failure_reason TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reminder_invoice ON reminders (invoice_id);
CREATE INDEX idx_reminder_due ON reminders (status, scheduled_for);

CREATE TABLE lender_partners (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    api_base_url VARCHAR(300) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE risk_scores (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES businesses (id),
    score         INTEGER NOT NULL,
    factors       JSONB NOT NULL DEFAULT '{}',
    computed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_risk_score_business ON risk_scores (business_id, computed_at DESC);

CREATE TABLE advances (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id         UUID NOT NULL REFERENCES invoices (id),
    business_id        UUID NOT NULL REFERENCES businesses (id),
    lender_partner_id  UUID REFERENCES lender_partners (id),
    requested_amount   NUMERIC(14,2) NOT NULL,
    fee_amount         NUMERIC(14,2) NOT NULL,
    net_payout         NUMERIC(14,2) NOT NULL,
    status             advance_status NOT NULL DEFAULT 'REQUESTED',
    requested_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    decisioned_at      TIMESTAMPTZ,
    disbursed_at       TIMESTAMPTZ,
    repaid_at          TIMESTAMPTZ,
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_one_active_advance_per_invoice UNIQUE (invoice_id)
);
CREATE INDEX idx_advance_business ON advances (business_id);
CREATE INDEX idx_advance_status ON advances (status);

CREATE TABLE transactions (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id             UUID REFERENCES invoices (id),
    advance_id             UUID REFERENCES advances (id),
    type                   transaction_type NOT NULL,
    status                 transaction_status NOT NULL DEFAULT 'PENDING',
    amount                 NUMERIC(14,2) NOT NULL,
    counterparty_phone     VARCHAR(20),
    mpesa_receipt_number   VARCHAR(40),
    mpesa_checkout_request_id VARCHAR(60),
    raw_callback_payload   JSONB,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at             TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_txn_mpesa_receipt ON transactions (mpesa_receipt_number) WHERE mpesa_receipt_number IS NOT NULL;
CREATE INDEX idx_txn_checkout_request ON transactions (mpesa_checkout_request_id) WHERE mpesa_checkout_request_id IS NOT NULL;
CREATE INDEX idx_txn_invoice ON transactions (invoice_id);
CREATE INDEX idx_txn_advance ON transactions (advance_id);

-- Append-only by convention (enforced at the application layer): no UPDATE/DELETE grants in production roles.
CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users (id),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     UUID,
    metadata      JSONB NOT NULL DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id);
