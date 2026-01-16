# Quick Reference - Payment Gateway Implementation

## 🎯 What Was Built

| Component | Status | Location |
|-----------|--------|----------|
| SDK Bundle (checkout.js) | ✅ Complete | `checkout-page/checkout.js` (11 KB) |
| Iframe Bundle (checkout-iframe.js) | ✅ Complete | `checkout-page/checkout-iframe.js` (156 KB) |
| Checkout Page | ✅ Complete | `checkout-page/index.html` |
| Iframe Template | ✅ Complete | `checkout-page/iframe.html` |
| React Form Component | ✅ Complete | `checkout-widget/src/iframe-content/CheckoutForm.jsx` |
| Dashboard Documentation | ✅ Complete | `backend/src/main/resources/templates/dashboard-docs.html` |
| Build Configuration | ✅ Complete | `checkout-widget/webpack.config.js` |

---

## 🚀 Quick Start

### 1. Start Docker Services
```bash
docker-compose up -d
```

### 2. Open Checkout Page
```
http://localhost:3001
```

### 3. Test Payment
- Order ID: `order_123`
- Amount: `500`
- Name: `John Doe`
- Email: `john@example.com`
- Phone: `+919876543210`
- Click: "Pay with Payment Gateway"

### 4. In Payment Modal
- Method: UPI
- VPA: `user@paytm`
- Click: "Pay ₹500"

### 5. Success!
- Message: "✓ Payment successful!"

---

## 📋 Test IDs for Automation

```javascript
data-test-id="payment-modal"           // Modal container
data-test-id="payment-iframe"          // Iframe element
data-test-id="close-modal-button"      // Close button
```

---

## 🔌 SDK Integration Example

```html
<!DOCTYPE html>
<html>
<head>
  <title>My Checkout</title>
</head>
<body>
  <h1>Purchase Item</h1>
  <button onclick="pay()">Pay Now</button>

  <script src="http://localhost:3001/checkout.js"></script>
  
  <script>
    async function pay() {
      // Create order on your backend
      const order = await fetch('/create-order', {
        method: 'POST',
        body: JSON.stringify({ amount: 50000 })
      }).then(r => r.json());

      // Initialize SDK
      const gateway = new PaymentGateway({
        key: 'your_api_key_here',
        orderId: order.id,
        onSuccess: (data) => {
          alert('Payment successful! ID: ' + data.paymentId);
        },
        onFailure: (error) => {
          alert('Payment failed: ' + error.message);
        }
      });

      // Open modal
      gateway.open();
    }
  </script>
</body>
</html>
```

---

## 🔑 Test Credentials

| Item | Value |
|------|-------|
| API Key | `key_test_abc123` |
| API Secret | `secret_test_xyz789` |
| Test UPI | `user@paytm` |
| Test Card | `4111 1111 1111 1111` |
| Card Expiry | Any future date |
| Card CVV | Any 3 digits |

---

## 📡 API Endpoints

### Create Order
```bash
POST /api/v1/orders
Authorization: X-Api-Key, X-Api-Secret
Body: { "amount": 50000, "currency": "INR" }
```

### Create Payment
```bash
POST /api/v1/payments
Authorization: X-Api-Key, X-Api-Secret
Body: {
  "order_id": "order_123",
  "method": "upi",
  "vpa": "user@paytm"
}
```

### Check Payment Status
```bash
GET /api/v1/payments/{paymentId}
Authorization: X-Api-Key, X-Api-Secret
```

### Refund Payment
```bash
PUT /api/v1/payments/{paymentId}/refund
Authorization: X-Api-Key, X-Api-Secret
Body: { "reason": "customer_request" }
```

---

## 📂 File Structure

```
checkout-page/                    ← Production Files
├── index.html                     ← Demo checkout page
├── iframe.html                    ← Payment form iframe
├── checkout.js                    ← SDK for merchants
└── checkout-iframe.js             ← Form bundle

checkout-widget/src/               ← Source Code
├── sdk/
│   ├── PaymentGateway.js
│   ├── modal.js
│   ├── styles.css
│   └── index.js
└── iframe-content/
    ├── CheckoutForm.jsx
    ├── index.jsx
    ├── index.html
    └── styles.css
```

---

## 🔧 Available Commands

```bash
# Development
cd checkout-widget
npm run dev                   # Watch mode

# Production Build
npm run build               # Creates dist files

# Verify Files
ls -lah dist/              # Check output bundles
```

---

## 🎨 Customize (Optional)

### Update API Endpoints
In `checkout-page/index.html`:
```javascript
// Change this:
const response = await fetch('http://localhost:8000/api/v1/orders', {
// To your production URL:
const response = await fetch('https://api.yourdomain.com/v1/orders', {
```

### Update SDK Source
In `checkout-widget/src/sdk/PaymentGateway.js`:
```javascript
// Change:
return `${protocol}//localhost:3001`;
// To:
return `${protocol}//checkout.yourdomain.com`;
```

### Rebuild After Changes
```bash
npm run build
cp dist/* ../checkout-page/
```

---

## 📊 URLs Reference

| Service | URL | Port |
|---------|-----|------|
| Checkout Page | http://localhost:3001 | 3001 |
| API Server | http://localhost:8000 | 8000 |
| Dashboard | http://localhost:3000 | 3000 |
| Documentation | http://localhost:3000/dashboard/docs | 3000 |
| PostgreSQL | localhost:5432 | 5432 |
| Redis | localhost:6379 | 6379 |

---

## ✅ Verification Steps

1. Check Docker services:
   ```bash
   docker-compose ps
   ```
   All should be "UP"

2. Test API:
   ```bash
   curl http://localhost:8000/health
   # Should return: {"status":"UP"}
   ```

3. Access Checkout:
   ```bash
   open http://localhost:3001
   ```

4. View Docs:
   ```bash
   open http://localhost:3000/dashboard/docs
   ```

---

## 🐛 Troubleshooting

### SDK Not Loading
```javascript
// Check if window.PaymentGateway exists
console.log(window.PaymentGateway);
```

### Modal Not Opening
- Verify API key is correct
- Check browser console for errors
- Ensure iframe.html loads correctly

### Payment Creation Failed
- Verify backend is running (`docker-compose ps`)
- Check API credentials in headers
- Ensure order ID is valid

### Build Issues
```bash
# Clear and reinstall
rm -rf node_modules package-lock.json
npm install
npm run build
```

---

## 📞 Support Files

| Document | Purpose |
|----------|---------|
| IMPLEMENTATION_COMPLETE.md | Full technical documentation |
| DELIVERY_SUMMARY.md | High-level overview |
| QUICK_REFERENCE.md | This file - quick lookup |
| dashboard-docs.html | Interactive API documentation |

---

## 🎉 Summary

- ✅ **SDK Ready:** checkout.js is embeddable
- ✅ **Form Ready:** React component for iframe
- ✅ **Page Ready:** Demo checkout page working
- ✅ **Docs Ready:** Full API documentation
- ✅ **API Ready:** Backend endpoints available
- ✅ **Build Ready:** Production bundles created

**Everything is ready to deploy!**

---

Last Updated: 2024-01-16
Status: ✅ COMPLETE
