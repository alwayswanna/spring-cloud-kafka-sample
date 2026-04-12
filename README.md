# spring-cloud-kafka-sample

A sample multi-module Spring Boot application demonstrating Kafka-based messaging with Spring Cloud Stream.
The producer exposes a REST API to publish messages; the consumer reads them, validates the payload,
and routes invalid messages to a Dead Letter Queue (DLQ).

![CI](https://github.com/alwayswanna/spring-cloud-kafka-sample/actions/workflows/ci.yml/badge.svg)

---

## Architecture

```
 Client
   │
   │  POST /api/v1/message/send
   ▼
┌─────────────────┐      user-message topic       ┌──────────────────┐
│  producer-app   │ ─────────────────────────────► │  consumer-app    │
│  (port 9000)    │   Kafka (3 brokers, RF=3)      │  (port 9002)     │
└─────────────────┘                                └──────────────────┘
                                                          │ validation fails
                                                          ▼
                                                   user-message.dlq topic
```

---

## Modules

| Module         | Description                                                                        |
|----------------|------------------------------------------------------------------------------------|
| `common`       | Shared model — `UserMessage` record with Jakarta Validation constraints            |
| `producer-app` | REST API → validates request → publishes to `user-message` with Kafka transactions |
| `consumer-app` | Consumes `user-message` → validates payload → logs → routes failures to DLQ        |


---

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- **Docker** and **Docker Compose** (for local Kafka infrastructure)

---

## Running Locally

### 1. Start Kafka infrastructure

```bash
docker compose up -d
```

This starts:
- Zookeeper on port `2181`
- 3 Kafka brokers on ports `9091`, `9092`, `9093`
- Kafka UI on `http://localhost:8080`
- Topic init container — creates `user-message` and `user-message.dlq` with 3 partitions and replication factor 3

Wait until all containers are healthy (usually ~20 seconds):

```bash
docker compose ps
```

### 2. Build the project

```bash
mvn install -DskipTests
```

### 3. Start the applications

Run each in a separate terminal:

```bash
# Terminal 1 — producer
java -jar producer-app/target/producer-app-1.0.0.jar

# Terminal 2 — consumer
java -jar consumer-app/target/consumer-app-1.0.0.jar
```

| App           | API port | Management port |
|---------------|----------|-----------------|
| producer-app  | 9000     | 9001            |
| consumer-app  | —        | 9003            |

---

## API

### Send a message

```
POST http://localhost:9000/api/v1/message/send
```

### Swagger UI

Interactive API docs are available at:

```
http://localhost:9000/swagger-ui.html
```

---

## Kafka Topics

| Topic              | Partitions | Replication | Purpose                             |
|--------------------|------------|-------------|-------------------------------------|
| `user-message`     | 3          | 3           | Main topic — producer → consumer    |
| `user-message.dlq` | 3          | 3           | Dead Letter Queue — failed messages |

Messages that fail payload validation (null `login`, `message`, or `dtCreated`) are automatically
routed to the DLQ by Spring Cloud Stream. The original `correlationId` header is preserved.