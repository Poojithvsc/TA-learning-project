-- Database initialization script for PostgreSQL
-- This script creates the messages table and inserts sample data

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO messages (message_id, content, timestamp)
VALUES
    ('msg-001', 'First sample message from SQL', 1728750000000),
    ('msg-002', 'Second sample message from SQL', 1728750060000),
    ('msg-003', 'Third sample message from SQL', 1728750120000),
    ('msg-004', 'Fourth sample message from SQL', 1728750180000),
    ('msg-005', 'Fifth sample message from SQL', 1728750240000)
ON CONFLICT (message_id) DO NOTHING;

-- Display the inserted data
SELECT * FROM messages ORDER BY created_at DESC;

-- Show table information
SELECT
    COUNT(*) as total_messages,
    MIN(created_at) as earliest_message,
    MAX(created_at) as latest_message
FROM messages;
