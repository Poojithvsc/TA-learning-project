# Kafka + Database Integration Guide

This guide explains how your Kafka application works with PostgreSQL database and how to use it.

---

## 🎯 What Does This Application Do?

Your application creates a **complete data pipeline**:

```
Producer → Kafka → Consumer → PostgreSQL Database
```

### Step-by-Step Flow:

1. **Producer** creates a message (e.g., "Hello World")
2. **Kafka** receives and stores the message temporarily in a topic
3. **Consumer** reads the message from Kafka
4. **Consumer** saves the message to PostgreSQL database
5. **You** can query the database to see all saved messages

---

## 🏗️ Architecture Overview

```
┌─────────────┐         ┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Your      │         │             │         │              │         │  PostgreSQL  │
│   Java      │ ------> │    Kafka    │ ------> │   Consumer   │ ------> │   Database   │
│  Producer   │         │   (Topic)   │         │              │         │    (mydb)    │
└─────────────┘         └─────────────┘         └──────────────┘         └──────────────┘
     │                                                                            │
     │                                                                            │
     └──────────── You can query saved messages anytime ───────────────────────┘
```

### Components:

| Component | Purpose | Location |
|-----------|---------|----------|
| **Kafka** | Message broker | Docker container (port 9092) |
| **PostgreSQL** | Database | Docker container (port 5432) |
| **Producer** | Sends messages | `KafkaProducer.java` |
| **Consumer** | Receives & saves messages | `KafkaConsumer.java` |
| **Repository** | Database operations | `MessageRepository.java` |
| **Integration** | Connects everything | `KafkaDatabaseIntegration.java` |

---

## 🚀 How to Run the Application

### Prerequisites - Start Docker Services

```bash
# Start Kafka and database
docker-compose up -d

# Verify they're running
docker ps
```

You should see:
- kafka-broker
- postgres-db
- kafka-ui
- pgadmin

### Method 1: Auto Demo Mode

This runs a pre-configured demo that sends 5 messages:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration" -Dexec.args="--demo"
```

**What happens:**
1. Connects to Kafka and database
2. Sends 5 sample messages to Kafka
3. Consumer automatically receives and saves them to database
4. Shows you all saved messages
5. Shuts down

### Method 2: Interactive Mode

This lets you send your own messages:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration"
```

**Menu options:**
1. **Send a message** - Type your own message to send
2. **View all messages** - See all messages in database
3. **View message count** - Check how many messages are stored
4. **Exit** - Close the application

---

## 📊 How to Monitor Everything

### 1. View Kafka Messages (Web UI)

Open: http://localhost:8080

- See all topics
- View messages in real-time
- Monitor consumer groups

### 2. View Database (pgAdmin)

Open: http://localhost:5050

Login:
- Email: `admin@admin.com`
- Password: `admin123`

Then connect to PostgreSQL:
- Host: `postgres`
- Port: `5432`
- Database: `mydb`
- Username: `admin`
- Password: `admin123`

### 3. Query Database Directly

```bash
# Connect to database
docker exec -it postgres-db psql -U admin -d mydb

# View all messages
SELECT * FROM messages ORDER BY created_at DESC;

# Count messages
SELECT COUNT(*) FROM messages;

# Exit
\q
```

---

## 🔍 Understanding the Code

### 1. Producer (Sending Messages)

**File:** `src/main/java/com/example/kafka/producer/KafkaProducer.java`

```java
// Create a message
Message message = new Message();
message.setId(UUID.randomUUID().toString());
message.setContent("Hello from Kafka!");
message.setTimestamp(System.currentTimeMillis());

// Send to Kafka
producer.sendMessage(message);
```

**What happens:**
- Message is serialized to JSON
- Sent to Kafka topic "demo-topic"
- Kafka confirms receipt

### 2. Consumer (Receiving Messages)

**File:** `src/main/java/com/example/kafka/consumer/KafkaConsumer.java`

```java
// Consumer reads messages and processes them
consumer.start((message, record) -> {
    // This handler is called for each message
    System.out.println("Received: " + message.getContent());

    // You can do anything here - like save to database!
});
```

**What happens:**
- Consumer polls Kafka for new messages
- When message arrives, handler is called
- Message is deserialized from JSON to Java object

### 3. Database Saving

**File:** `src/main/java/com/example/kafka/db/MessageRepository.java`

```java
// Save message to database
repository.insertMessage(message);

// Retrieve messages
List<Message> messages = repository.getAllMessages();

// Count messages
int count = repository.getMessageCount();
```

**What happens:**
- Uses JDBC to connect to PostgreSQL
- HikariCP manages connection pool for performance
- SQL queries store and retrieve messages

### 4. Integration (Putting It All Together)

**File:** `src/main/java/com/example/kafka/KafkaDatabaseIntegration.java`

This is the **main application** that connects everything:

```java
// When message arrives from Kafka...
consumer.start((message, record) -> {
    logger.info("Received: " + message.getContent());

    // Save it to database
    repository.insertMessage(message);
    logger.info("Saved to database!");
});
```

---

## 💡 Real-World Use Cases

### 1. Event Logging System
- **Producer**: Your application sends log events
- **Kafka**: Stores events temporarily
- **Consumer**: Saves events to database for analytics
- **Benefit**: Never lose logs, can replay events

### 2. Order Processing System
- **Producer**: Customer places order
- **Kafka**: Queues order for processing
- **Consumer**: Validates and stores order in database
- **Benefit**: Handles high traffic, orders never lost

### 3. Real-time Analytics
- **Producer**: User actions on website
- **Kafka**: Streams user events
- **Consumer**: Aggregates data and stores in database
- **Benefit**: Real-time insights, historical data

### 4. Microservices Communication
- **Service A**: Sends event to Kafka
- **Kafka**: Broadcasts to multiple services
- **Service B**: Processes and stores in database
- **Benefit**: Decoupled services, scalable

---

## 🧪 Testing the Integration

### Test 1: Send and Verify

```bash
# Terminal 1: Run the application
mvn exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration"

# Choose option 1, send a message: "Test message 1"

# Choose option 2, view all messages
# You should see "Test message 1" in the list
```

### Test 2: Database Persistence

```bash
# Send some messages using the application

# Stop the application (option 4)

# Query database directly
docker exec postgres-db psql -U admin -d mydb -c "SELECT content FROM messages;"

# You should see all your messages are still there!
```

### Test 3: Kafka Replay

```bash
# Stop your application

# Send messages to Kafka directly
mvn exec:java -Dexec.mainClass="com.example.kafka.producer.KafkaProducer"

# Start consumer
# It will process ALL messages (even old ones)
```

---

## 🐛 Troubleshooting

### Problem: "Cannot connect to database"

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# If not running, start it
docker-compose up -d postgres

# Test connection
docker exec postgres-db psql -U admin -d mydb -c "SELECT 1;"
```

### Problem: "Cannot connect to Kafka"

**Solution:**
```bash
# Check if Kafka is running
docker ps | grep kafka

# If not running, start everything
docker-compose up -d

# Wait 30 seconds for Kafka to start
```

### Problem: "Messages not appearing in database"

**Check these:**

1. **Consumer is running?**
   - Look for log: "Consumer started and listening..."

2. **Database table exists?**
   ```bash
   docker exec postgres-db psql -U admin -d mydb -c "\dt"
   ```

3. **Check for errors in logs**
   - Look for red ERROR messages in console

### Problem: "Duplicate messages in database"

**This is normal!** Kafka can deliver messages more than once. To handle this:

```sql
-- Our table has UNIQUE constraint on message_id
-- Duplicates are automatically handled with ON CONFLICT
SELECT COUNT(*) FROM messages;  -- Shows unique messages only
```

---

## 📚 Learn More

### Kafka Concepts

1. **Topic**: Like a folder that holds messages
2. **Producer**: Sends messages to topic
3. **Consumer**: Reads messages from topic
4. **Partition**: Topics are split for scalability
5. **Offset**: Position of message in partition

### Database Concepts

1. **Connection Pool**: Reuses database connections for performance
2. **Transaction**: Group of operations that succeed or fail together
3. **Index**: Speeds up queries on specific columns
4. **Primary Key**: Unique identifier for each row

### Integration Patterns

1. **Event Sourcing**: Store all changes as events
2. **CQRS**: Separate read and write operations
3. **Change Data Capture**: Capture database changes to Kafka
4. **Saga Pattern**: Manage distributed transactions

---

## 🎓 Next Steps

### Level 1: Beginner
- ✅ Run the demo mode
- ✅ Send custom messages in interactive mode
- ✅ View messages in pgAdmin
- ✅ Query database with SQL

### Level 2: Intermediate
- Modify `Message` class to add more fields
- Create a new consumer that filters messages
- Add database indexes for better performance
- Create a REST API to send messages

### Level 3: Advanced
- Implement error handling and retry logic
- Add message transformation before saving
- Create multiple consumers for parallel processing
- Implement idempotent processing (prevent duplicates)
- Add monitoring with Prometheus/Grafana

---

## 🔗 Quick Reference Links

| Service | URL | Credentials |
|---------|-----|-------------|
| Kafka UI | http://localhost:8080 | None |
| pgAdmin | http://localhost:5050 | admin@admin.com / admin123 |
| PostgreSQL | localhost:5432 | admin / admin123 / mydb |
| Kafka Broker | localhost:9092 | None |

---

## 📞 Key Files to Explore

```
project/
├── src/main/java/com/example/kafka/
│   ├── KafkaDatabaseIntegration.java    ⭐ Main application (START HERE)
│   ├── producer/KafkaProducer.java      📤 Sends messages
│   ├── consumer/KafkaConsumer.java      📥 Receives messages
│   ├── db/
│   │   ├── DatabaseManager.java         🔌 DB connection
│   │   └── MessageRepository.java       💾 Save/retrieve messages
│   └── model/Message.java               📝 Message structure
├── src/main/resources/
│   ├── application.properties           ⚙️  Configuration
│   └── kafka.properties                 ⚙️  Kafka settings
├── docker-compose.yml                   🐳 Docker services
└── init-db.sql                          🗄️  Database setup
```

---

Happy coding! 🚀

If you have questions, check the logs, use the monitoring tools, or query the database directly!
