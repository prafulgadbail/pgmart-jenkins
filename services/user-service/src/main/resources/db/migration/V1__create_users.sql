CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          TEXT        NOT NULL UNIQUE,   -- always stored lower case
    name           TEXT        NOT NULL,
    phone          TEXT,
    password_hash  TEXT        NOT NULL,          -- BCrypt
    role           TEXT        NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One active code per email and purpose; a new code replaces the old one
CREATE TABLE otp_codes (
    email      TEXT        NOT NULL,
    purpose    TEXT        NOT NULL CHECK (purpose IN ('VERIFY', 'RESET')),
    code_hash  TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts   INT         NOT NULL DEFAULT 0,
    sent_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (email, purpose)
);

CREATE TABLE addresses (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name  TEXT        NOT NULL,
    mobile     TEXT        NOT NULL,
    line1      TEXT        NOT NULL,
    city       TEXT        NOT NULL,
    state      TEXT        NOT NULL,
    pincode    TEXT        NOT NULL,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX addresses_user_idx ON addresses (user_id);
