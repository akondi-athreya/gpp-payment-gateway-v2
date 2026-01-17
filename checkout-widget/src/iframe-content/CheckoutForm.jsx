import React, { useState, useEffect } from 'react';
import './styles.css';

/**
 * CheckoutForm - Embedded payment form shown in iframe
 */
export default function CheckoutForm() {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [method, setMethod] = useState('upi');
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    vpa: '',
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    cvv: '',
    holderName: ''
  });

  // Fetch order on mount
  useEffect(() => {
    fetchOrder();
  }, []);

  // Fetch order details from parent
  const fetchOrder = async () => {
    try {
      const params = new URLSearchParams(window.location.search);
      const orderId = params.get('order_id');

      if (!orderId) {
        setError('Order ID not provided');
        setLoading(false);
        return;
      }

      const response = await fetch(`http://localhost:8000/api/v1/orders/${orderId}`);
      if (!response.ok) {
        throw new Error('Failed to fetch order');
      }

      const data = await response.json();
      setOrder(data);
      setLoading(false);
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  // Send message to parent window
  const sendToParent = (type, data) => {
    window.parent.postMessage({ type, data }, '*');
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      // Validate form
      if (method === 'upi' && !formData.vpa) {
        throw new Error('VPA is required');
      }
      if (method === 'card' && !formData.cardNumber) {
        throw new Error('Card number is required');
      }

      // Prepare payment data
      const paymentData = {
        order_id: order.id,
        method,
        ...(method === 'upi' ? { vpa: formData.vpa } : {}),
        ...(method === 'card' ? {
          card: {
            number: formData.cardNumber,
            expiry_month: parseInt(formData.expiryMonth),
            expiry_year: parseInt(formData.expiryYear),
            cvv: formData.cvv,
            holder_name: formData.holderName
          }
        } : {})
      };

      // Get API key from parent (passed via iframe src)
      const params = new URLSearchParams(window.location.search);
      const apiKey = params.get('key');

      // Create payment
      const response = await fetch('http://localhost:8000/api/v1/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Api-Key': apiKey || 'key_test_abc123',
          'X-Api-Secret': 'secret_test_xyz789'
        },
        body: JSON.stringify(paymentData)
      });

      const payment = await response.json();

      if (!response.ok) {
        throw new Error(payment.error?.description || 'Payment creation failed');
      }

      // Poll for payment completion
      await pollPaymentStatus(payment.id, apiKey);

      // Send success message to parent
      sendToParent('payment_success', {
        paymentId: payment.id,
        orderId: order.id,
        amount: order.amount,
        message: 'Payment successful'
      });
    } catch (err) {
      console.error('Payment error:', err);
      sendToParent('payment_failed', {
        orderId: order.id,
        message: err.message
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Poll payment status until completion
  const pollPaymentStatus = async (paymentId, apiKey) => {
    const maxAttempts = 30;
    const interval = 1000; // 1 second

    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        const response = await fetch(
          `http://localhost:8000/api/v1/payments/${paymentId}`,
          {
            headers: {
              'X-Api-Key': apiKey || 'key_test_abc123',
              'X-Api-Secret': 'secret_test_xyz789'
            }
          }
        );

        const payment = await response.json();

        // Check if payment is completed
        if (payment.status === 'success') {
          return payment;
        } else if (payment.status === 'failed') {
          throw new Error('Payment processing failed');
        }

        // Wait before next attempt
        await new Promise(resolve => setTimeout(resolve, interval));
      } catch (err) {
        console.error('Status check error:', err);
        if (attempt === maxAttempts - 1) {
          throw err;
        }
      }
    }

    throw new Error('Payment processing timeout');
  };

  // Handle close
  const handleClose = () => {
    sendToParent('close_modal', {});
  };

  if (loading) {
    return (
      <div className="checkout-container">
        <div className="spinner"></div>
        <p>Loading order details...</p>
      </div>
    );
  }

  if (error && !order) {
    return (
      <div className="checkout-container">
        <div className="error-message">
          <p>❌ {error}</p>
          <button onClick={handleClose} className="close-btn">
            Close
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="checkout-container" data-test-id="checkout-form">
      <div className="checkout-header">
        <h1>Complete Payment</h1>
        <button
          type="button"
          className="header-close"
          data-test-id="checkout-close-button"
          onClick={handleClose}
          title="Close payment form"
        >
          ×
        </button>
      </div>

      {order && (
        <>
          <div className="order-summary" data-test-id="order-summary">
            <div className="summary-row">
              <span>Order ID:</span>
              <strong data-test-id="order-id">{order.id}</strong>
            </div>
            <div className="summary-row">
              <span>Amount:</span>
              <strong className="amount" data-test-id="order-amount">₹{(order.amount / 100).toLocaleString('en-IN')}</strong>
            </div>
            <div className="summary-row">
              <span>Currency:</span>
              <strong data-test-id="order-currency">{order.currency}</strong>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="payment-form">
            {/* Payment Method Selection */}
            <div className="form-section">
              <label>Payment Method</label>
              <div className="method-tabs" data-test-id="payment-method-tabs">
                <button
                  type="button"
                  className={`tab ${method === 'upi' ? 'active' : ''}`}
                  onClick={() => setMethod('upi')}
                  data-test-id="method-upi"
                >
                  UPI
                </button>
                <button
                  type="button"
                  className={`tab ${method === 'card' ? 'active' : ''}`}
                  onClick={() => setMethod('card')}
                  data-test-id="method-card"
                >
                  Card
                </button>
              </div>
            </div>

            {/* UPI Form */}
            {method === 'upi' && (
              <div className="form-section" data-test-id="upi-form">
                <label htmlFor="vpa">UPI Address (VPA)</label>
                <input
                  id="vpa"
                  type="text"
                  placeholder="user@paytm"
                  value={formData.vpa}
                  onChange={(e) => setFormData({ ...formData, vpa: e.target.value })}
                  data-test-id="upi-vpa-input"
                  required
                  disabled={submitting}
                />
                <small>Example: user@paytm, user@googleplay, user@okhdfcbank</small>
              </div>
            )}

            {/* Card Form */}
            {method === 'card' && (
              <div className="form-section" data-test-id="card-form">
                <label htmlFor="cardNumber">Card Number</label>
                <input
                  id="cardNumber"
                  type="text"
                  placeholder="4111 1111 1111 1111"
                  value={formData.cardNumber}
                  onChange={(e) => setFormData({ ...formData, cardNumber: e.target.value })}
                  data-test-id="card-number-input"
                  maxLength="19"
                  required
                  disabled={submitting}
                />

                <div className="form-row">
                  <div className="form-col">
                    <label htmlFor="expiryMonth">Expiry Month</label>
                    <input
                      id="expiryMonth"
                      type="number"
                      placeholder="MM"
                      min="1"
                      max="12"
                      value={formData.expiryMonth}
                      onChange={(e) => setFormData({ ...formData, expiryMonth: e.target.value })}
                      data-test-id="card-expiry-month-input"
                      required
                      disabled={submitting}
                    />
                  </div>
                  <div className="form-col">
                    <label htmlFor="expiryYear">Expiry Year</label>
                    <input
                      id="expiryYear"
                      type="number"
                      placeholder="YYYY"
                      value={formData.expiryYear}
                      onChange={(e) => setFormData({ ...formData, expiryYear: e.target.value })}
                      data-test-id="card-expiry-year-input"
                      required
                      disabled={submitting}
                    />
                  </div>
                </div>

                <label htmlFor="cvv">CVV</label>
                <input
                  id="cvv"
                  type="text"
                  placeholder="123"
                  maxLength="4"
                  value={formData.cvv}
                  onChange={(e) => setFormData({ ...formData, cvv: e.target.value })}
                  data-test-id="card-cvv-input"
                  required
                  disabled={submitting}
                />

                <label htmlFor="holderName">Cardholder Name</label>
                <input
                  id="holderName"
                  type="text"
                  placeholder="Full Name"
                  value={formData.holderName}
                  onChange={(e) => setFormData({ ...formData, holderName: e.target.value })}
                  data-test-id="card-holder-name-input"
                  required
                  disabled={submitting}
                />
              </div>
            )}

            {error && (
              <div className="error-alert" data-test-id="payment-error">
                <p>❌ {error}</p>
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              className="submit-btn"
              data-test-id="submit-payment-button"
              disabled={submitting}
            >
              {submitting ? 'Processing...' : `Pay ₹${(order.amount / 100).toLocaleString('en-IN')}`}
            </button>
          </form>

          <p className="secure-notice">🔒 Secure payment powered by Payment Gateway</p>
        </>
      )}
    </div>
  );
}
