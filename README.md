# Payment Gateway V2

A comprehensive, production-ready payment processing system with support for multiple payment methods, refund management, and webhook integration.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Latest-cyan?style=flat-square&logo=docker)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![Node.js](https://img.shields.io/badge/Node.js-18-green?style=flat-square&logo=node.js)

## Project Overview

Payment Gateway V2 is a full-stack payment processing platform designed to handle complex payment workflows with enterprise-grade reliability. The system provides secure payment processing, real-time status tracking, refund management, and webhook delivery for merchant integrations.

## Key Features

- Multi-payment method support (UPI, Card)
- Real-time payment status tracking
- Full and partial refund processing
- Idempotency key support for request deduplication
- Webhook event delivery system
- Redis-based caching and queuing
- Async job processing
- Health monitoring and status checks
- RESTful API with merchant authentication
- Merchant dashboard
- Payment checkout interface

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          FRONTEND LAYER                         │
├──────────────────────┬──────────────────────┬──────────────────┤
│   Dashboard UI       │   Checkout Interface │   Merchant Panel │
│   (React - 3000)     │   (React - 3001)     │                  │
└──────────────┬───────┴──────────────┬───────┴──────────────────┘
               │                      │
               └──────────┬───────────┘
                         │
        ┌────────────────▼────────────────┐
        │    API GATEWAY (Port 8000)      │
        │  Spring Boot 3.2.1 Application  │
        └────────────────┬────────────────┘
                         │
        ┌────────────────┴────────────────┐
        │                                 │
   ┌────▼─────┐                    ┌────▼─────┐
   │ JPA/ORM  │                    │   Redis  │
   │ Layer    │                    │  Cache   │
   └────┬─────┘                    │  & Queue │
        │                          └──────────┘
        │
   ┌────▼──────────────────────┐
   │   PostgreSQL Database      │
   │   - Merchants              │
   │   - Orders                 │
   │   - Payments               │
   │   - Refunds                │
   │   - Webhooks               │
   │   - Idempotency Keys       │
   └────────────────────────────┘

   ┌──────────────────────────────────────┐
   │    Async Job Processing (Workers)    │
   │  - Payment Processing                │
   │  - Refund Processing                 │
   │  - Webhook Delivery                  │
   │  - Job Status Tracking               │
   └──────────────────────────────────────┘
```

## Payment Flow Diagram

```
MERCHANT CLIENT
     │
     │ 1. Create Order
     ▼
┌─────────────────┐
│ Order Service   │  Validates credentials
├─────────────────┤  Creates order record
│ Order Created   │
└────────┬────────┘
         │
         │ 2. Create Payment
         ▼
    ┌─────────────────────┐
    │ Payment Service     │  Validates payment method
    ├─────────────────────┤  Validates amount & currency
    │ Payment Processing  │  Creates payment record
    └────────┬────────────┘
             │
             │ 3. Async Job Enqueue
             ▼
        ┌──────────────────┐
        │ Job Service      │
        ├──────────────────┤
        │ Queue Job in     │  Stores in Redis queue
        │ Redis            │
        └────────┬─────────┘
                 │
                 │ 4. Background Processing
                 ▼
        ┌──────────────────┐
        │ Payment Worker   │  Simulates payment
        ├──────────────────┤  Updates status
        │ Processes Job    │  Enqueues webhook
        └────────┬─────────┘
                 │
         ┌───────┴───────┐
         │               │
         │ 5. Webhook    │ 6. Update Status
         ▼               ▼
    ┌─────────┐    ┌──────────────┐
    │ Webhook │    │ Payment      │
    │ Delivery│    │ Status:      │
    │ Worker  │    │ - Success    │
    │         │    │ - Failed     │
    └─────────┘    └──────────────┘
```

## Refund Processing Flow

```
REFUND REQUEST
     │
     │ Validate merchant credentials
     │ Validate payment exists & successful
     │ Validate refund amount
     ▼
┌─────────────────────┐
│ Refund Service      │  Creates refund record
├─────────────────────┤  Status: "pending"
│ Refund Created      │
└────────┬────────────┘
         │
         │ Enqueue ProcessRefundJob
         ▼
    ┌──────────────────┐
    │ Redis Job Queue  │
    │ process_refund   │
    └────────┬─────────┘
             │
             │ Background Processing
             ▼
    ┌──────────────────────┐
    │ Refund Worker        │  Simulates refund
    ├──────────────────────┤  Updates status
    │ Updates Status       │  Enqueues webhook
    │ - completed/failed   │  Updates payment
    └────────┬─────────────┘
             │
         ┌───┴───┐
         │       │
         ▼       ▼
    ┌────────┐  ┌─────────────┐
    │Webhook │  │Payment      │
    │Delivery│  │Refunded Amt │
    │        │  │Updated      │
    └────────┘  └─────────────┘
```

## API Endpoints

### Order Management
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders/{order_id}` - Get order details
- `GET /api/v1/orders/{order_id}/public` - Public order retrieval

### Payment Processing
- `POST /api/v1/payments` - Create and process payment
- `GET /api/v1/payments/{payment_id}` - Get payment details
- `GET /api/v1/payments/{payment_id}/public` - Public payment status

### Refund Management
- `POST /api/v1/refunds` - Create refund
- `GET /api/v1/refunds/{refund_id}` - Get refund details
- `POST /api/v1/refunds/{refund_id}/retry` - Retry failed refund

### Health & Status
- `GET /health` - System health check

## Project Structure

```
payment-gateway-v2/
├── backend/                          # Java Spring Boot Backend
│   ├── src/main/java/com/example/gateway/
│   │   ├── PaymentGatewayApplication.java
│   │   ├── config/                   # Spring configurations
│   │   ├── controllers/              # REST controllers
│   │   ├── services/                 # Business logic
│   │   │   ├── OrderService.java
│   │   │   ├── PaymentService.java
│   │   │   ├── RefundService.java
│   │   │   ├── ValidationService.java
│   │   │   └── WebhookServiceImpl.java
│   │   ├── models/                   # JPA entities
│   │   ├── repositories/             # Data access layer
│   │   ├── jobs/                     # Job definitions
│   │   ├── workers/                  # Background job workers
│   │   └── exceptions/               # Exception handling
│   ├── src/main/resources/
│   │   └── application.properties    # Configuration
│   ├── pom.xml                       # Maven dependencies
│   ├── Dockerfile                    # Container image
│   └── mvnw                          # Maven wrapper
│
├── dashboard/                         # Admin Dashboard (React)
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── checkout/                          # Payment Checkout (React)
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── docker-compose.yml                 # Container orchestration
└── README.md                          # This file
```

## Technology Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.1
- **ORM**: Hibernate/JPA
- **Database**: PostgreSQL 15
- **Caching**: Redis 7
- **Build Tool**: Maven
- **Web Server**: Apache Tomcat (embedded)

### Frontend
- **Language**: JavaScript/TypeScript
- **Framework**: React 18
- **Build Tool**: npm/yarn
- **Web Server**: Node.js

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Database**: PostgreSQL 15-alpine
- **Cache**: Redis 7-alpine

## Setup & Installation

### Prerequisites
- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 18+ (for frontend development)
- Maven 3.9+ (for backend builds)

### Quick Start with Docker

```bash
# Clone the repository
git clone <repository-url>
cd payment-gateway-v2

# Start all services
docker-compose up -d

# Verify services are running
docker ps

# Check health
curl http://localhost:8000/health
```

### Access Points
- API: http://localhost:8000
- Dashboard: http://localhost:3000
- Checkout: http://localhost:3001
- Database: localhost:5432
- Redis: localhost:6379

### Local Development

**Backend**:
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

**Frontend**:
```bash
cd dashboard    # or checkout
npm install
npm start
```

## Configuration

### Environment Variables

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payment_gateway
SPRING_DATASOURCE_USERNAME=gateway_user
SPRING_DATASOURCE_PASSWORD=gateway_password

# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Payment Processing
TEST_MODE=true
TEST_PAYMENT_SUCCESS=true
UPI_SUCCESS_RATE=0.90
CARD_SUCCESS_RATE=0.95
```

## Key Services

### OrderService
- Order creation with validation
- Merchant authentication
- Amount and currency validation
- Order status tracking

### PaymentService
- Multi-method payment processing (UPI, Card)
- Idempotency key management
- Real-time status updates
- Async job enqueueing

### RefundService
- Full and partial refund processing
- Refund amount validation
- Available amount calculation
- Refund status tracking

### ValidationService
- VPA format validation
- Card number validation (Luhn algorithm)
- Card expiry validation
- Email and URL validation
- API key validation

### WebhookService
- Event-based webhook delivery
- Retry mechanism
- Webhook log tracking
- Payload building and signing

## Database Schema

### Core Entities
- **Merchants**: Merchant accounts with API credentials
- **Orders**: Customer orders with amount and status
- **Payments**: Payment records with method and status
- **Refunds**: Refund records with amount and status
- **WebhookLogs**: Webhook delivery tracking
- **IdempotencyKeys**: Request deduplication

## Building

### Maven Build

```bash
cd backend

# Development build
./mvnw clean install

# Production build
./mvnw clean package -DskipTests

# Run tests
./mvnw test
```

### Docker Build

```bash
# Build all services
docker-compose build

# Rebuild specific service
docker-compose build backend
```

## Monitoring & Logging

### Health Check
```bash
curl http://localhost:8000/health
```

Response includes:
- API status
- Database connection status
- Redis connection status
- Worker service status

### Docker Logs
```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f postgres
docker-compose logs -f redis
```

## Deployment

### Production Considerations
- Use environment-specific configurations
- Enable SSL/TLS for all endpoints
- Configure proper authentication
- Set up monitoring and alerting
- Enable database backups
- Configure Redis persistence
- Set up log aggregation

## Performance Metrics

- Order creation: < 100ms
- Payment processing: < 5s (async)
- Refund processing: < 5s (async)
- Webhook delivery: Configurable retry mechanism
- Database queries optimized with indexes
- Redis caching for frequently accessed data

## Security

- API key and secret-based authentication
- Input validation on all endpoints
- SQL injection prevention via parameterized queries
- CORS configuration for frontend
- Rate limiting on webhook delivery
- Secure password hashing for credentials

## Testing

### Test Coverage
- Unit tests for services
- Integration tests for workflows
- API endpoint testing
- Validation logic testing

Run tests:
```bash
./mvnw test
```

## Contributing

1. Create feature branch: `git checkout -b feature/feature-name`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/feature-name`
4. Submit pull request

## Git Commit History

Major commits in this project:
- `9070864` - Fix RefundService implementation
- `7463af9` - Add RefundService and update WebhookService tests
- `601e9f1` - Add comprehensive Java-Spring backend test suite
- `9de2643` - Add comprehensive HTTP testing
- Previous commits with core implementation

## Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# View database logs
docker-compose logs postgres
```

### Redis Connection Issues
```bash
# Check Redis is running
docker ps | grep redis

# Connect to Redis
redis-cli -h localhost
```

### API Not Responding
```bash
# Check API health
curl http://localhost:8000/health

# View API logs
docker-compose logs -f backend
```

## Project Status

- [x] Core API implementation
- [x] Order management
- [x] Payment processing
- [x] Refund system
- [x] Webhook integration
- [x] Job processing (async)
- [x] Authentication
- [x] Frontend dashboard
- [x] Checkout interface
- [x] Docker deployment
- [x] Health monitoring
