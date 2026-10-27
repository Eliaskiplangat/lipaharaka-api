BEGIN;

-- Clean previous test data for this scenario
DELETE FROM invoices WHERE business_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444'
);

DELETE FROM buyers WHERE business_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444'
);

DELETE FROM businesses WHERE id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444'
);

DELETE FROM users WHERE id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'dddddddd-dddd-dddd-dddd-dddddddddddd'
);

-- 1) Low-risk user and business
INSERT INTO users (id, phone_number, full_name, password_hash, role, phone_verified, active, created_at)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '+254700000001',
    'Low Risk Owner',
    '$2a$10$dummyhashforlocaltestonly',
    'SME_OWNER',
    true,
    true,
    now() - interval '18 months'
);

INSERT INTO businesses (id, owner_user_id, name, kra_pin, sector, kyc_status, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Low Risk Traders',
    'A000000000Z',
    'Retail',
    'APPROVED',
    now() - interval '18 months'
);

INSERT INTO buyers (id, business_id, name, phone_number, email, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Alice Retailer', '+254711111001', 'alice@example.com', now() - interval '10 months'),
    ('10000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Beta Foods', '+254711111002', 'beta@example.com', now() - interval '9 months'),
    ('10000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Gamma Mart', '+254711111003', 'gamma@example.com', now() - interval '8 months');

INSERT INTO invoices (
    id, business_id, buyer_id, invoice_number, status, issue_date, due_date,
    subtotal, tax_amount, total_amount, amount_paid, shareable_token,
    sent_at, paid_at, created_at, updated_at
)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        '10000000-0000-0000-0000-000000000001',
        'LOW-INV-001',
        'PAID',
        current_date - interval '60 days',
        current_date - interval '30 days',
        15000, 1500, 16500, 16500,
        'low-inv-001-token-1234567890',
        now() - interval '60 days',
        now() - interval '29 days',
        now() - interval '60 days',
        now() - interval '29 days'
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        '11111111-1111-1111-1111-111111111111',
        '10000000-0000-0000-0000-000000000002',
        'LOW-INV-002',
        'PAID',
        current_date - interval '50 days',
        current_date - interval '20 days',
        21000, 2100, 23100, 23100,
        'low-inv-002-token-1234567891',
        now() - interval '50 days',
        now() - interval '19 days',
        now() - interval '50 days',
        now() - interval '19 days'
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        '11111111-1111-1111-1111-111111111111',
        '10000000-0000-0000-0000-000000000003',
        'LOW-INV-003',
        'PAID',
        current_date - interval '40 days',
        current_date - interval '10 days',
        18000, 1800, 19800, 19800,
        'low-inv-003-token-1234567892',
        now() - interval '40 days',
        now() - interval '9 days',
        now() - interval '40 days',
        now() - interval '9 days'
    ),
    (
        '20000000-0000-0000-0000-000000000004',
        '11111111-1111-1111-1111-111111111111',
        '10000000-0000-0000-0000-000000000001',
        'LOW-INV-004',
        'PAID',
        current_date - interval '35 days',
        current_date - interval '5 days',
        12000, 1200, 13200, 13200,
        'low-inv-004-token-1234567893',
        now() - interval '35 days',
        now() - interval '4 days',
        now() - interval '35 days',
        now() - interval '4 days'
    );

-- 2) Moderate-risk user and business
INSERT INTO users (id, phone_number, full_name, password_hash, role, phone_verified, active, created_at)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '+254700000002',
    'Moderate Risk Owner',
    '$2a$10$dummyhashforlocaltestonly',
    'SME_OWNER',
    true,
    true,
    now() - interval '12 months'
);

INSERT INTO businesses (id, owner_user_id, name, kra_pin, sector, kyc_status, created_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'Moderate Risk Store',
    'B000000000Z',
    'Wholesale',
    'APPROVED',
    now() - interval '12 months'
);

INSERT INTO buyers (id, business_id, name, phone_number, email, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000010', '22222222-2222-2222-2222-222222222222', 'Malia Supply', '+254711111010', 'malia@example.com', now() - interval '9 months'),
    ('10000000-0000-0000-0000-000000000011', '22222222-2222-2222-2222-222222222222', 'North Goods', '+254711111011', 'north@example.com', now() - interval '8 months');

INSERT INTO invoices (
    id, business_id, buyer_id, invoice_number, status, issue_date, due_date,
    subtotal, tax_amount, total_amount, amount_paid, shareable_token,
    sent_at, paid_at, created_at, updated_at
)
VALUES
    (
        '20000000-0000-0000-0000-000000000101',
        '22222222-2222-2222-2222-222222222222',
        '10000000-0000-0000-0000-000000000010',
        'MOD-INV-001',
        'PAID',
        current_date - interval '55 days',
        current_date - interval '25 days',
        22000, 2200, 24200, 24200,
        'moderate-inv-001-abc',
        now() - interval '55 days',
        now() - interval '16 days',
        now() - interval '55 days',
        now() - interval '16 days'
    ),
    (
        '20000000-0000-0000-0000-000000000102',
        '22222222-2222-2222-2222-222222222222',
        '10000000-0000-0000-0000-000000000011',
        'MOD-INV-002',
        'PAID',
        current_date - interval '48 days',
        current_date - interval '18 days',
        26000, 2600, 28600, 28600,
        'moderate-inv-002-abc',
        now() - interval '48 days',
        now() - interval '10 days',
        now() - interval '48 days',
        now() - interval '10 days'
    ),
    (
        '20000000-0000-0000-0000-000000000103',
        '22222222-2222-2222-2222-222222222222',
        '10000000-0000-0000-0000-000000000010',
        'MOD-INV-003',
        'OVERDUE',
        current_date - interval '70 days',
        current_date - interval '15 days',
        30000, 3000, 33000, 0,
        'moderate-inv-003-abc',
        now() - interval '70 days',
        NULL,
        now() - interval '70 days',
        now() - interval '1 day'
    ),
    (
        '20000000-0000-0000-0000-000000000104',
        '22222222-2222-2222-2222-222222222222',
        '10000000-0000-0000-0000-000000000011',
        'MOD-INV-004',
        'PAID',
        current_date - interval '30 days',
        current_date - interval '5 days',
        18000, 1800, 19800, 19800,
        'moderate-inv-004-abc',
        now() - interval '30 days',
        now() - interval '2 days',
        now() - interval '30 days',
        now() - interval '2 days'
    );

-- 3) High-risk user and business
INSERT INTO users (id, phone_number, full_name, password_hash, role, phone_verified, active, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '+254700000003',
    'High Risk Owner',
    '$2a$10$dummyhashforlocaltestonly',
    'SME_OWNER',
    true,
    true,
    now() - interval '9 months'
);

INSERT INTO businesses (id, owner_user_id, name, kra_pin, sector, kyc_status, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'High Risk Supplies',
    'C000000000Z',
    'Logistics',
    'APPROVED',
    now() - interval '9 months'
);

INSERT INTO buyers (id, business_id, name, phone_number, email, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000020', '33333333-3333-3333-3333-333333333333', 'Oscar Logistics', '+254711111020', 'oscar@example.com', now() - interval '8 months'),
    ('10000000-0000-0000-0000-000000000021', '33333333-3333-3333-3333-333333333333', 'Quick Cargo', '+254711111021', 'quick@example.com', now() - interval '7 months');

INSERT INTO invoices (
    id, business_id, buyer_id, invoice_number, status, issue_date, due_date,
    subtotal, tax_amount, total_amount, amount_paid, shareable_token,
    sent_at, paid_at, created_at, updated_at
)
VALUES
    (
        '20000000-0000-0000-0000-000000000201',
        '33333333-3333-3333-3333-333333333333',
        '10000000-0000-0000-0000-000000000020',
        'HIGH-INV-001',
        'PAID',
        current_date - interval '80 days',
        current_date - interval '20 days',
        42000, 4200, 46200, 46200,
        'high-inv-001-xyz',
        now() - interval '80 days',
        now() - interval '12 days',
        now() - interval '80 days',
        now() - interval '12 days'
    ),
    (
        '20000000-0000-0000-0000-000000000202',
        '33333333-3333-3333-3333-333333333333',
        '10000000-0000-0000-0000-000000000021',
        'HIGH-INV-002',
        'OVERDUE',
        current_date - interval '95 days',
        current_date - interval '35 days',
        50000, 5000, 55000, 0,
        'high-inv-002-xyz',
        now() - interval '95 days',
        NULL,
        now() - interval '95 days',
        now() - interval '1 day'
    ),
    (
        '20000000-0000-0000-0000-000000000203',
        '33333333-3333-3333-3333-333333333333',
        '10000000-0000-0000-0000-000000000020',
        'HIGH-INV-003',
        'OVERDUE',
        current_date - interval '90 days',
        current_date - interval '30 days',
        47000, 4700, 51700, 0,
        'high-inv-003-xyz',
        now() - interval '90 days',
        NULL,
        now() - interval '90 days',
        now() - interval '1 day'
    ),
    (
        '20000000-0000-0000-0000-000000000204',
        '33333333-3333-3333-3333-333333333333',
        '10000000-0000-0000-0000-000000000021',
        'HIGH-INV-004',
        'PAID',
        current_date - interval '50 days',
        current_date - interval '8 days',
        27000, 2700, 29700, 29700,
        'high-inv-004-xyz',
        now() - interval '50 days',
        now() - interval '3 days',
        now() - interval '50 days',
        now() - interval '3 days'
    );

-- 4) Critical-risk user and business
INSERT INTO users (id, phone_number, full_name, password_hash, role, phone_verified, active, created_at)
VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '+254700000004',
    'Critical Risk Owner',
    '$2a$10$dummyhashforlocaltestonly',
    'SME_OWNER',
    true,
    true,
    now() - interval '6 months'
);

INSERT INTO businesses (id, owner_user_id, name, kra_pin, sector, kyc_status, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'Critical Risk Enterprises',
    'D000000000Z',
    'Construction',
    'APPROVED',
    now() - interval '6 months'
);

INSERT INTO buyers (id, business_id, name, phone_number, email, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000030', '44444444-4444-4444-4444-444444444444', 'Stone Works', '+254711111030', 'stone@example.com', now() - interval '6 months'),
    ('10000000-0000-0000-0000-000000000031', '44444444-4444-4444-4444-444444444444', 'Build Fast Ltd', '+254711111031', 'buildfast@example.com', now() - interval '5 months');

INSERT INTO invoices (
    id, business_id, buyer_id, invoice_number, status, issue_date, due_date,
    subtotal, tax_amount, total_amount, amount_paid, shareable_token,
    sent_at, paid_at, created_at, updated_at
)
VALUES
    (
        '20000000-0000-0000-0000-000000000301',
        '44444444-4444-4444-4444-444444444444',
        '10000000-0000-0000-0000-000000000030',
        'CRIT-INV-001',
        'OVERDUE',
        current_date - interval '120 days',
        current_date - interval '60 days',
        80000, 8000, 88000, 0,
        'critical-inv-001-zzz',
        now() - interval '120 days',
        NULL,
        now() - interval '120 days',
        now() - interval '1 day'
    ),
    (
        '20000000-0000-0000-0000-000000000302',
        '44444444-4444-4444-4444-444444444444',
        '10000000-0000-0000-0000-000000000031',
        'CRIT-INV-002',
        'OVERDUE',
        current_date - interval '110 days',
        current_date - interval '50 days',
        70000, 7000, 77000, 0,
        'critical-inv-002-zzz',
        now() - interval '110 days',
        NULL,
        now() - interval '110 days',
        now() - interval '1 day'
    ),
    (
        '20000000-0000-0000-0000-000000000303',
        '44444444-4444-4444-4444-444444444444',
        '10000000-0000-0000-0000-000000000030',
        'CRIT-INV-003',
        'PAID',
        current_date - interval '90 days',
        current_date - interval '18 days',
        65000, 6500, 71500, 71500,
        'critical-inv-003-zzz',
        now() - interval '90 days',
        now() - interval '10 days',
        now() - interval '90 days',
        now() - interval '10 days'
    ),
    (
        '20000000-0000-0000-0000-000000000304',
        '44444444-4444-4444-4444-444444444444',
        '10000000-0000-0000-0000-000000000031',
        'CRIT-INV-004',
        'OVERDUE',
        current_date - interval '100 days',
        current_date - interval '40 days',
        76000, 7600, 83600, 0,
        'critical-inv-004-zzz',
        now() - interval '100 days',
        NULL,
        now() - interval '100 days',
        now() - interval '1 day'
    );

COMMIT;