# PostgreSQL Database - SQL Commands Reference

This guide shows you how to interact with your PostgreSQL database using SQL commands.

## Quick Access Methods

### Method 1: Using Docker Exec (Command Line)
```bash
# Connect to PostgreSQL container
docker exec -it postgres-db psql -U admin -d mydb
```

### Method 2: Using pgAdmin (Web UI)
- Open http://localhost:5050 in your browser
- Login: admin@admin.com / admin123
- Connect to server: postgres (host), port 5432, database mydb, user admin

### Method 3: From Your Java Application
The database connection code is in `src/main/java/com/example/kafka/db/`

---

## Basic SQL Commands

### 1. View All Tables
```sql
\dt
```
or
```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public';
```

### 2. View Table Structure
```sql
\d messages
```
or
```sql
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'messages';
```

---

## CRUD Operations

### CREATE - Insert New Messages

```sql
-- Insert a single message
INSERT INTO messages (message_id, content, timestamp)
VALUES ('msg-006', 'My new message', 1728750300000);

-- Insert multiple messages
INSERT INTO messages (message_id, content, timestamp)
VALUES
    ('msg-007', 'Batch message 1', 1728750360000),
    ('msg-008', 'Batch message 2', 1728750420000),
    ('msg-009', 'Batch message 3', 1728750480000);
```

### READ - Query Messages

```sql
-- Get all messages
SELECT * FROM messages;

-- Get messages ordered by date (newest first)
SELECT * FROM messages ORDER BY created_at DESC;

-- Get a specific message by ID
SELECT * FROM messages WHERE message_id = 'msg-001';

-- Search messages by content
SELECT * FROM messages WHERE content LIKE '%sample%';

-- Get only first 10 messages
SELECT * FROM messages LIMIT 10;

-- Get messages with pagination (skip 5, take 10)
SELECT * FROM messages OFFSET 5 LIMIT 10;

-- Count total messages
SELECT COUNT(*) as total FROM messages;

-- Get messages from last 24 hours
SELECT * FROM messages
WHERE created_at >= NOW() - INTERVAL '24 hours';
```

### UPDATE - Modify Existing Messages

```sql
-- Update a single message
UPDATE messages
SET content = 'Updated message content'
WHERE message_id = 'msg-001';

-- Update multiple fields
UPDATE messages
SET content = 'Modified content',
    timestamp = 1728750540000
WHERE message_id = 'msg-002';

-- Update all messages (be careful!)
UPDATE messages
SET timestamp = EXTRACT(EPOCH FROM NOW()) * 1000;
```

### DELETE - Remove Messages

```sql
-- Delete a specific message
DELETE FROM messages WHERE message_id = 'msg-001';

-- Delete messages older than a certain date
DELETE FROM messages
WHERE created_at < NOW() - INTERVAL '30 days';

-- Delete all messages (use with caution!)
DELETE FROM messages;

-- Delete all messages and reset the auto-increment counter
TRUNCATE TABLE messages RESTART IDENTITY;
```

---

## Advanced Queries

### Aggregations

```sql
-- Count messages
SELECT COUNT(*) as total_messages FROM messages;

-- Get earliest and latest messages
SELECT
    MIN(created_at) as first_message,
    MAX(created_at) as last_message
FROM messages;

-- Count messages per day
SELECT
    DATE(created_at) as message_date,
    COUNT(*) as count
FROM messages
GROUP BY DATE(created_at)
ORDER BY message_date DESC;
```

### Filtering & Searching

```sql
-- Case-insensitive search
SELECT * FROM messages
WHERE LOWER(content) LIKE LOWER('%sample%');

-- Messages containing specific words
SELECT * FROM messages
WHERE content ~* '(sample|test|demo)';

-- Messages NOT containing certain words
SELECT * FROM messages
WHERE content NOT LIKE '%spam%';
```

### Sorting

```sql
-- Sort by creation date (newest first)
SELECT * FROM messages ORDER BY created_at DESC;

-- Sort by content alphabetically
SELECT * FROM messages ORDER BY content ASC;

-- Sort by multiple columns
SELECT * FROM messages
ORDER BY created_at DESC, message_id ASC;
```

---

## Useful Database Operations

### Backup & Export

```bash
# Export database to SQL file
docker exec postgres-db pg_dump -U admin mydb > backup.sql

# Export specific table
docker exec postgres-db pg_dump -U admin -t messages mydb > messages_backup.sql

# Export data as CSV
docker exec postgres-db psql -U admin -d mydb -c "COPY messages TO STDOUT WITH CSV HEADER" > messages.csv
```

### Restore & Import

```bash
# Restore from SQL file
docker exec -i postgres-db psql -U admin -d mydb < backup.sql

# Import CSV (first create table structure)
docker exec -i postgres-db psql -U admin -d mydb -c "COPY messages FROM STDIN WITH CSV HEADER" < messages.csv
```

### Database Management

```sql
-- View database size
SELECT pg_size_pretty(pg_database_size('mydb'));

-- View table size
SELECT pg_size_pretty(pg_total_relation_size('messages'));

-- View all indexes
SELECT * FROM pg_indexes WHERE tablename = 'messages';

-- Create an index for faster searches
CREATE INDEX idx_messages_content ON messages USING gin(to_tsvector('english', content));

-- Drop an index
DROP INDEX idx_messages_content;
```

---

## Performance Tips

### 1. Use EXPLAIN to analyze queries
```sql
EXPLAIN ANALYZE SELECT * FROM messages WHERE content LIKE '%sample%';
```

### 2. Create indexes for frequently queried columns
```sql
CREATE INDEX idx_message_id ON messages(message_id);
CREATE INDEX idx_created_at ON messages(created_at);
```

### 3. Use prepared statements in your Java code
Already implemented in `MessageRepository.java`

---

## Connection Information

- **Host**: localhost
- **Port**: 5432
- **Database**: mydb
- **Username**: admin
- **Password**: admin123
- **JDBC URL**: `jdbc:postgresql://localhost:5432/mydb`

---

## Quick Reference Commands

### Enter PostgreSQL CLI
```bash
docker exec -it postgres-db psql -U admin -d mydb
```

### Common psql Commands
```
\l          - List all databases
\dt         - List all tables
\d messages - Describe messages table
\q          - Quit psql
\h          - Help with SQL commands
\?          - Help with psql commands
```

### Execute SQL file
```bash
docker exec -i postgres-db psql -U admin -d mydb < your-script.sql
```

### Run single SQL command
```bash
docker exec postgres-db psql -U admin -d mydb -c "SELECT COUNT(*) FROM messages;"
```

---

## Troubleshooting

### Check if database is running
```bash
docker ps | grep postgres
```

### View database logs
```bash
docker logs postgres-db
```

### Restart database
```bash
docker restart postgres-db
```

### Connect and verify
```bash
docker exec -it postgres-db psql -U admin -d mydb -c "SELECT version();"
```

---

## Next Steps

1. **Practice with pgAdmin**: Open http://localhost:5050 and try running queries in the web interface
2. **Explore the Java code**: Check `src/main/java/com/example/kafka/db/MessageRepository.java` to see how to interact with the database programmatically
3. **Learn SQL**: Visit [PostgreSQL Tutorial](https://www.postgresqltutorial.com/) for more advanced topics
4. **Integrate with Kafka**: Modify the Kafka consumer to save messages to the database

---

Happy querying! 🚀
