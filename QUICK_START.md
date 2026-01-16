# Payment Gateway - Quick Start & Testing Guide

## 🚀 Quick Start

### 1. Start All Services
```bash
cd payment-gateway-v2
docker-compose up -d
```

### 2. Wait for Healthy Status
```bash
docker-compose ps
# Wait until all services show "Up" status
# api and db should show "(healthy)"
```

### 3. Test Endpoints
```bash
# Health check
curl http://localhost:8000/health

# Create order
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -d '{"amount":10000,"currency":"INR","receipt":"test_001"}'
```

---

## 📋 Test Credentials

```
API Key:       key_test_abc123
API Secret:    secret_test_xyz789
Webhook Secret: whsec_test_abc123
```

---

## 🌐 Service Endpoints

| Service | URL | Purpose |
|---------|-----|---------|
| **API** | http://localhost:8000 | Payment processing |
| **Dashboard** | http://localhost:3000 | Admin dashboard |
| **Checkout** | http://localhost:3001 | Payment form |
| **Database** | localhost:5432 | PostgreSQL |
| **Redis** | localhost:6379 | Job queue |

---

## 🧪 Test Scripts

### Comprehensive Manual Test
```bash
bash run-tests.sh
```

### Automated Test Suite
```bash
bash test-api.sh
```

### Check Logs
```bash
# API logs
docker logs payment-gateway-api -f

# Worker logs
docker logs payment-gateway-worker -f

# Database logs
docker logs payment-gateway-db -f
```

---

## 📊 API Quick Reference

### Create Order
```bash
POST /api/v1/orders
{
  "amount": 10000,
  "currency": "INR",
  "receipt": "receipt_id"
}
```

### Create Payment
```bash
POST /api/v1/payments
{
  "order_id": "order_F93BGJLEh8UW5In3",
  "method": "upi",
  "vpa": "user@bank"
}
```

### Get Payment Status
```bash
GET /api/v1/payments/{payment_id}
```

### List Webhooks
```bash
GET /api/v1/webhooks
```

### Job Queue Status
```bash
GET /api/v1/test/jobs/status
```

---

## 🐛 Troubleshooting

### Services Not Starting
```bash
# Check logs
docker-compose logs

# Restart services
docker-compose restart

# Full reset
docker-compose down -v
docker-compose up -d
```

### API Returning 400
- Check field names use snake_case (order_id, not orderId)
- Verify authentication headers (X-Api-Key, X-Api-Secret)
- Check minimum amount is 100 (₹1)

### Database Connection Error
```bash
# Check PostgreSQL is healthy
docker-compose ps | grep postgres

# Check logs
docker logs payment-gateway-db
```

### Worker Not Processing
```bash
# Check Redis is healthy
docker-compose ps | grep redis

# Check worker logs
docker logs payment-gateway-worker
```

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Service configuration |
| `backend/src/main/java/com/example/gateway/` | API source code |
| `frontend/` | Dashboard frontend |
| `checkout-page/` | Checkout form |
| `run-tests.sh` | Manual testing |
| `API_REFERENCE.md` | Full API documentation |
| `DEPLOYMENT_GUIDE.md` | Production setup |
| `BUG_REPORT.md` | Test results |
| `FINAL_STATUS_REPORT.md` | Complete status |

---

## ✅ What's Working

- ✅ Order creation (POST /api/v1/orders)
- ✅ Payment creation (POST /api/v1/payments)
- ✅ Payment retrieval (GET /api/v1/payments/{id})
- ✅ Webhook listing (GET /api/v1/webhooks)
- ✅ Job queue status (GET /api/v1/test/jobs/status)
- ✅ Health check (GET /health)
- ✅ Database persistence
- ✅ Redis caching
- ✅ Docker containerization
- ✅ Health checks
- ✅ Service dependencies

---

## 🔧 Development Notes

### Build Backend JAR
```bash
cd backend
./mvnw clean package -DskipTests
```

### Rebuild Docker Images
```bash
docker-compose build api worker
docker-compose up -d
```

### Database Access
```bash
psql -U gateway_user -d payment_gateway -h localhost
# Password: gateway_pass
```

### Redis Access
```bash
redis-cli -h localhost
```

---

## 📞 Support

For detailed information, see:
- **API Docs**: [API_REFERENCE.md](API_REFERENCE.md)
- **Deployment**: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **Test Results**: [BUG_REPORT.md](BUG_REPORT.md)
- **Full Status**: [FINAL_STATUS_REPORT.md](FINAL_STATUS_REPORT.md)

---

**Status**: 🟢 **FULLY OPERATIONAL**

Last Updated: January 16, 2026
