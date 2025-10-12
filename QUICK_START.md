# Quick Start Guide - Kafka + Database Integration

## ⚡ Start Here (3 Simple Steps)

### Step 1: Start Docker Services (if not already running)

```bash
docker-compose up -d
```

Wait 30 seconds for services to start.

### Step 2: Run the Application

**Option A - Auto Demo (Recommended for first time):**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration" -Dexec.args="--demo"
```

**Option B - Interactive Mode:**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration"
```

### Step 3: View Results

**In pgAdmin (Database UI):**
- Open: http://localhost:5050
- Login: `admin@admin.com` / `admin123`
- Connect to server (if not connected):
  - Right-click "Servers" → "Register" → "Server"
  - Name: `Local PostgreSQL`
  - Connection tab:
    - Host: `postgres`
    - Port: `5432`
    - Database: `mydb`
    - Username: `admin`
    - Password: `admin123`
- Navigate to: Servers → Local PostgreSQL → Databases → mydb → Schemas → public → Tables → messages
- Right-click on `messages` → View/Edit Data → All Rows

**OR via Command Line:**
```bash
docker exec postgres-db psql -U admin -d mydb -c "SELECT * FROM messages;"
```

---

## 🎯 What Just Happened?

1. ✅ Application sent messages to **Kafka**
2. ✅ **Kafka** stored messages in topic "demo-topic"
3. ✅ Consumer read messages from **Kafka**
4. ✅ Consumer saved messages to **PostgreSQL database**
5. ✅ You can now see all messages in the database!

---

## 🔍 How It Works

```
Your Java App → Kafka Topic → Consumer → PostgreSQL Database
```

**Producer** sends message:
```java
Message msg = new Message();
msg.setContent("Hello World!");
producer.sendMessage(msg);  // → Goes to Kafka
```

**Consumer** receives and saves:
```java
consumer.start((message, record) -> {
    // Message arrives from Kafka
    repository.insertMessage(message);  // → Saved to database
});
```

---

## 📊 Monitor Everything

| Service | URL | What You See |
|---------|-----|--------------|
| **Kafka UI** | http://localhost:8080 | Messages in Kafka topics |
| **pgAdmin** | http://localhost:5050 | Database tables and data |
| **Logs** | Your terminal | Real-time application logs |

---

## 🧪 Try These Commands

```bash
# See all messages in database
docker exec postgres-db psql -U admin -d mydb -c "SELECT message_id, content FROM messages;"

# Count messages
docker exec postgres-db psql -U admin -d mydb -c "SELECT COUNT(*) FROM messages;"

# View last 5 messages
docker exec postgres-db psql -U admin -d mydb -c "SELECT * FROM messages ORDER BY created_at DESC LIMIT 5;"

# Check Docker services
docker ps

# View Kafka logs
docker logs kafka-broker --tail 50

# View database logs
docker logs postgres-db --tail 50
```

---

## 🚀 What to Do Next

1. **Read the full guide**: Open `KAFKA_DATABASE_INTEGRATION_GUIDE.md`
2. **Explore the code**:
   - Start with `KafkaDatabaseIntegration.java`
   - Then look at `MessageRepository.java`
3. **Modify and experiment**:
   - Add more fields to `Message` class
   - Create custom message handlers
   - Try querying with different SQL commands

---

## 🐛 Quick Troubleshooting

**Services not starting?**
```bash
docker-compose down
docker-compose up -d
# Wait 30 seconds
```

**Application can't connect?**
```bash
# Check services are running
docker ps

# Should see: kafka-broker, postgres-db, kafka-zookeeper, kafka-ui, pgadmin
```

**Database is empty?**
```bash
# Run the application first!
mvn exec:java -Dexec.mainClass="com.example.kafka.KafkaDatabaseIntegration" -Dexec.args="--demo"
```

---

## 📚 Learn More

- Full Integration Guide: `KAFKA_DATABASE_INTEGRATION_GUIDE.md`
- SQL Reference: `SQL_COMMANDS_REFERENCE.md`
- Database Details: `DB deatils.txt`

---

**That's it! You now have a complete event-driven application with Kafka and PostgreSQL!** 🎉
