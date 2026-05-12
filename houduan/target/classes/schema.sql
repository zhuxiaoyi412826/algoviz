-- 反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    username TEXT,
    email TEXT,
    type TEXT,
    title TEXT,
    content TEXT,
    status TEXT DEFAULT 'PENDING',
    reply TEXT,
    reply_time TEXT,
    create_time TEXT,
    update_time TEXT
);

-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    content TEXT,
    status TEXT DEFAULT 'DRAFT',
    sort_order INTEGER DEFAULT 0,
    publish_time TEXT,
    create_time TEXT,
    update_time TEXT
);

-- 提交记录表
CREATE TABLE IF NOT EXISTS submission (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    problem_id INTEGER,
    code TEXT,
    language TEXT,
    result TEXT,
    runtime INTEGER,
    memory INTEGER,
    error_message TEXT,
    create_time TEXT
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT UNIQUE,
    user_id INTEGER,
    product_id INTEGER,
    product_name TEXT,
    price REAL,
    quantity INTEGER DEFAULT 1,
    status TEXT DEFAULT 'PENDING',
    refund_status TEXT DEFAULT 'NONE',
    refund_reason TEXT,
    transaction_id TEXT,
    create_time TEXT,
    update_time TEXT
);