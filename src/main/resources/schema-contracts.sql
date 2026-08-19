-- Contract table for batch persistence.
-- Created automatically on startup via spring.sql.init.
CREATE TABLE IF NOT EXISTS contract (
    contract_id   VARCHAR(50)  PRIMARY KEY,
    client_id     VARCHAR(50)  NOT NULL,
    start_date    VARCHAR(20),
    status        VARCHAR(20),
    line_count    INT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
