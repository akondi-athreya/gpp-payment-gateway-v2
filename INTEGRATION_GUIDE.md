# Test IDs & Integration Points Reference

## 🎯 Test IDs for Automation Testing

### SDK Modal (3 Primary Test IDs)

```html
<!-- Container div for modal -->
<div data-test-id="payment-modal" class="modal-overlay">
  
  <!-- Iframe element -->
  <iframe data-test-id="payment-iframe" class="payment-iframe"></iframe>
  
  <!-- Close button -->
  <button data-test-id="close-modal-button" class="close-button">×</button>
</div>
```

### Dashboard Documentation Page

```html
<!-- Navigation links -->
<div data-test-id="nav-links" class="nav-links">
  <a href="#sdk-integration">SDK Integration</a>
  <a href="#api-endpoints">API Endpoints</a>
  <a href="#webhook-setup">Webhooks</a>
  <a href="#testing">Testing</a>
</div>

<!-- Sections -->
<div data-test-id="sdk-section" id="sdk-integration">
  <!-- SDK Integration Guide -->
</div>

<div data-test-id="api-section" id="api-endpoints">
  <!-- API Endpoints -->
</div>

<div data-test-id="webhook-section" id="webhook-setup">
  <!-- Webhook Setup -->
</div>

<div data-test-id="testing-section" id="testing">
  <!-- Testing Guide -->
</div>
```

### Dashboard Webhook Page (From Previous Implementation)

```html
data-test-id="webhook-url-input"
data-test-id="webhook-secret-display"
data-test-id="regenerate-secret-btn"
data-test-id="test-webhook-btn"
data-test-id="webhook-logs-table"
data-test-id="retry-btn" (multiple)
data-test-id="delete-log-btn" (multiple)
data-test-id="pagination-prev"
data-test-id="pagination-next"
data-test-id="filter-event-select"
data-test-id="filter-status-select"
data-test-id="clear-logs-btn"
```

---

## 🔌 Integration Points

### 1. SDK Entry Point (Browser)

```javascript
// SDK is available globally after loading script
<script src="http://localhost:3001/checkout.js"></script>

// Access SDK class
window.PaymentGateway

// Initialize instance
const gateway = new PaymentGateway({
  key: string,                    // Required: API key
  orderId: string,                // Required: Order ID
  onSuccess?: function,           // Optional: Success callback
  onFailure?: function,           // Optional: Failure callback  
  onClose?: function              // Optional: Close callback
});

// Open modal
gateway.open();
```

### 2. Parent ↔ Iframe Communication (postMessage)

#### From Iframe (CheckoutForm) to Parent (SDK)

```javascript
// Success message
window.parent.postMessage({
  type: 'payment_success',
  data: {
    paymentId: string,
    orderId: string,
    amount: number,
    message: string
  }
}, '*');

// Failure message
window.parent.postMessage({
  type: 'payment_failed',
  data: {
    orderId: string,
    message: string
  }
}, '*');

// Close modal request
window.parent.postMessage({
  type: 'close_modal',
  data: {}
}, '*');
```

#### From Parent (SDK) to SDK Callbacks

```javascript
// Success callback invoked
gateway.onSuccess({
  paymentId: string,
  orderId: string,
  amount: number,
  message: string
});

// Failure callback invoked
gateway.onFailure({
  message: string
});

// Close callback invoked
gateway.onClose();
```

### 3. Frontend → Backend API Integration

#### Create Order
```
Endpoint: POST /api/v1/orders
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json

Request Body:
{
  "amount": number,           // In paise (e.g., 50000 = ₹500)
  "currency": string,         // "INR"
  "reference_id": string      // Your reference
}

Response:
{
  "id": string,               // Order ID (e.g., "order_123...")
  "amount": number,
  "currency": string,
  "status": "pending",
  "reference_id": string,
  "created_at": string
}
```

#### Create Payment
```
Endpoint: POST /api/v1/payments
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json

Request Body (UPI):
{
  "order_id": string,         // From order response
  "method": "upi",
  "vpa": string               // e.g., "user@paytm"
}

Request Body (Card):
{
  "order_id": string,
  "method": "card",
  "card": {
    "number": string,         // e.g., "4111111111111111"
    "expiry_month": number,   // 1-12
    "expiry_year": number,    // 4 digits
    "cvv": string,            // 3-4 digits
    "holder_name": string
  }
}

Response:
{
  "id": string,               // Payment ID
  "order_id": string,
  "amount": number,
  "currency": string,
  "status": "pending|success|failed",
  "method": "upi|card",
  "created_at": string
}
```

#### Get Payment Status (Polling)
```
Endpoint: GET /api/v1/payments/{paymentId}
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789

Response:
{
  "id": string,
  "order_id": string,
  "amount": number,
  "status": "pending|success|failed",
  "method": string,
  "created_at": string,
  "updated_at": string
}
```

#### Refund Payment
```
Endpoint: PUT /api/v1/payments/{paymentId}/refund
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json

Request Body:
{
  "reason": string            // "customer_request", etc.
}

Response:
{
  "id": string,
  "payment_id": string,
  "amount": number,
  "status": "pending|success|failed",
  "reason": string,
  "created_at": string
}
```

### 4. Iframe URL Parameters

When SDK opens iframe:
```
URL Format:
http://localhost:3001/iframe.html?order_id={orderId}&key={apiKey}

Parameters:
- order_id: Order ID from payment flow
- key: API key for authentication
```

### 5. Webhook Delivery (Optional)

```
Endpoint: {WEBHOOK_URL} (configured in dashboard)
Method: POST
Headers:
  Content-Type: application/json
  X-Webhook-Signature: HMAC-SHA256 signature

Body:
{
  "event": "payment.success|payment.failed|payment.refunded",
  "data": {
    "payment_id": string,
    "order_id": string,
    "amount": number,
    "status": string,
    "method": string,
    "created_at": string
  },
  "timestamp": string,
  "signature": string
}

Signature Verification:
HMAC = SHA256(JSON.stringify(body), webhook_secret)
```

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Merchant Website (Loads SDK)                                │
│ <script src="checkout.js"></script>                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ├─→ 1. Create Order (API)
                       │   POST /api/v1/orders
                       │
                       ├─→ 2. Initialize SDK
                       │   new PaymentGateway({...})
                       │
                       ├─→ 3. Open Modal
                       │   gateway.open()
                       │
                       ├─→ 4. Load Iframe
                       │   iframe.html?order_id=...&key=...
                       │
                       ├─→ 5. Iframe loads React Form
                       │   Fetch order details
                       │
                       ├─→ 6. User enters payment details
                       │
                       ├─→ 7. Submit Payment (API)
                       │   POST /api/v1/payments
                       │
                       ├─→ 8. Poll Payment Status (API)
                       │   GET /api/v1/payments/{id}
                       │
                       ├─→ 9. postMessage: payment_success
                       │   window.parent.postMessage()
                       │
                       └─→ 10. SDK Callback & Modal Close
                           gateway.onSuccess()
```

---

## 🧪 Test Automation Example (Puppeteer)

```javascript
// Initialize Payment Gateway
await page.evaluate(() => {
  window.testGateway = new PaymentGateway({
    key: 'key_test_abc123',
    orderId: 'order_123',
    onSuccess: (data) => {
      window.testResult = { success: true, data };
    }
  });
});

// Open modal
await page.evaluate(() => window.testGateway.open());

// Wait for modal to appear
await page.waitForSelector('[data-test-id="payment-modal"]', { timeout: 5000 });

// Verify iframe loaded
const iframe = await page.$('[data-test-id="payment-iframe"]');
expect(iframe).toBeTruthy();

// Switch to iframe and fill form
const frameHandle = await page.$('[data-test-id="payment-iframe"]');
const frame = await frameHandle.contentFrame();

await frame.fill('#vpa', 'user@paytm');
await frame.click('button[type="submit"]');

// Wait for success message
await page.waitForFunction(() => window.testResult?.success, { timeout: 10000 });

// Verify modal closed
const modal = await page.$('[data-test-id="payment-modal"]');
expect(modal).toBeFalsy();
```

---

## 🔐 Security Integration Points

### CORS (Cross-Origin Resource Sharing)
- Iframe src: Same origin allowed
- postMessage: No origin check (uses *)
- Backend: Configure CORS headers for API

### API Key Security
- Never expose in frontend source code
- Pass via SDK initialization only
- Backend validates on each request

### HMAC-SHA256 Signature
- Used for webhook verification
- Client: Create signature when sending webhook
- Server: Verify signature on receipt

---

## 📝 Error Handling Integration

### SDK Level
```javascript
// Constructor throws
new PaymentGateway({})  // Error: key required

// open() catches and calls onFailure
gateway.open()
// If error: calls this.onFailure({message: 'Error message'})
```

### Form Level
```javascript
// Validation errors
if (!formData.vpa) {
  throw new Error('VPA is required');
}

// API errors
if (!response.ok) {
  throw new Error(payment.error?.description);
}

// Timeout errors
if (attempt === maxAttempts - 1) {
  throw new Error('Payment processing timeout');
}
```

### postMessage Level
```javascript
// Messages sent as objects
{
  type: 'payment_success|payment_failed|close_modal',
  data: { /* response data */ }
}

// Only specific message types processed
```

---

## 🎯 Testing Checklist

- [ ] SDK loads via <script> tag
- [ ] Modal opens on gateway.open()
- [ ] Modal contains iframe with test-id
- [ ] Close button present and clickable
- [ ] Iframe loads checkout form
- [ ] Form validation working
- [ ] Payment API call successful
- [ ] Status polling works
- [ ] postMessage received in parent
- [ ] onSuccess callback invoked
- [ ] Modal closes after success
- [ ] Error handling works
- [ ] Mobile responsive

---

## 📞 Support Integration

### For SDK Issues
1. Check browser console for errors
2. Verify API key is correct
3. Ensure iframe loads (Network tab)
4. Check postMessage in DevTools

### For API Issues
1. Verify backend is running
2. Check API key/secret headers
3. Test with cURL first
4. Review API documentation in dashboard

### For Form Issues
1. Check iframe content loads
2. Verify order_id parameter passed
3. Test form fields individually
4. Check API responses

---

**Last Updated:** 2024-01-16
**Version:** 1.0 (Production Ready)
