CREATE SEQUENCE order_number_seq START 100001;

CREATE TABLE orders (
    id                  TEXT PRIMARY KEY,                  -- UUID, also sent to catalog-service for the stock
    number              TEXT        NOT NULL UNIQUE,       -- what the customer sees: PGM-100001
    user_id             BIGINT      NOT NULL,
    customer_name       TEXT        NOT NULL,
    customer_email      TEXT        NOT NULL,
    status              TEXT        NOT NULL CHECK (status IN
                            ('PAYMENT_PENDING', 'PLACED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
    payment_method      TEXT        NOT NULL CHECK (payment_method IN ('COD', 'RAZORPAY')),
    payment_status      TEXT        NOT NULL CHECK (payment_status IN
                            ('COD_DUE', 'PENDING', 'PAID', 'REFUND_PENDING', 'REFUNDED', 'NOT_PAID')),
    items_paise         BIGINT      NOT NULL,              -- selling price of all items
    mrp_paise           BIGINT      NOT NULL,
    delivery_paise      BIGINT      NOT NULL,
    total_paise         BIGINT      NOT NULL,
    ship_name           TEXT        NOT NULL,
    ship_mobile         TEXT        NOT NULL,
    ship_line1          TEXT        NOT NULL,
    ship_city           TEXT        NOT NULL,
    ship_state          TEXT        NOT NULL,
    ship_pincode        TEXT        NOT NULL,
    razorpay_order_id   TEXT UNIQUE,
    razorpay_payment_id TEXT,
    stock_released      BOOLEAN     NOT NULL DEFAULT FALSE,   -- set once catalog-service gave the stock back
    idempotency_key     TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)                         -- a double click never makes two orders
);

CREATE INDEX orders_user_idx ON orders (user_id, created_at DESC);
CREATE INDEX orders_status_idx ON orders (status, created_at DESC);

CREATE TABLE order_items (
    order_id    TEXT   NOT NULL REFERENCES orders (id),
    book_id     BIGINT NOT NULL,
    title       TEXT   NOT NULL,
    qty         INT    NOT NULL CHECK (qty > 0),
    price_paise BIGINT NOT NULL,
    mrp_paise   BIGINT NOT NULL,
    PRIMARY KEY (order_id, book_id)
);

-- Tracking timeline: one row per status change
CREATE TABLE order_events (
    id         BIGSERIAL PRIMARY KEY,
    order_id   TEXT        NOT NULL REFERENCES orders (id),
    status     TEXT        NOT NULL,
    note       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX order_events_order_idx ON order_events (order_id, id);

-- The bell in the store header
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    order_id   TEXT REFERENCES orders (id),
    title      TEXT        NOT NULL,
    body       TEXT        NOT NULL,
    is_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX notifications_user_idx ON notifications (user_id, id DESC);
