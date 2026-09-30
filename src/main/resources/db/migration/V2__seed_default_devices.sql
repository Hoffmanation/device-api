INSERT INTO devices (id, name, brand, state, created_at, updated_at)
VALUES
    ('0f1a2b3c-4d5e-4601-8a01-111111111111', 'eSIM Gateway X1', 'Motorola', 'AVAILABLE', '2026-09-28T08:00:00Z', '2026-09-28T08:00:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-222222222222', '5G Edge Router X2', 'Nokia', 'IN_USE', '2026-09-28T08:05:00Z', '2026-09-28T08:05:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-333333333333', 'Industrial LTE Router Pro', 'Samsung', 'INACTIVE', '2026-09-28T08:10:00Z', '2026-09-28T08:10:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-444444444444', 'Remote SIM Hub M2M', 'Motorola', 'AVAILABLE', '2026-09-28T08:15:00Z', '2026-09-28T08:15:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-555555555555', 'NB-IoT Tracker One', 'Samsung', 'AVAILABLE', '2026-09-28T08:20:00Z', '2026-09-28T08:20:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-666666666666', '5G Provisioning Gateway', 'Nokia', 'IN_USE', '2026-09-28T08:25:00Z', '2026-09-28T08:25:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-777777777777', 'Industrial SIM Gateway', 'Motorola', 'INACTIVE', '2026-09-28T08:30:00Z', '2026-09-28T08:30:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-888888888888', 'Private Cellular Bridge', 'Samsung', 'AVAILABLE', '2026-09-28T08:35:00Z', '2026-09-28T08:35:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-999999999999', 'Fleet Connectivity Node', 'Nokia', 'INACTIVE', '2026-09-28T08:40:00Z', '2026-09-28T08:40:00Z'),
    ('0f1a2b3c-4d5e-4601-8a01-aaaaaaaaaaaa', 'Multi-SIM Edge Controller', 'Motorola', 'AVAILABLE', '2026-09-28T08:45:00Z', '2026-09-28T08:45:00Z')
ON CONFLICT (id) DO NOTHING;


