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
    units_per_box INTEGER NOT NULL DEFAULT 1 CHECK (units_per_box >= 1),
    reorder_level INTEGER NOT NULL DEFAULT 0,
    created_at    TEXT    NOT NULL
);

-- one row per rack cell (1..54), filled at the bottom
CREATE TABLE IF NOT EXISTS rack_cells (
    id         INTEGER PRIMARY KEY CHECK (id BETWEEN 1 AND 54),
    status     TEXT    NOT NULL DEFAULT 'EMPTY' CHECK (status IN ('EMPTY','INBOUND','OCCUPIED','OUTBOUND')),
    product_id INTEGER REFERENCES products(id) ON DELETE RESTRICT,
    quantity   INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS sales_orders (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    customer      TEXT    NOT NULL,
    product_id    INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    boxes         INTEGER NOT NULL CHECK (boxes > 0),
    boxes_shipped INTEGER NOT NULL DEFAULT 0,
    status        TEXT    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','DONE','CANCELLED')),
    username      TEXT,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    cell_id    INTEGER NOT NULL REFERENCES rack_cells(id),
    order_id   INTEGER REFERENCES sales_orders(id) ON DELETE SET NULL,
    type       TEXT    NOT NULL CHECK (type IN ('IN','OUT')),
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    username   TEXT,
    created_at TEXT    NOT NULL
);

INSERT OR IGNORE INTO rack_cells (id, status, quantity)
    WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x + 1 FROM n WHERE x < 54)
    SELECT x, 'EMPTY', 0 FROM n;

CREATE INDEX IF NOT EXISTS idx_movements_product ON stock_movements(product_id);
