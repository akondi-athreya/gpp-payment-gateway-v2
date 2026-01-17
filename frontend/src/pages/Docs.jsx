import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './Docs.css'

function Docs() {
  const navigate = useNavigate()
  const apiKey = import.meta.env.VITE_API_KEY || 'key_test_abc123'
  const apiSecret = import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'

  useEffect(() => {
    const isAuth = localStorage.getItem('isAuthenticated')
    if (!isAuth) {
      navigate('/login')
    }
  }, [navigate])

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
  }

  return (
    <div className="docs-container" data-test-id="api-docs">
      <header className="docs-header">
        <h1>Integration Guide</h1>
        <p>Complete documentation for integrating the Payment Gateway API</p>
      </header>

      <div className="docs-content">
        {/* Create Order Section */}
        <section data-test-id="section-create-order" className="doc-section">
          <h2>Create an Order</h2>
          <p>Create a new order that can be paid for</p>
          <div className="code-block">
            <div className="code-header">
              <span>curl</span>
              <button onClick={() => copyToClipboard(curlCreateOrder)} className="btn-copy">Copy</button>
            </div>
            <pre data-test-id="code-snippet-create-order"><code>{curlCreateOrder}</code></pre>
          </div>
          <div className="doc-info">
            <strong>Response:</strong>
            <pre><code>{JSON.stringify(orderResponse, null, 2)}</code></pre>
          </div>
        </section>

        {/* Get Payment Section */}
        <section data-test-id="section-get-payment" className="doc-section">
          <h2>Check Payment Status</h2>
          <p>Retrieve payment details by payment ID</p>
          <div className="code-block">
            <div className="code-header">
              <span>curl</span>
              <button onClick={() => copyToClipboard(curlGetPayment)} className="btn-copy">Copy</button>
            </div>
            <pre data-test-id="code-snippet-get-payment"><code>{curlGetPayment}</code></pre>
          </div>
          <div className="doc-info">
            <strong>Response:</strong>
            <pre><code>{JSON.stringify(paymentResponse, null, 2)}</code></pre>
          </div>
        </section>

        {/* SDK Integration Section */}
        <section data-test-id="section-sdk-integration" className="doc-section">
          <h2>SDK Integration (Browser)</h2>
          <p>Embed the payment gateway modal in your checkout page</p>
          <div className="code-block">
            <div className="code-header">
              <span>html</span>
              <button onClick={() => copyToClipboard(htmlSDKIntegration)} className="btn-copy">Copy</button>
            </div>
            <pre data-test-id="code-snippet-sdk"><code>{htmlSDKIntegration}</code></pre>
          </div>
          <div className="doc-info">
            <p><strong>Note:</strong> The SDK creates a global <code>PaymentGateway</code> object. Use <code>PaymentGateway.openModal(config)</code> to open the payment modal.</p>
          </div>
        </section>

        {/* Webhook Verification Section */}
        <section data-test-id="section-webhook-verification" className="doc-section">
          <h2>Verify Webhook Signature</h2>
          <p>Verify webhook authenticity using the secret</p>
          <div className="code-block">
            <div className="code-header">
              <span>javascript</span>
              <button onClick={() => copyToClipboard(webhookVerification)} className="btn-copy">Copy</button>
            </div>
            <pre data-test-id="code-snippet-webhook"><code>{webhookVerification}</code></pre>
          </div>
          <div className="doc-info">
            <p><strong>Important:</strong> Always verify the X-Webhook-Signature header to ensure the webhook comes from Payment Gateway, not an attacker.</p>
          </div>
        </section>

        {/* Refunds Section */}
        <section className="doc-section">
          <h2>Create a Refund</h2>
          <p>Refund a payment (full or partial)</p>
          <div className="code-block">
            <div className="code-header">
              <span>curl</span>
              <button onClick={() => copyToClipboard(curlCreateRefund)} className="btn-copy">Copy</button>
            </div>
            <pre><code>{curlCreateRefund}</code></pre>
          </div>
        </section>

        {/* Headers Info */}
        <section className="doc-section">
          <h2>Authentication Headers</h2>
          <p>Include these headers in all API requests</p>
          <div className="code-block">
            <pre><code>{headersInfo}</code></pre>
          </div>
        </section>
      </div>
    </div>
  )
}

// Code snippets with env variables
const curlCreateOrder = `curl -X POST ${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'}/orders \\
  -H "Content-Type: application/json" \\
  -H "X-Api-Key: ${import.meta.env.VITE_API_KEY || 'key_test_abc123'}" \\
  -H "X-Api-Secret: ${import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'}" \\
  -d '{
    "amount": 9999,
    "currency": "INR",
    "description": "Widget Purchase"
  }'`

const orderResponse = {
  status: 'success',
  order_id: 'ord_abc123def456',
  amount: 9999,
  currency: 'INR',
  description: 'Widget Purchase',
  created_at: '2024-01-15T10:30:00Z'
}

const curlGetPayment = `curl -X GET ${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'}/payments/pay_xyz789abc123 \\
  -H "X-Api-Key: ${import.meta.env.VITE_API_KEY || 'key_test_abc123'}" \\
  -H "X-Api-Secret: ${import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'}"`

const paymentResponse = {
  status: 'success',
  payment_id: 'pay_xyz789abc123',
  order_id: 'ord_abc123def456',
  amount: 9999,
  currency: 'INR',
  payment_method: 'card',
  payment_status: 'completed',
  created_at: '2024-01-15T10:31:00Z',
  completed_at: '2024-01-15T10:32:15Z'
}

const htmlSDKIntegration = `<!-- Add script to your page -->
<script src="${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'}/checkout.js"></script>

<!-- When user clicks checkout -->
<script>
  PaymentGateway.openModal({
    key: '${import.meta.env.VITE_API_KEY || 'key_test_abc123'}',
    order_id: 'ord_abc123def456',
    amount: 9999,
    currency: 'INR',
    onSuccess: (response) => {
      console.log('Payment successful:', response);
      // Verify payment status via API
    },
    onError: (error) => {
      console.log('Payment failed:', error);
    }
  });
</script>`

const webhookVerification = `const crypto = require('crypto');

// Verify webhook signature
function verifyWebhook(body, signature, secret) {
  const hash = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(body))
    .digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(hash),
    Buffer.from(signature)
  );
}

// In your webhook handler
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const secret = '${import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'}';
  
  if (!verifyWebhook(req.body, signature, secret)) {
    return res.status(401).json({ error: 'Invalid signature' });
  }
  
  // Process webhook
  console.log('Webhook verified:', req.body);
  res.json({ success: true });
});`

const curlCreateRefund = `curl -X POST ${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'}/refunds \\
  -H "Content-Type: application/json" \\
  -H "X-Api-Key: ${import.meta.env.VITE_API_KEY || 'key_test_abc123'}" \\
  -H "X-Api-Secret: ${import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'}" \\
  -d '{
    "payment_id": "pay_xyz789abc123",
    "amount": 5000,
    "reason": "Customer requested"
  }'`

const headersInfo = `X-Api-Key: ${import.meta.env.VITE_API_KEY || 'key_test_abc123'}
X-Api-Secret: ${import.meta.env.VITE_API_SECRET || 'secret_test_xyz789'}
Content-Type: application/json`

export default Docs
