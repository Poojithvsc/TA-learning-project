# Interactive File Manager - Complete User Guide

## 🎯 What This System Does

Upload and download YOUR OWN files using a **production-grade cloud architecture**:
- **S3 (MinIO)**: Stores your actual files (like Dropbox, Google Drive)
- **PostgreSQL**: Stores file information (metadata)
- **Kafka**: Sends real-time events when files are uploaded/downloaded/deleted

---

## 🚀 Quick Start (3 Steps)

### Step 1: Start Docker Services

Open a terminal and run:

```bash
cd "D:\Tinku anna project"
docker-compose up -d
```

Wait 30-60 seconds for all services to start.

### Step 2: Verify Services Are Running

```bash
docker-compose ps
```

You should see all services as "running" and "healthy":
- ✅ kafka-broker
- ✅ kafka-zookeeper
- ✅ postgres-db
- ✅ minio-s3
- ✅ kafka-ui
- ✅ pgadmin

### Step 3: Run the Interactive Application

```bash
mvn exec:java -Dexec.mainClass="com.example.kafka.InteractiveFileManager"
```

---

## 📋 Menu Options Explained

### 1. 📤 Upload a File

**What it does:** Uploads any file from your computer to the cloud

**Example:**
```
Your choice: 1

Enter file path: C:\Users\YourName\Documents\report.pdf
Enter your username: john
Enter description: Monthly sales report
```

**What happens behind the scenes:**
1. File uploaded to S3 (MinIO) - `uploads/john/report.pdf`
2. Metadata saved to PostgreSQL database
3. Event sent to Kafka topic `file-events`
4. Kafka consumer receives the event in real-time!

**File Types Supported:**
- Documents: PDF, Word, Excel, PowerPoint
- Images: JPG, PNG, GIF
- Videos: MP4, AVI, MOV
- Audio: MP3, WAV
- Archives: ZIP, RAR
- Any other file type!

---

### 2. 📋 List All Files

**What it does:** Shows all files in the system

**Output example:**
```
Total files: 3

─────────────────────────────────────────────
1. report.pdf
   ID: 550e8400-e29b-41d4-a716-446655440000
   Size: 2.5 MB
   Uploaded by: john
   Date: Sat Oct 18 14:30:00 2025
   Description: Monthly sales report
─────────────────────────────────────────────
```

---

### 3. 📥 Download a File

**What it does:** Downloads a file from S3 to your computer

**Example:**
```
Your choice: 3

Enter filename to download: report.pdf

📄 File found:
  • Filename: report.pdf
  • Size: 2.5 MB
  • Uploaded by: john
  • S3 Key: uploads/john/report.pdf

Enter download location: C:\Users\YourName\Downloads

✅ SUCCESS! File downloaded!
  📁 Location: C:\Users\YourName\Downloads\report.pdf
```

**What happens:**
1. File retrieved from S3 storage
2. Saved to your specified location
3. Download event sent to Kafka

---

### 4. 🔍 Search Files

**What it does:** Searches for files by name

**Example:**
```
Your choice: 4

Enter search term: report

🔍 Found 2 file(s):

─────────────────────────────────────────────
1. monthly_report.pdf
   Size: 2.5 MB
   Uploaded by: john
   Date: Sat Oct 18 14:30:00 2025
─────────────────────────────────────────────
2. sales_report.xlsx
   Size: 856 KB
   Uploaded by: sarah
   Date: Sat Oct 18 15:45:00 2025
─────────────────────────────────────────────
```

---

### 5. 👤 View My Files

**What it does:** Shows all files uploaded by a specific user

**Example:**
```
Your choice: 5

Enter username: john

👤 Files for john (5 total):

─────────────────────────────────────────────
1. report.pdf
   Size: 2.5 MB
   Date: Sat Oct 18 14:30:00 2025
─────────────────────────────────────────────
2. presentation.pptx
   Size: 15.3 MB
   Date: Sat Oct 18 16:20:00 2025
─────────────────────────────────────────────
```

---

### 6. 🗑️ Delete a File

**What it does:** Permanently deletes a file from S3 and database

**Example:**
```
Your choice: 6

Enter filename to delete: old_report.pdf

⚠️  Are you sure you want to delete:
  • old_report.pdf
  • Size: 1.2 MB

Type 'yes' to confirm: yes

⏳ Deleting...
  [1/3] Deleting from S3...
        ✓ Deleted from S3
  [2/3] Deleting from database...
        ✓ Deleted from database
  [3/3] Sending event to Kafka...
        ✓ Event sent

✅ File deleted successfully!
```

---

### 7. 📊 View Statistics

**What it does:** Shows system-wide statistics

**Example output:**
```
📊 System Statistics:
  • Total files in database: 15
  • Total files in S3: 15
  • Total storage used: 127.5 MB

🗂️  Files in S3:
  - uploads/john/report.pdf
  - uploads/sarah/presentation.pptx
  - uploads/mike/budget.xlsx
  ...
```

---

### 8. 🌐 Open Web Consoles

**What it does:** Shows URLs to access web-based management tools

**Web Consoles Available:**

#### MinIO Console (View Your Files in Browser)
- **URL:** http://localhost:9001
- **Username:** minioadmin
- **Password:** minioadmin123
- **Features:**
  - Browse all uploaded files
  - Download files
  - View file details
  - Create/delete buckets

#### pgAdmin (View Database)
- **URL:** http://localhost:5050
- **Username:** admin@admin.com
- **Password:** admin123
- **Features:**
  - View files table
  - Run SQL queries
  - See all metadata

**First time setup for pgAdmin:**
1. Go to http://localhost:5050
2. Login with credentials above
3. Right-click "Servers" → Register → Server
4. General tab: Name = "My PostgreSQL"
5. Connection tab:
   - Host: postgres
   - Port: 5432
   - Username: admin
   - Password: admin123
6. Click Save
7. Navigate to: Servers → My PostgreSQL → Databases → mydb → Schemas → public → Tables → files
8. Right-click files → View/Edit Data → All Rows

#### Kafka UI (View Events)
- **URL:** http://localhost:8080
- **No login required**
- **Features:**
  - View topics
  - See messages in real-time
  - Monitor consumer groups

---

## 🎬 Real-Time Kafka Events

When you upload, download, or delete a file, you'll see **real-time events** appear in the console:

```
╔════════════════════════════════════════════════════════╗
║  📨 KAFKA EVENT RECEIVED                              ║
╚════════════════════════════════════════════════════════╝
  Event: FILE_UPLOADED
  File: report.pdf
  Size: 2.5 MB
  User: john
  Time: Sat Oct 18 14:30:00 2025
════════════════════════════════════════════════════════
```

This happens in the background while you use the application!

---

## 🏗️ How the Architecture Works

### Upload Flow (Step-by-Step)

```
YOU
 │
 ├─► [1] Select file from computer
 │
 ├─► [2] Upload to S3 (MinIO)
 │      └─► File stored in: my-files/uploads/username/filename.pdf
 │
 ├─► [3] Save metadata to PostgreSQL
 │      └─► Database stores: ID, filename, size, user, date, S3 location
 │
 ├─► [4] Send event to Kafka
 │      └─► Message: "FILE_UPLOADED: report.pdf by john"
 │
 └─► [5] Kafka Consumer receives event
        └─► Shows real-time notification in console
```

### Download Flow

```
YOU
 │
 ├─► [1] Search for file by name
 │
 ├─► [2] Database returns file metadata
 │      └─► Gets S3 location from database
 │
 ├─► [3] Download from S3
 │      └─► Retrieves actual file from S3 storage
 │
 ├─► [4] Save to your computer
 │      └─► File saved to Downloads folder
 │
 └─► [5] Send event to Kafka
        └─► Message: "FILE_DOWNLOADED: report.pdf by john"
```

---

## 💡 Common Use Cases

### Use Case 1: Personal File Backup

Upload important documents to cloud storage:

```bash
# Upload your files
1. Upload a File
   - Select: C:\Important\Passport.pdf
   - Username: myself
   - Description: Personal documents backup

# Later, retrieve them
3. Download a File
   - Filename: Passport.pdf
   - Location: C:\Backup
```

### Use Case 2: Team File Sharing

Multiple users can upload and share files:

```bash
# John uploads project files
Username: john
Files: project_plan.pdf, budget.xlsx

# Sarah can later search and download
Username: sarah
Search: project
Download: project_plan.pdf
```

### Use Case 3: Document Management System

Organize files with descriptions and search:

```bash
# Upload with descriptions
Filename: contract_2024_Q4.pdf
Description: Annual contract - Client ABC

# Search later
Search term: contract
Search term: 2024
Search term: Client ABC
```

---

## 🔧 Troubleshooting

### Problem: "Cannot connect to database"

**Solution:**
```bash
# Check if Docker services are running
docker-compose ps

# Restart services if needed
docker-compose restart

# Check PostgreSQL logs
docker-compose logs postgres
```

### Problem: "S3 upload failed"

**Solution:**
```bash
# Check MinIO status
docker-compose ps minio

# View MinIO logs
docker-compose logs minio

# Restart MinIO
docker-compose restart minio
```

### Problem: "Kafka events not appearing"

**Solution:**
```bash
# Check Kafka status
docker-compose ps kafka

# View Kafka logs
docker-compose logs kafka

# Check Kafka UI
# Open: http://localhost:8080
# Look for topic: file-events
```

### Problem: Port already in use

**Solution:**
```bash
# Stop all containers
docker-compose down

# Check what's using the port
netstat -ano | findstr :9092  # Kafka
netstat -ano | findstr :5432  # PostgreSQL
netstat -ano | findstr :9000  # MinIO

# Kill the process or change port in docker-compose.yml
```

---

## 📊 Understanding the Data Flow

### Where is everything stored?

| Component | What's Stored | Location |
|-----------|---------------|----------|
| **S3 (MinIO)** | Actual files (PDF, images, videos) | `my-files` bucket |
| **PostgreSQL** | File metadata (name, size, user, date) | `files` table |
| **Kafka** | Events (upload/download/delete) | `file-events` topic |

### Example: Uploading "vacation.jpg"

**S3 Storage:**
```
Bucket: my-files
Path: uploads/john/vacation.jpg
Size: 3.5 MB (actual image file)
```

**PostgreSQL Database:**
```sql
INSERT INTO files (
  id = '550e8400-e29b-41d4-a716-446655440000',
  filename = 'vacation.jpg',
  s3_key = 'uploads/john/vacation.jpg',
  s3_url = 'http://localhost:9000/my-files/uploads/john/vacation.jpg',
  file_size = 3670016,
  content_type = 'image/jpeg',
  uploaded_by = 'john',
  uploaded_at = 1729260000000,
  description = 'Summer vacation photos'
);
```

**Kafka Event:**
```json
{
  "eventType": "FILE_UPLOADED",
  "timestamp": 1729260000000,
  "fileMetadata": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "vacation.jpg",
    "fileSize": 3670016,
    "uploadedBy": "john",
    "s3Url": "http://localhost:9000/my-files/uploads/john/vacation.jpg"
  }
}
```

---

## 🎓 Learning the Architecture

### Why S3 for files?

**Traditional Approach (BAD):**
- Store files directly in database
- Database becomes huge and slow
- Expensive to scale

**Modern Approach (GOOD):**
- Store files in S3 (blob storage)
- Unlimited capacity
- Fast and cheap
- Database only stores metadata

### Why Kafka for events?

**Without Kafka:**
- No real-time notifications
- Can't track what happens
- No audit trail

**With Kafka:**
- Real-time event streaming
- Multiple systems can react to events
- Complete audit trail of all actions
- Scalable to millions of events

### Why PostgreSQL for metadata?

- Fast searching by filename, user, date
- Structured data with relationships
- ACID transactions (data integrity)
- SQL queries for complex searches

---

## 🚀 Advanced Usage

### Running Automated Demo

See the system in action automatically:

```bash
mvn exec:java -Dexec.mainClass="com.example.kafka.CompleteIntegrationDemo"
```

This will:
1. Upload 3 sample files
2. Save to S3 and database
3. Send Kafka events
4. Show consumer receiving events
5. Display statistics

### Viewing Kafka Messages Directly

```bash
# List all topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Read messages from file-events topic
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic file-events \
  --from-beginning
```

### Querying Database Directly

Connect to PostgreSQL:
```bash
docker-compose exec postgres psql -U admin -d mydb
```

Useful queries:
```sql
-- Count total files
SELECT COUNT(*) FROM files;

-- Files by user
SELECT filename, file_size, uploaded_at
FROM files
WHERE uploaded_by = 'john';

-- Total storage per user
SELECT uploaded_by, SUM(file_size) as total_bytes
FROM files
GROUP BY uploaded_by;

-- Recent files (last 10)
SELECT filename, uploaded_by, uploaded_at
FROM files
ORDER BY uploaded_at DESC
LIMIT 10;

-- Search files
SELECT filename, uploaded_by
FROM files
WHERE filename LIKE '%report%';
```

---

## 📖 Event Types

The system sends 3 types of events to Kafka:

### 1. FILE_UPLOADED
Sent when a file is uploaded

```json
{
  "eventType": "FILE_UPLOADED",
  "timestamp": 1729260000000,
  "fileMetadata": { ... }
}
```

### 2. FILE_DOWNLOADED
Sent when a file is downloaded

```json
{
  "eventType": "FILE_DOWNLOADED",
  "timestamp": 1729260100000,
  "fileMetadata": { ... }
}
```

### 3. FILE_DELETED
Sent when a file is deleted

```json
{
  "eventType": "FILE_DELETED",
  "timestamp": 1729260200000,
  "fileMetadata": { ... }
}
```

---

## ✅ Complete Example Walkthrough

Let's upload and download a real file:

### Step 1: Start Services
```bash
cd "D:\Tinku anna project"
docker-compose up -d
# Wait 60 seconds
```

### Step 2: Run Application
```bash
mvn exec:java -Dexec.mainClass="com.example.kafka.InteractiveFileManager"
```

### Step 3: Upload a File
```
Your choice: 1
Enter file path: C:\Users\YourName\Documents\resume.pdf
Enter your username: alice
Enter description: My resume for job applications
```

Watch the console - you'll see:
1. Upload progress (4 steps)
2. Success message
3. **Kafka event appears in real-time!**

### Step 4: View Files
```
Your choice: 2
```

You'll see your resume.pdf listed!

### Step 5: Download It
```
Your choice: 3
Enter filename to download: resume.pdf
Enter download location: C:\Users\YourName\Desktop
```

The file is now on your Desktop!

### Step 6: Check Web Consoles

**MinIO Console** (http://localhost:9001):
- Login: minioadmin / minioadmin123
- Navigate to `my-files` bucket
- See `uploads/alice/resume.pdf`
- Can download it from web too!

**pgAdmin** (http://localhost:5050):
- Login: admin@admin.com / admin123
- View the files table
- See metadata for resume.pdf

**Kafka UI** (http://localhost:8080):
- Click on `file-events` topic
- See all upload/download events

---

## 🎯 Next Steps

1. **Try uploading different file types**
   - Images, videos, documents, archives

2. **Simulate multiple users**
   - Use different usernames
   - View files by user

3. **Explore the web consoles**
   - See your files in MinIO
   - Query the database in pgAdmin
   - Monitor events in Kafka UI

4. **Read the code**
   - `InteractiveFileManager.java` - Main application
   - `S3Manager.java` - S3 operations
   - `FileRepository.java` - Database operations
   - `FileEventProducer.java` - Kafka producer
   - `FileEventConsumer.java` - Kafka consumer

---

## 🏆 You Now Have

A **production-grade cloud file storage system** with:

✅ **Object Storage (S3)** - Like AWS S3, Google Cloud Storage
✅ **Relational Database (PostgreSQL)** - Like AWS RDS
✅ **Event Streaming (Kafka)** - Like AWS Kinesis, Google Pub/Sub
✅ **Real-time Processing** - Event-driven architecture
✅ **Web Management Consoles** - UI for all services

This is the **EXACT architecture** used by companies like:
- Dropbox, Google Drive (file storage)
- Netflix, YouTube (video platforms)
- Spotify (music streaming)
- Instagram, Facebook (photo sharing)

---

## 📞 Need Help?

Common commands:

```bash
# View running services
docker-compose ps

# View logs for specific service
docker-compose logs postgres
docker-compose logs kafka
docker-compose logs minio

# Restart everything
docker-compose restart

# Stop everything
docker-compose down

# Start fresh (removes all data)
docker-compose down -v
docker-compose up -d
```

Enjoy your cloud file storage system! 🚀
