-- V1 used native PostgreSQL ENUM types for these nine columns. Hibernate's default
-- @Enumerated(EnumType.STRING) mapping binds enum values as plain VARCHAR parameters, and
-- Postgres does not implicitly cast a bound VARCHAR parameter to a native enum type (only a
-- literal written directly in SQL text gets that implicit cast) — every INSERT/UPDATE touching
-- an enum column fails with "column X is of type Y but expression is of type character varying".
-- Fixing this at the schema level (VARCHAR + CHECK) rather than annotating every entity avoids
-- depending on Hibernate-version-specific native-enum binding support, and needs no Java changes
-- since the entities already declare these fields as @Enumerated(EnumType.STRING) + @Column(length=N).

-- users.role
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::text;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'SME_OWNER';
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('SME_OWNER','ADMIN','OPS'));

-- businesses.kyc_status
ALTER TABLE businesses ALTER COLUMN kyc_status DROP DEFAULT;
ALTER TABLE businesses ALTER COLUMN kyc_status TYPE VARCHAR(20) USING kyc_status::text;
ALTER TABLE businesses ALTER COLUMN kyc_status SET DEFAULT 'PENDING';
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_kyc_status CHECK (kyc_status IN ('PENDING','APPROVED','REJECTED'));

-- kyc_documents.document_type
ALTER TABLE kyc_documents ALTER COLUMN document_type TYPE VARCHAR(40) USING document_type::text;
ALTER TABLE kyc_documents ADD CONSTRAINT chk_kyc_documents_type CHECK (document_type IN ('NATIONAL_ID','CERTIFICATE_OF_REGISTRATION','KRA_PIN_CERTIFICATE'));

-- invoices.status
ALTER TABLE invoices ALTER COLUMN status DROP DEFAULT;
ALTER TABLE invoices ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE invoices ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_status CHECK (status IN ('DRAFT','SENT','OVERDUE','PAID','CANCELLED'));

-- reminders.channel
ALTER TABLE reminders ALTER COLUMN channel TYPE VARCHAR(10) USING channel::text;
ALTER TABLE reminders ADD CONSTRAINT chk_reminders_channel CHECK (channel IN ('SMS','EMAIL'));

-- reminders.status
ALTER TABLE reminders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE reminders ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE reminders ALTER COLUMN status SET DEFAULT 'SCHEDULED';
ALTER TABLE reminders ADD CONSTRAINT chk_reminders_status CHECK (status IN ('SCHEDULED','SENT','FAILED'));

-- transactions.type
ALTER TABLE transactions ALTER COLUMN type TYPE VARCHAR(30) USING type::text;
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_type CHECK (type IN ('INVOICE_PAYMENT','ADVANCE_DISBURSEMENT','ADVANCE_REPAYMENT'));

-- transactions.status
ALTER TABLE transactions ALTER COLUMN status DROP DEFAULT;
ALTER TABLE transactions ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE transactions ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING','SUCCESS','FAILED'));

-- advances.status
ALTER TABLE advances ALTER COLUMN status DROP DEFAULT;
ALTER TABLE advances ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE advances ALTER COLUMN status SET DEFAULT 'REQUESTED';
ALTER TABLE advances ADD CONSTRAINT chk_advances_status CHECK (status IN ('REQUESTED','APPROVED','REJECTED','DISBURSED','REPAID','DEFAULTED'));

-- The native enum types are no longer referenced by any column, safe to drop.
DROP TYPE user_role;
DROP TYPE kyc_status;
DROP TYPE kyc_document_type;
DROP TYPE invoice_status;
DROP TYPE reminder_channel;
DROP TYPE reminder_status;
DROP TYPE transaction_type;
DROP TYPE transaction_status;
DROP TYPE advance_status;