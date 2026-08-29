CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    full_name     TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('ADMIN','INVENTORY_MANAGER','FINANCE_MANAGER')),
    salt          TEXT    NOT NULL,
    password_hash TEXT    NOT NULL,
    active        INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sku           TEXT    NOT NULL UNIQUE,
    name          TEXT    NOT NULL,
    category_id   INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    cost_price    REAL    NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    unit_price    REAL    NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    quantity      INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 0,
    created_at    TEXT    NOT NULL
);

