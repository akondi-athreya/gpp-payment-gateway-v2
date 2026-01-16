# Payment Gateway - Deployment Guide

Complete guide for deploying the Payment Gateway to production.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Building the Project](#building-the-project)
5. [Docker Deployment](#docker-deployment)
6. [Production Checklist](#production-checklist)
7. [Monitoring & Troubleshooting](#monitoring--troubleshooting)
8. [Backup & Recovery](#backup--recovery)

---

## Prerequisites

- Docker & Docker Compose (v20.10+)
- Java 21+ (for local development)
- Node.js 18+ (for frontend)
- Maven 3.8+ (for backend builds)
- Git

---

## Environment Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd payment-gateway-v2
```

### 2. Configure Environment Variables

Create `.env` file in root directory:

```env
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/payment_gateway
DATABASE_USERNAME=gateway_user
DATABASE_PASSWORD=your_secure_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# API Configuration
PORT=8000
SERVER_CONTEXT_PATH=/

# Test Mode (disable in production)
TEST_MODE=false
TEST_PAYMENT_SUCCESS=true
TEST_PROCESSING_DELAY=1000
WEBHOOK_RETRY_TEST_MODE=false

# Merchant Credentials (change in production)
MERCHANT_API_KEY=key_prod_your_key_here
MERCHANT_API_SECRET=secret_prod_your_secret_here
MERCHANT_WEBHOOK_SECRET=whsec_prod_your_secret_here

# SSL/TLS (for production)
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=/path/to/keystore.jks
SERVER_SSL_KEY_STORE_PASSWORD=your_keystore_password

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_EXAMPLE_GATEWAY=DEBUG
```

---

## Database Configuration

### 1. PostgreSQL Setup

#### Option A: Using Docker Compose
```bash
docker-compose up -d postgres
```

#### Option B: Existing PostgreSQL Server
```sql
-- Create database
CREATE DATABASE payment_gateway;

-- Create user
CREATE USER gateway_user WITH PASSWORD 'gateway_pass';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE payment_gateway TO gateway_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO gateway_user;
```

### 2. Database Initialization

The application auto-migrates on startup via Hibernate:
```properties
spring.jpa.hibernate.ddl-auto=update
```

To use SQL migrations instead:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

---

## Building the Project

### 1. Backend Build

```bash
cd backend

# Build with Maven
mvn clean package -DskipTests

# Or with test
mvn clean package

# Build Docker image
docker build -f DockerFile -t payment-gateway-api:latest .
docker build -f Dockerfile.worker -t payment-gateway-worker:latest .
```

### 2. Frontend Build

```bash
cd frontend

# Install dependencies
npm install

# Build production bundle
npm run build

# Create Docker image
docker build -f Dockerfile -t payment-gateway-dashboard:latest .
```

### 3. Checkout Widget Build

```bash
cd checkout-widget

# Install dependencies
npm install

# Build SDK and iframe bundles
npm run build

# Output: dist/checkout.js and dist/checkout-iframe.js
```

---

## Docker Deployment

### 1. Using Docker Compose (Recommended)

```bash
# Build all services
docker-compose build

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f api
docker-compose logs -f worker
docker-compose logs -f dashboard
```

### 2. Manual Docker Deployment

```bash
# Create network
docker network create payment-gateway-network

# Run PostgreSQL
docker run -d \
  --name payment-postgres \
  --network payment-gateway-network \
  -e POSTGRES_DB=payment_gateway \
  -e POSTGRES_USER=gateway_user \
  -e POSTGRES_PASSWORD=gateway_pass \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15-alpine

# Run Redis
docker run -d \
  --name payment-redis \
  --network payment-gateway-network \
  redis:7-alpine

# Run API
docker run -d \
  --name payment-api \
  --network payment-gateway-network \
  -p 8000:8000 \
  -e DATABASE_URL=jdbc:postgresql://payment-postgres:5432/payment_gateway \
  -e REDIS_HOST=payment-redis \
  payment-gateway-api:latest

# Run Worker
docker run -d \
  --name payment-worker \
  --network payment-gateway-network \
  -e DATABASE_URL=jdbc:postgresql://payment-postgres:5432/payment_gateway \
  -e REDIS_HOST=payment-redis \
  payment-gateway-worker:latest

# Run Dashboard
docker run -d \
  --name payment-dashboard \
  --network payment-gateway-network \
  -p 3000:80 \
  -e VITE_API_BASE_URL=http://localhost:8000 \
  payment-gateway-dashboard:latest

# Run Checkout
docker run -d \
  --name payment-checkout \
  --network payment-gateway-network \
  -p 3001:80 \
  payment-gateway-checkout:latest
```

### 3. Health Checks

```bash
# API Health
curl http://localhost:8000/health

# Dashboard Health
curl http://localhost:3000/health

# Job Status (no auth required)
curl http://localhost:8000/api/v1/test/jobs/status
```

---

## Production Checklist

### Security
- [ ] Change default merchant credentials
- [ ] Enable SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Set up API rate limiting
- [ ] Enable CORS restrictions
- [ ] Rotate webhook secrets regularly
- [ ] Use environment-specific credentials
- [ ] Enable database encryption

### Performance
- [ ] Enable connection pooling
- [ ] Configure Redis persistence
- [ ] Set up database backups
- [ ] Enable query caching
- [ ] Optimize logging levels (INFO instead of DEBUG)
- [ ] Configure health check intervals
- [ ] Set up CDN for static assets
- [ ] Enable gzip compression

### Monitoring
- [ ] Set up error tracking (e.g., Sentry)
- [ ] Configure log aggregation (e.g., ELK Stack)
- [ ] Set up APM monitoring (e.g., DataDog)
- [ ] Configure alerting
- [ ] Set up dashboards
- [ ] Monitor database performance
- [ ] Monitor job queue health
- [ ] Track webhook delivery rates

### Operations
- [ ] Document runbooks
- [ ] Set up backup automation
- [ ] Test disaster recovery
- [ ] Configure automated scaling
- [ ] Set up deployment pipelines
- [ ] Document rollback procedures
- [ ] Test failover mechanisms
- [ ] Set up database replication

---

## Monitoring & Troubleshooting

### Logs

```bash
# API Logs
docker logs payment-api

# Worker Logs
docker logs payment-worker

# Database Logs
docker logs payment-postgres

# Redis Logs
docker logs payment-redis
```

### Common Issues

#### 1. Database Connection Failed
```
Error: org.postgresql.util.PSQLException: Connection refused
```

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check database exists
docker exec payment-postgres psql -U gateway_user -d payment_gateway -c "SELECT 1"
```

#### 2. Redis Connection Failed
```
Error: WRONGTYPE Operation against a key holding the wrong kind of value
```

**Solution:**
```bash
# Clear Redis cache
docker exec payment-redis redis-cli FLUSHALL

# Restart Redis
docker restart payment-redis
```

#### 3. Worker Not Processing Jobs
```bash
# Check worker status
curl http://localhost:8000/api/v1/test/jobs/status

# Check worker logs
docker logs payment-worker

# Restart worker
docker restart payment-worker
```

#### 4. Webhook Delivery Failing
```bash
# Check webhook logs
curl -H "X-Api-Key: key_test_abc123" \
     -H "X-Api-Secret: secret_test_xyz789" \
     http://localhost:8000/api/v1/webhooks

# Retry webhook
curl -X POST http://localhost:8000/api/v1/webhooks/{webhook_id}/retry \
     -H "X-Api-Key: key_test_abc123" \
     -H "X-Api-Secret: secret_test_xyz789"
```

### Performance Troubleshooting

```bash
# Check database connections
docker exec payment-postgres psql -U gateway_user -d payment_gateway \
  -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname"

# Check Redis memory
docker exec payment-redis redis-cli INFO memory

# Monitor job queue
curl http://localhost:8000/api/v1/test/jobs/status
```

---

## Backup & Recovery

### Database Backup

```bash
# Full backup
docker exec payment-postgres pg_dump -U gateway_user payment_gateway > backup.sql

# Restore backup
docker exec -i payment-postgres psql -U gateway_user payment_gateway < backup.sql

# Automated daily backup (Linux cron)
0 2 * * * docker exec payment-postgres pg_dump -U gateway_user payment_gateway > /backups/gateway_$(date +\%Y\%m\%d).sql
```

### Volume Backup

```bash
# Backup PostgreSQL volume
docker run --rm -v postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_data.tar.gz -C /data .

# Restore PostgreSQL volume
docker run --rm -v postgres_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_data.tar.gz -C /data
```

### Redis Persistence

```bash
# Enable Redis persistence in docker-compose.yml
volumes:
  - redis_data:/data

# Manual snapshot
docker exec payment-redis redis-cli BGSAVE
```

---

## Scaling

### Horizontal Scaling (Multiple Workers)

```yaml
# docker-compose.yml
worker:
  build:
    context: ./backend
    dockerfile: Dockerfile.worker
  deploy:
    replicas: 3  # Run 3 worker instances
  # ... rest of configuration
```

### Load Balancing

```bash
# Using Nginx as reverse proxy
docker run -d \
  --name payment-lb \
  -p 80:80 \
  -v /path/to/nginx.conf:/etc/nginx/nginx.conf:ro \
  nginx:alpine
```

---

## SSL/TLS Configuration

### Self-Signed Certificate (Development Only)

```bash
# Generate certificate
keytool -genkey -alias payment-gateway \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 2048 \
  -keystore keystore.p12 \
  -validity 365 \
  -storepass password

# Configure in application.properties
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=payment-gateway
```

### Production Certificates

Use Let's Encrypt with Certbot:

```bash
# Install Certbot
apt-get install certbot python3-certbot-nginx

# Generate certificate
certbot certonly --standalone -d payment-gateway.com

# Configure in application.properties
server.ssl.key-store=/etc/letsencrypt/live/payment-gateway.com/keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
```

---

## Performance Tuning

### Database Connection Pool

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### JVM Memory

```bash
# docker-compose.yml
environment:
  JAVA_OPTS: "-Xms512m -Xmx2g -XX:+UseG1GC"
```

### Redis Configuration

```bash
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

---

## Rollback Procedure

### Docker Rollback

```bash
# Stop current version
docker-compose down

# Start previous version
docker-compose down
git checkout <previous-commit>
docker-compose up -d
```

### Database Rollback

```bash
# Restore from backup
docker exec -i payment-postgres psql -U gateway_user payment_gateway < backup.sql

# Verify data
curl http://localhost:8000/api/v1/payments
```

---

## Support & Documentation

- [API Reference](./API_REFERENCE.md)
- [Architecture Guide](./IMPLEMENTATION_COMPLETE.md)
- [Troubleshooting](./AUDIT_EXECUTIVE_SUMMARY.md)

---

*Last Updated: January 16, 2026*
*Version: 2.0*
