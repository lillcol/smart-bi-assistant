-- =========================================================
-- Smart BI Assistant - Semantic Metadata Tables (MySQL)
-- Purpose:
-- 1) metric_definition: business metric semantic definitions
-- 2) table_schema: physical table/column metadata dictionary
-- =========================================================

-- Create and switch to project database.
-- Keep database name aligned with application-local.yml datasource url.
CREATE DATABASE IF NOT EXISTS smart_bi
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE smart_bi;

CREATE TABLE IF NOT EXISTS metric_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
  metric_code VARCHAR(50) NOT NULL COMMENT 'Unique metric code, e.g. gmv, uv',
  metric_name VARCHAR(100) NOT NULL COMMENT 'Metric display name',
  metric_sql TEXT COMMENT 'Optional metric SQL expression/template',
  description TEXT COMMENT 'Business definition and usage notes',
  UNIQUE KEY uk_metric_code (metric_code)
) COMMENT='Metric semantic definition table';

CREATE TABLE IF NOT EXISTS table_schema (
  table_name VARCHAR(100) NOT NULL COMMENT 'Physical table name',
  column_name VARCHAR(100) NOT NULL COMMENT 'Physical column name',
  column_desc VARCHAR(255) COMMENT 'Column business description',
  PRIMARY KEY (table_name, column_name)
) COMMENT='Table and column metadata dictionary';

-- =========================================================
-- Demo business table for quick local verification
-- =========================================================
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Order primary key',
  user_id BIGINT NOT NULL COMMENT 'User ID',
  order_no VARCHAR(64) NOT NULL COMMENT 'Order number',
  pay_amount DECIMAL(16,2) NOT NULL DEFAULT 0 COMMENT 'Paid amount',
  status VARCHAR(32) NOT NULL DEFAULT 'PAID' COMMENT 'Order status',
  pay_time DATETIME NOT NULL COMMENT 'Payment time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_pay_time (pay_time)
) COMMENT='Demo order fact table';

-- =========================================================
-- Seed data: semantic metadata (idempotent inserts)
-- =========================================================
INSERT INTO metric_definition (metric_code, metric_name, metric_sql, description)
VALUES
  ('gmv', 'Gross Merchandise Volume', 'SUM(pay_amount)', 'Total paid amount in selected period'),
  ('order_count', 'Order Count', 'COUNT(*)', 'Total order count in selected period')
ON DUPLICATE KEY UPDATE
  metric_name = VALUES(metric_name),
  metric_sql = VALUES(metric_sql),
  description = VALUES(description);

INSERT INTO table_schema (table_name, column_name, column_desc)
VALUES
  ('orders', 'id', 'Order primary key'),
  ('orders', 'user_id', 'User ID'),
  ('orders', 'order_no', 'Order number'),
  ('orders', 'pay_amount', 'Paid amount'),
  ('orders', 'status', 'Order status'),
  ('orders', 'pay_time', 'Payment time'),
  ('orders', 'created_at', 'Creation time')
ON DUPLICATE KEY UPDATE
  column_desc = VALUES(column_desc);

-- =========================================================
-- Seed data: demo order records (safe repeat execution)
-- =========================================================
INSERT INTO orders (user_id, order_no, pay_amount, status, pay_time)
VALUES
  (1001, 'ORD-20260410-0001', 199.00, 'PAID', NOW() - INTERVAL 6 DAY),
  (1002, 'ORD-20260411-0002', 89.90,  'PAID', NOW() - INTERVAL 5 DAY),
  (1003, 'ORD-20260412-0003', 319.00, 'PAID', NOW() - INTERVAL 4 DAY),
  (1001, 'ORD-20260413-0004', 49.90,  'PAID', NOW() - INTERVAL 3 DAY),
  (1004, 'ORD-20260414-0005', 520.00, 'PAID', NOW() - INTERVAL 2 DAY),
  (1005, 'ORD-20260415-0006', 129.00, 'PAID', NOW() - INTERVAL 1 DAY),
  (1006, 'ORD-20260416-0007', 259.00, 'PAID', NOW())
ON DUPLICATE KEY UPDATE
  pay_amount = VALUES(pay_amount),
  status = VALUES(status),
  pay_time = VALUES(pay_time);
