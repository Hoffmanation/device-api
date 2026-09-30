CREATE TABLE devices (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    brand       VARCHAR(255) NOT NULL,
    state       VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_devices_brand ON devices (brand);
CREATE INDEX idx_devices_state ON devices (state);
CREATE INDEX idx_devices_brand_state ON devices (brand, state);
CREATE INDEX idx_devices_created_at ON devices (created_at DESC);


