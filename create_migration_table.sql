CREATE TABLE IF NOT EXISTS migration (
  id        INTEGER      PRIMARY KEY,
  name      VARCHAR(120) UNIQUE NOT NULL,
  hash      VARCHAR(40)  NOT NULL,
  run_at    TIMESTAMP    DEFAULT current_timestamp
);

-- COMMENT ON migration.hash IS 'The SHA-1 hash of migration script contents.';
