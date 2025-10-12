# Kafka Producer-Consumer Demo

A comprehensive Java Kafka application demonstrating producer-consumer patterns with proper error handling, logging, and production-ready practices.

## Features

- **Complete Kafka Implementation**: Full-featured producer and consumer classes
- **Configuration Management**: Externalized configuration with properties files
- **Error Handling**: Robust error handling and retry mechanisms
- **Logging**: Comprehensive logging with SLF4J and Logback
- **Docker Support**: Docker Compose setup for local Kafka development
- **Multiple Run Modes**: Interactive, demo, producer-only, and consumer-only modes
- **Production Ready**: Best practices for production deployment

## Project Structure

```
kafka-producer-consumer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/kafka/
│   │   │       ├── config/
│   │   │       │   └── KafkaConfig.java          # Configuration management
│   │   │       ├── model/
│   │   │       │   └── Message.java              # Message model
│   │   │       ├── producer/
│   │   │       │   └── KafkaProducer.java        # Producer implementation
│   │   │       ├── consumer/
│   │   │       │   └── KafkaConsumer.java        # Consumer implementation
│   │   │       └── KafkaApplication.java         # Main application
│   │   └── resources/
│   │       ├── kafka.properties                  # Kafka configuration
│   │       ├── application.properties           # Application configuration
│   │       └── logback.xml                      # Logging configuration
│   └── test/
│       └── java/
├── docker-compose.yml                           # Kafka Docker setup
├── pom.xml                                      # Maven dependencies
├── .gitignore                                   # Git ignore rules
└── README.md                                    # This file
```

## Prerequisites

- **Java 11** or higher
- **Maven 3.6** or higher
- **Docker & Docker Compose** (for local Kafka setup)

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd kafka-producer-consumer
mvn clean compile
```

### 2. Start Kafka with Docker

```bash
# Start Kafka, Zookeeper, and Kafka UI
docker-compose up -d

# Wait for services to be ready (about 30-60 seconds)
docker-compose logs -f kafka-init

# Verify services are running
docker-compose ps
```

### 3. Run the Application

#### Interactive Mode (Default)
```bash
mvn exec:java
```

#### Demo Mode (Automated)
```bash
mvn exec:java -Dexec.args="demo"
```

#### Producer Only
```bash
mvn exec:java -Dexec.args="producer"
```

#### Consumer Only
```bash
mvn exec:java -Dexec.args="consumer"
```

## Configuration

### Kafka Configuration (`src/main/resources/kafka.properties`)

Key configuration parameters:

```properties
# Broker
bootstrap.servers=localhost:9092

# Producer
producer.acks=all                    # Wait for all replicas
producer.retries=3                   # Retry failed sends
producer.batch.size=16384            # Batch size for efficiency

# Consumer
consumer.group.id=kafka-demo-group   # Consumer group
consumer.auto.offset.reset=earliest  # Read from beginning
consumer.enable.auto.commit=true     # Auto-commit offsets

# Topic
topic.name=demo-topic
topic.partitions=3
topic.replication.factor=1
```

### Application Configuration (`src/main/resources/application.properties`)

```properties
app.name=Kafka Producer Consumer Demo
kafka.config.file=kafka.properties
logging.level.com.example.kafka=DEBUG
```

## Usage Examples

### Demo Mode
Runs automated producer and consumer for demonstration:

```bash
mvn exec:java -Dexec.args="demo"
```

This will:
1. Start a consumer in the background
2. Send 10 demo messages
3. Display received messages
4. Automatically shutdown after 15 seconds

### Interactive Producer
Send custom messages:

```bash
mvn exec:java -Dexec.args="producer"
```

Type messages and press Enter. Type `quit` to exit.

### Interactive Consumer
Listen for messages:

```bash
mvn exec:java -Dexec.args="consumer"
```

Press `Ctrl+C` to stop the consumer.

## Docker Services

The Docker Compose setup includes:

### Kafka Broker
- **Port**: 9092 (external), 29092 (internal)
- **Image**: confluentinc/cp-kafka:7.5.0

### Zookeeper
- **Port**: 2181
- **Image**: confluentinc/cp-zookeeper:7.5.0

### Kafka UI
- **Port**: 8080
- **URL**: http://localhost:8080
- **Features**: Topic management, message browsing, consumer group monitoring

### Topic Initialization
Automatically creates the `demo-topic` with 3 partitions.

## Monitoring

### Kafka UI Dashboard
Access the web-based Kafka UI at: http://localhost:8080

Features:
- View topics and partitions
- Browse messages
- Monitor consumer groups
- View broker information

### Application Logs
Logs are written to:
- **Console**: Formatted output with timestamps
- **File**: `logs/kafka-app.log` (rotating, 10MB max, 30 days retention)

### Log Levels
- `com.example.kafka`: DEBUG
- `org.apache.kafka`: WARN
- Root level: INFO

## Production Considerations

### Configuration
- Adjust `acks`, `retries`, and `batch.size` for your throughput/durability needs
- Set appropriate `replication.factor` for production topics
- Configure `security.protocol` for secured clusters

### Error Handling
- Producer: Implements retry logic and callback error handling
- Consumer: Graceful error handling with logging and recovery

### Monitoring
- Enable JMX metrics for production monitoring
- Use Kafka's built-in monitoring tools
- Implement custom health checks

### Security
- Configure SASL/SSL for secure communication
- Implement proper authentication and authorization
- Use secrets management for credentials

## Development

### Building
```bash
mvn clean compile
```

### Testing
```bash
mvn test
```

### Packaging
```bash
mvn clean package
```

### Running Tests with Embedded Kafka
```bash
mvn test -Dtest=KafkaIntegrationTest
```

## Troubleshooting

### Common Issues

#### Connection Refused
```
Error: Connection to node -1 could not be established
```
**Solution**: Ensure Kafka is running: `docker-compose ps`

#### Topic Not Found
```
Error: Topic 'demo-topic' not found
```
**Solution**: Check topic creation: `docker-compose logs kafka-init`

#### Port Already in Use
```
Error: Port 9092 is already in use
```
**Solution**: Stop conflicting services or change ports in docker-compose.yml

### Docker Commands

```bash
# View all services
docker-compose ps

# View logs
docker-compose logs kafka
docker-compose logs zookeeper

# Restart services
docker-compose restart

# Stop and remove all services
docker-compose down

# Remove volumes (clean slate)
docker-compose down -v
```

### Kafka CLI Tools (via Docker)

```bash
# List topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic demo-topic

# Console producer
docker-compose exec kafka kafka-console-producer --bootstrap-server localhost:9092 --topic demo-topic

# Console consumer
docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic demo-topic --from-beginning
```

## License

This project is provided as-is for educational and demonstration purposes.

## Contributing

Feel free to submit issues and enhancement requests!

## Learning Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Kafka Tutorials](https://kafka-tutorials.confluent.io/)
- [Spring Kafka Reference](https://spring.io/projects/spring-kafka)