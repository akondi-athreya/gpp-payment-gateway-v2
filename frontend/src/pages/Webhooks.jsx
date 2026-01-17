import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import apiService from '../services/api'
import './Webhooks.css'

function Webhooks() {
  const [webhookUrl, setWebhookUrl] = useState('')
  const [webhookSecret, setWebhookSecret] = useState('')
  const [webhookLogs, setWebhookLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actionMsg, setActionMsg] = useState(null)
  const [logsLoading, setLogsLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const isAuth = localStorage.getItem('isAuthenticated')
    if (!isAuth) {
      navigate('/login')
      return
    }
    loadInitialData()
  }, [navigate])

  const loadInitialData = async () => {
    setLoading(true)
    try {
      // Load initial config (optional - backend may not have endpoint for get config)
      // For now, initialize with empty state
      setLoading(false)
    } catch (err) {
      setError('Failed to load webhook config: ' + err.message)
      setLoading(false)
    }
  }

  const loadWebhookLogs = async () => {
    setLogsLoading(true)
    try {
      const res = await apiService.getWebhookLogs(20, 0)
      setWebhookLogs(res.data || [])
    } catch (err) {
      setError('Failed to load webhook logs: ' + err.message)
    } finally {
      setLogsLoading(false)
    }
  }

  const saveWebhookConfig = async (e) => {
    e.preventDefault()
    setActionMsg(null)
    try {
      const res = await apiService.saveWebhookConfig(webhookUrl)
      setWebhookUrl(res.webhook_url || webhookUrl)
      setActionMsg('✓ Webhook URL saved successfully')
    } catch (err) {
      setActionMsg('✗ Save failed: ' + (err.message || 'Unknown error'))
    }
  }

  const regenerateSecret = async () => {
    setActionMsg(null)
    try {
      const res = await apiService.regenerateWebhookSecret()
      setWebhookSecret(res.webhook_secret || '')
      setActionMsg('✓ Secret regenerated successfully')
    } catch (err) {
      setActionMsg('✗ Regenerate failed: ' + (err.message || 'Unknown error'))
    }
  }

  const sendTestWebhook = async () => {
    setActionMsg(null)
    try {
      await apiService.sendTestWebhook()
      setActionMsg('✓ Test webhook enqueued')
      setTimeout(() => loadWebhookLogs(), 1000)
    } catch (err) {
      setActionMsg('✗ Test failed: ' + (err.message || 'Unknown error'))
    }
  }

  const retryWebhook = async (id) => {
    try {
      await apiService.retryWebhook(id)
      setActionMsg('✓ Retry scheduled')
      setTimeout(() => loadWebhookLogs(), 500)
    } catch (err) {
      setActionMsg('✗ Retry failed: ' + (err.message || 'Unknown error'))
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

  if (loading) {
    return <div className="webhooks-container"><div className="loading">Loading...</div></div>
  }

  return (
    <div className="webhooks-container">
      <header className="webhooks-header">
        <h1>Webhook Configuration</h1>
      </header>

      {error && <div className="error-message">{error}</div>}
      {actionMsg && <div className={actionMsg.startsWith('✓') ? 'success-message' : 'error-message'}>{actionMsg}</div>}

      <div className="webhooks-content">
        <div className="webhook-section">
          <div className="section-card">
            <h2>Configuration</h2>
            <form onSubmit={saveWebhookConfig}>
              <div className="form-group">
                <label>Webhook URL</label>
                <input
                  type="url"
                  data-test-id="webhook-url-input"
                  value={webhookUrl}
                  onChange={(e) => setWebhookUrl(e.target.value)}
                  placeholder="https://yoursite.com/webhook"
                  required
                />
              </div>

              <div className="form-group">
                <label>Webhook Secret</label>
                <div className="secret-display">
                  <span data-test-id="webhook-secret">{webhookSecret || 'Not generated yet'}</span>
                  <button
                    type="button"
                    data-test-id="regenerate-secret-button"
                    onClick={regenerateSecret}
                    className="btn-secondary"
                  >
                    Regenerate
                  </button>
                </div>
              </div>

              <div className="form-actions">
                <button type="submit" className="btn-primary" data-test-id="save-webhook-button">
                  Save Configuration
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  data-test-id="test-webhook-button"
                  onClick={sendTestWebhook}
                >
                  Send Test Webhook
                </button>
              </div>
            </form>
          </div>
        </div>

        <div className="webhook-section">
          <div className="section-card">
            <h2>Delivery Logs</h2>
            <button
              onClick={loadWebhookLogs}
              disabled={logsLoading}
              className="btn-secondary"
              style={{ marginBottom: '15px' }}
            >
              {logsLoading ? 'Loading...' : 'Refresh Logs'}
            </button>

            {webhookLogs.length > 0 ? (
              <table data-test-id="webhook-logs-table" className="logs-table">
                <thead>
                  <tr>
                    <th>Event</th>
                    <th>Status</th>
                    <th>Attempts</th>
                    <th>Last Attempt</th>
                    <th>Response Code</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {webhookLogs.map((log) => (
                    <tr key={log.id} data-test-id="webhook-log-item" data-webhook-id={log.id}>
                      <td data-test-id="webhook-event">{log.event}</td>
                      <td data-test-id="webhook-status">{log.status}</td>
                      <td data-test-id="webhook-attempts">{log.attempts}</td>
                      <td data-test-id="webhook-last-attempt">{formatDate(log.last_attempt_at)}</td>
                      <td data-test-id="webhook-response-code">{log.response_code || '—'}</td>
                      <td>
                        <button
                          data-test-id="retry-webhook-button"
                          data-webhook-id={log.id}
                          onClick={() => retryWebhook(log.id)}
                          className="btn-small"
                          disabled={log.status === 'success' || log.attempts >= 5}
                        >
                          Retry
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="empty-state">No webhook logs yet. Send a test webhook to get started.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Webhooks
