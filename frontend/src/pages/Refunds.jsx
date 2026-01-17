import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import apiService from '../services/api'
import './Refunds.css'

function Refunds() {
  const [paymentId, setPaymentId] = useState('')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [refundResult, setRefundResult] = useState(null)
  const [lookupId, setLookupId] = useState('')
  const [lookupResult, setLookupResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [actionMsg, setActionMsg] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    const isAuth = localStorage.getItem('isAuthenticated')
    if (!isAuth) {
      navigate('/login')
    }
  }, [navigate])

  const handleCreateRefund = async (e) => {
    e.preventDefault()
    setLoading(true)
    setActionMsg(null)
    setError(null)
    setRefundResult(null)

    try {
      const refundAmount = amount ? parseInt(amount) : null
      const res = await apiService.createRefund(paymentId, {
        amount: refundAmount,
        reason: reason
      })

      setRefundResult(res)
      setActionMsg('✓ Refund created successfully')
      setPaymentId('')
      setAmount('')
      setReason('')
    } catch (err) {
      setError('Failed to create refund: ' + (err.message || 'Unknown error'))
      setActionMsg('✗ Refund creation failed')
    } finally {
      setLoading(false)
    }
  }

  const handleLookupRefund = async (e) => {
    e.preventDefault()
    setLoading(true)
    setActionMsg(null)
    setError(null)

    try {
      const res = await apiService.getRefund(lookupId)
      setLookupResult(res)
      setActionMsg('✓ Refund found')
    } catch (err) {
      setError('Refund not found: ' + (err.message || 'Unknown error'))
      setActionMsg('✗ Lookup failed')
      setLookupResult(null)
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return '—'
    const date = new Date(dateString)
    return date.toLocaleString('en-IN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  const formatCurrency = (amount) => {
    if (!amount) return '₹0'
    return '₹' + (amount / 100).toFixed(2)
  }

  return (
    <div className="refunds-container">
      <header className="refunds-header">
        <h1>Refunds Management</h1>
      </header>

      {actionMsg && <div className={actionMsg.startsWith('✓') ? 'success-message' : 'error-message'}>{actionMsg}</div>}
      {error && <div className="error-message">{error}</div>}

      <div className="refunds-content">
        {/* Create Refund Form */}
        <div className="refund-section">
          <div className="section-card">
            <h2>Create Refund</h2>
            <p className="section-description">Create a new refund for a completed payment (full or partial)</p>

            <form onSubmit={handleCreateRefund}>
              <div className="form-group">
                <label htmlFor="payment-id">Payment ID *</label>
                <input
                  id="payment-id"
                  type="text"
                  data-test-id="refund-payment-id-input"
                  value={paymentId}
                  onChange={(e) => setPaymentId(e.target.value)}
                  placeholder="pay_abc123def456"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="refund-amount">Refund Amount (in paise, optional)</label>
                <input
                  id="refund-amount"
                  type="number"
                  data-test-id="refund-amount-input"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="5000 (for ₹50.00)"
                  min="1"
                />
                <small>Leave empty for full refund</small>
              </div>

              <div className="form-group">
                <label htmlFor="refund-reason">Reason</label>
                <select
                  id="refund-reason"
                  data-test-id="refund-reason-select"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                >
                  <option value="">Select a reason</option>
                  <option value="customer_request">Customer Request</option>
                  <option value="duplicate">Duplicate Payment</option>
                  <option value="fraud">Fraud</option>
                  <option value="payment_failed">Payment Failed</option>
                  <option value="other">Other</option>
                </select>
              </div>

              <button
                type="submit"
                className="btn-primary"
                disabled={loading}
                data-test-id="create-refund-button"
              >
                {loading ? 'Processing...' : 'Create Refund'}
              </button>
            </form>

            {refundResult && (
              <div className="result-card success">
                <h3>Refund Created</h3>
                <div className="result-content">
                  <div className="result-item">
                    <span className="label">Refund ID:</span>
                    <span data-test-id="refund-id-result" className="value">{refundResult.refund_id}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Payment ID:</span>
                    <span data-test-id="refund-payment-id-result" className="value">{refundResult.payment_id}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Amount:</span>
                    <span data-test-id="refund-amount-result" className="value">{formatCurrency(refundResult.amount)}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Status:</span>
                    <span data-test-id="refund-status-result" className="value status">{refundResult.status}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Reason:</span>
                    <span data-test-id="refund-reason-result" className="value">{refundResult.reason || '—'}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Created:</span>
                    <span data-test-id="refund-created-result" className="value">{formatDate(refundResult.created_at)}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Lookup Refund */}
        <div className="refund-section">
          <div className="section-card">
            <h2>Lookup Refund</h2>
            <p className="section-description">Search for an existing refund by ID</p>

            <form onSubmit={handleLookupRefund}>
              <div className="form-group">
                <label htmlFor="lookup-id">Refund ID</label>
                <input
                  id="lookup-id"
                  type="text"
                  data-test-id="lookup-refund-id-input"
                  value={lookupId}
                  onChange={(e) => setLookupId(e.target.value)}
                  placeholder="ref_abc123def456"
                  required
                />
              </div>

              <button
                type="submit"
                className="btn-secondary"
                disabled={loading}
                data-test-id="lookup-refund-button"
              >
                {loading ? 'Searching...' : 'Search'}
              </button>
            </form>

            {lookupResult && (
              <div className="result-card">
                <h3>Refund Details</h3>
                <div className="result-content">
                  <div className="result-item">
                    <span className="label">Refund ID:</span>
                    <span data-test-id="lookup-refund-id-result" className="value">{lookupResult.refund_id}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Payment ID:</span>
                    <span data-test-id="lookup-payment-id-result" className="value">{lookupResult.payment_id}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Amount:</span>
                    <span data-test-id="lookup-amount-result" className="value">{formatCurrency(lookupResult.amount)}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Status:</span>
                    <span data-test-id="lookup-status-result" className="value status">{lookupResult.status}</span>
                  </div>
                  <div className="result-item">
                    <span className="label">Created:</span>
                    <span data-test-id="lookup-created-result" className="value">{formatDate(lookupResult.created_at)}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Refunds
