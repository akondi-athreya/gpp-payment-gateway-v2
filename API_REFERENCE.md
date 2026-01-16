# Payment Gateway API Reference

Complete API documentation for Payment Gateway endpoints.

## Table of Contents
1. [Authentication](#authentication)
2. [Orders API](#orders-api)
3. [Payments API](#payments-api)
4. [Refunds API](#refunds-api)
5. [Webhooks API](#webhooks-api)
6. [Testing & Status](#testing--status)
7. [Error Codes](#error-codes)
8. [Examples](#examples)

---

## Authentication

All API endpoints (except test/jobs/status) require authentication via headers:

```http
X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789
```

### Test Credentials
```
API Key: key_test_abc123
API Secret: secret_test_xyz789
Webhook Secret: whsec_test_abc123
```

---

## Orders API

### Create Order

**Endpoint:** `POST /api/v1/orders`

**Authentication:** Required

**Request Body:**
```json
{
  "amount": 50000,
  "currency": "INR",
  "receipt": "receipt_unique_123",
  "description": "Order for iPhone"
}
```

**Response (201 Created):**
```json
{
  "id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "receipt": "receipt_unique_123",
  "description": "Order for iPhone",
  "status": "created",
  "created_at": "2024-01-15T10:31:00Z"
}
```

### Get Order

**Endpoint:** `GET /api/v1/orders/{order_id}`

**Authentication:** Not required

**Response (200 OK):**
```json
{
  "id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "receipt": "receipt_unique_123",
  "description": "Order for iPhone",
  "status": "created",
  "created_at": "2024-01-15T10:31:00Z"
}
```

---

## Payments API

### Create Payment (Async)

**Endpoint:** `POST /api/v1/payments`

**Authentication:** Required

**Headers:**
```http
X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789
Idempotency-Key: unique_request_id_123 (optional, for duplicate prevention)
Content-Type: application/json
```

**Request Body:**
```json
{
  "order_id": "order_NXhj67fGH2jk9mPq",
  "method": "upi",
  "vpa": "user@paytm"
}
```

**Or for card:**
```json
{
  "order_id": "order_NXhj67fGH2jk9mPq",
  "method": "card",
  "card": {
    "number": "4111111111111111",
    "expiry_month": 12,
    "expiry_year": 2025,
    "cvv": "123",
    "holder_name": "John Doe"
  }
}
```

**Response (201 Created):**
```json
{
  "id": "pay_H8sK3jD9s2L1pQr",
  "order_id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "method": "upi",
  "vpa": "user@paytm",
  "status": "pending",
  "created_at": "2024-01-15T10:31:00Z"
}
```

**Note:** Status is "pending" during async processing. Poll the GET endpoint to get updated status.

### Get Payment

**Endpoint:** `GET /api/v1/payments/{payment_id}`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "id": "pay_H8sK3jD9s2L1pQr",
  "order_id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "method": "upi",
  "vpa": "user@paytm",
  "status": "success",
  "captured": false,
  "created_at": "2024-01-15T10:31:00Z",
  "updated_at": "2024-01-15T10:32:00Z"
}
```

### Get All Payments

**Endpoint:** `GET /api/v1/payments`

**Authentication:** Required

**Response (200 OK):**
```json
[
  {
    "id": "pay_H8sK3jD9s2L1pQr",
    "order_id": "order_NXhj67fGH2jk9mPq",
    "amount": 50000,
    "currency": "INR",
    "method": "upi",
    "status": "success",
    "created_at": "2024-01-15T10:31:00Z"
  }
]
```

### Capture Payment

**Endpoint:** `POST /api/v1/payments/{payment_id}/capture`

**Authentication:** Required

**Description:** Mark payment as captured for settlement. Only works on successful payments.

**Request Body:**
```json
{
  "amount": 50000
}
```

**Response (200 OK):**
```json
{
  "id": "pay_H8sK3jD9s2L1pQr",
  "order_id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "method": "upi",
  "status": "success",
  "captured": true,
  "created_at": "2024-01-15T10:31:00Z",
  "updated_at": "2024-01-15T10:32:00Z"
}
```

---

## Refunds API

### Create Refund

**Endpoint:** `POST /api/v1/payments/{payment_id}/refunds`

**Authentication:** Required

**Request Body:**
```json
{
  "amount": 25000,
  "reason": "Customer requested refund"
}
```

**Response (201 Created):**
```json
{
  "id": "rfnd_K9pL2mN4oQ5r",
  "payment_id": "pay_H8sK3jD9s2L1pQr",
  "amount": 25000,
  "reason": "Customer requested refund",
  "status": "pending",
  "created_at": "2024-01-15T10:33:00Z"
}
```

**Note:** Refund processing happens asynchronously. Status changes from "pending" to "processed" after 3-5 seconds.

### Get Refund

**Endpoint:** `GET /api/v1/refunds/{refund_id}`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "id": "rfnd_K9pL2mN4oQ5r",
  "payment_id": "pay_H8sK3jD9s2L1pQr",
  "amount": 25000,
  "reason": "Customer requested refund",
  "status": "processed",
  "created_at": "2024-01-15T10:33:00Z",
  "processed_at": "2024-01-15T10:33:05Z"
}
```

---

## Webhooks API

### List Webhooks

**Endpoint:** `GET /api/v1/webhooks`

**Authentication:** Required

**Query Parameters:**
- `limit`: Number of records (default: 10)
- `offset`: Number of records to skip (default: 0)

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "event": "payment.success",
      "status": "success",
      "attempts": 1,
      "created_at": "2024-01-15T10:31:10Z",
      "last_attempt_at": "2024-01-15T10:31:11Z",
      "response_code": 200
    }
  ],
  "total": 1,
  "limit": 10,
  "offset": 0
}
```

### Retry Webhook

**Endpoint:** `POST /api/v1/webhooks/{webhook_id}/retry`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "pending",
  "message": "Webhook retry scheduled"
}
```

---

## Testing & Status

### Job Queue Status

**Endpoint:** `GET /api/v1/test/jobs/status`

**Authentication:** Not required

**Description:** Get status of background job processing. Useful for testing.

**Response (200 OK):**
```json
{
  "pending": 5,
  "processing": 2,
  "completed": 100,
  "failed": 0,
  "worker_status": "running"
}
```

### Health Check

**Endpoint:** `GET /health`

**Authentication:** Not required

**Response (200 OK):**
```json
{
  "status": "UP"
}
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| BAD_REQUEST_ERROR | 400 | Invalid request parameters |
| NOT_FOUND_ERROR | 404 | Resource not found |
| UNAUTHORIZED_ERROR | 401 | Invalid API credentials |
| VALIDATION_ERROR | 400 | Input validation failed |
| PAYMENT_FAILED | 400 | Payment processing failed |
| REFUND_AMOUNT_ERROR | 400 | Refund amount exceeds available amount |
| INTERNAL_SERVER_ERROR | 500 | Server error |

**Error Response Format:**
```json
{
  "error": {
    "code": "BAD_REQUEST_ERROR",
    "description": "order_id is required"
  }
}
```

---

## Examples

### Complete Payment Flow

#### 1. Create Order
```bash
curl -X POST http://localhost:8000/api/v1/orders \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "INR",
    "receipt": "receipt_123"
  }'
```

#### 2. Create Payment
```bash
curl -X POST http://localhost:8000/api/v1/payments \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Idempotency-Key: req_unique_123" \
  -H "Content-Type: application/json" \
  -d '{
    "order_id": "order_NXhj67fGH2jk9mPq",
    "method": "upi",
    "vpa": "user@paytm"
  }'
```

#### 3. Poll Payment Status (wait for processing)
```bash
# Retry this every 1-2 seconds until status changes to 'success' or 'failed'
curl -X GET http://localhost:8000/api/v1/payments/pay_H8sK3jD9s2L1pQr \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789"
```

#### 4. Capture Payment
```bash
curl -X POST http://localhost:8000/api/v1/payments/pay_H8sK3jD9s2L1pQr/capture \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000
  }'
```

#### 5. Create Refund
```bash
curl -X POST http://localhost:8000/api/v1/payments/pay_H8sK3jD9s2L1pQr/refunds \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25000,
    "reason": "Customer requested refund"
  }'
```

### Webhook Signature Verification

```javascript
const crypto = require('crypto');

// Receive webhook
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);
  
  // Verify signature
  const expectedSignature = crypto
    .createHmac('sha256', 'whsec_test_abc123')
    .update(payload)
    .digest('hex');
  
  if (signature !== expectedSignature) {
    return res.status(401).send('Invalid signature');
  }
  
  // Process webhook
  console.log('Event:', req.body.event);
  console.log('Payment ID:', req.body.data.payment.id);
  
  res.status(200).send('OK');
});
```

---

## Rate Limiting

Currently no rate limits. In production, implement appropriate rate limiting per merchant.

## Pagination

List endpoints support pagination via `limit` and `offset` query parameters:
- `limit`: Maximum 100 (default: 10)
- `offset`: Starting position (default: 0)

---

## Idempotency

Use the `Idempotency-Key` header to prevent duplicate charges on network retries:

```bash
curl -X POST http://localhost:8000/api/v1/payments \
  -H "Idempotency-Key: unique_request_id_from_client" \
  ... (other headers and body)
```

The same key will return the same response for 24 hours.

---

## Webhook Events

### Supported Events

1. **payment.created** - Fired when payment is created
2. **payment.pending** - Fired when payment enters pending state
3. **payment.success** - Fired when payment succeeds
4. **payment.failed** - Fired when payment fails
5. **refund.created** - Fired when refund is initiated
6. **refund.processed** - Fired when refund completes

### Webhook Payload Format

```json
{
  "event": "payment.success",
  "timestamp": 1705315870,
  "data": {
    "payment": {
      "id": "pay_H8sK3jD9s2L1pQr",
      "order_id": "order_NXhj67fGH2jk9mPq",
      "amount": 50000,
      "currency": "INR",
      "method": "upi",
      "status": "success",
      "created_at": "2024-01-15T10:31:00Z"
    }
  }
}
```

### Webhook Retry Schedule

Webhooks are retried with exponential backoff:
- Attempt 1: Immediate
- Attempt 2: After 1 minute
- Attempt 3: After 5 minutes
- Attempt 4: After 30 minutes
- Attempt 5: After 2 hours

After 5 failed attempts, the webhook is marked as permanently failed. Manual retry is available via the API.

---

## Support

For issues or questions:
1. Check the dashboard documentation at `/dashboard/docs`
2. Review webhook configuration at `/dashboard/webhooks`
3. Monitor job status at `/api/v1/test/jobs/status`
4. Check payment status via GET `/api/v1/payments/{id}`

---

*Last Updated: January 16, 2026*
*Version: 2.0*
