CREATE TABLE topics (
    key         TEXT PRIMARY KEY,           -- e.g. CI/CD, Containers
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    sort_order  INT  NOT NULL
);

-- Covers are drawn by the frontend from logo_key + colours, so there are no image files to store
CREATE TABLE books (
    id          BIGSERIAL PRIMARY KEY,
    title       TEXT        NOT NULL,
    subtitle    TEXT        NOT NULL,
    topic_key   TEXT        NOT NULL REFERENCES topics (key),
    logo_key    TEXT        NOT NULL,
    color1      TEXT        NOT NULL,
    color2      TEXT        NOT NULL,
    mrp_paise   BIGINT      NOT NULL CHECK (mrp_paise > 0),
    price_paise BIGINT      NOT NULL CHECK (price_paise > 0),
    stock       INT         NOT NULL CHECK (stock >= 0),   -- never below zero, even with two orders at once
    pages       INT         NOT NULL DEFAULT 0,
    highlights  TEXT        NOT NULL DEFAULT '',          -- one per line
    image_url   TEXT,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,        -- hidden books stay for old orders
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (price_paise <= mrp_paise)
);

CREATE INDEX books_topic_idx ON books (topic_key) WHERE active;

CREATE TABLE wishlist (
    user_id    BIGINT      NOT NULL,
    book_id    BIGINT      NOT NULL REFERENCES books (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, book_id)
);

-- Stock taken for an order. order-service calls reserve and release with the same order id,
-- so a retry never takes stock twice.
CREATE TABLE stock_reservations (
    order_id   TEXT PRIMARY KEY,
    status     TEXT        NOT NULL CHECK (status IN ('RESERVED', 'RELEASED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_reservation_items (
    order_id    TEXT   NOT NULL REFERENCES stock_reservations (order_id),
    book_id     BIGINT NOT NULL REFERENCES books (id),
    qty         INT    NOT NULL CHECK (qty > 0),
    title       TEXT   NOT NULL,     -- price and title at the time of the order
    price_paise BIGINT NOT NULL,
    mrp_paise   BIGINT NOT NULL,
    PRIMARY KEY (order_id, book_id)
);
