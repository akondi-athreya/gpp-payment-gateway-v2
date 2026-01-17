import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import apiService from '../services/api'
import './Status.css'

function Status() {
  const [healthData, setHealthData] = useState(null)
  const [jobData, setJobData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const isAuth = localStorage.getItem('isAuthenticated')
    if (!isAuth) {
      navigate('/login')
      return
    }
    loadStatusData()
  }, [navigate])

  useEffect(() => {
    if (!autoRefresh) return

    const interval = setInterval(() => {
      loadStatusData()
    }, 5000)

    return () => clearInterval(interval)
  }, [autoRefresh])

  const loadStatusData = async () => {
    setLoading(true)
    try {
      const [health, jobs] = await Promise.all([
        apiService.healthCheck().catch(err => ({ status: 'error', error: err.message })),
        apiService.getJobStatus().catch(err => ({ status: 'error', error: err.message }))
      ])

      setHealthData(health)
      setJobData(jobs)
      setError(null)
    } catch (err) {
      setError('Failed to load status: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  const getStatusBadge = (status) => {
    const statusMap = {
      healthy: 'status-healthy',
      unhealthy: 'status-unhealthy',
      connected: 'status-healthy',
      disconnected: 'status-unhealthy',
      running: 'status-healthy',
      stopped: 'status-unhealthy',
      active: 'status-healthy',
      inactive: 'status-unhealthy',
      success: 'status-healthy',
      error: 'status-unhealthy'
    }
    return statusMap[status?.toLowerCase()] || 'status-unknown'
  }

  const StatusItem = ({ label, value, icon = '◯' }) => (
    <div className="status-item">
      <div className="status-label">
        <span className="icon">{icon}</span>
        <span>{label}</span>
      </div>
      <div className={`status-value ${getStatusBadge(value)}`}>{value || '—'}</div>
    </div>
  )

  if (loading && !healthData && !jobData) {
    return <div className="status-container"><div className="loading">Loading status...</div></div>
  }

  return (
    <div className="status-container">
      <header className="status-header">
        <h1>System Status</h1>
        <div className="header-controls">
          <label className="auto-refresh-toggle">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Auto-refresh every 5s
          </label>
          <button onClick={loadStatusData} disabled={loading} className="btn-refresh">
            {loading ? 'Refreshing...' : 'Refresh Now'}
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      <div className="status-grid">
        {/* Health Check Section */}
        <div className="status-section">
          <h2 className="section-title">API Health</h2>
          <div className="status-items">
            {healthData ? (
              <>
                <StatusItem label="API Status" value={healthData.status} icon="🟢" />
                <StatusItem label="Database" value={healthData.database} icon="🗄️" />
                <StatusItem label="Redis Cache" value={healthData.redis} icon="⚡" />
                <StatusItem label="Worker Service" value={healthData.worker_service} icon="⚙️" />
                {healthData.version && (
                  <div className="status-item">
                    <div className="status-label">
                      <span className="icon">📦</span>
                      <span>Version</span>
                    </div>
                    <div className="status-value">{healthData.version}</div>
                  </div>
                )}
              </>
            ) : (
              <div className="empty-state">Health check data unavailable</div>
            )}
          </div>
        </div>

        {/* Job Queue Section */}
        <div className="status-section">
          <h2 className="section-title">Job Queue Status</h2>
          <div className="job-stats">
            {jobData ? (
              <>
                <div className="job-stat-card pending">
                  <div className="stat-number" data-test-id="job-pending-count">
                    {jobData.pending || 0}
                  </div>
                  <div className="stat-label">Pending</div>
                </div>
                <div className="job-stat-card processing">
                  <div className="stat-number" data-test-id="job-processing-count">
                    {jobData.processing || 0}
                  </div>
                  <div className="stat-label">Processing</div>
                </div>
                <div className="job-stat-card completed">
                  <div className="stat-number" data-test-id="job-completed-count">
                    {jobData.completed || 0}
                  </div>
                  <div className="stat-label">Completed</div>
                </div>
                <div className="job-stat-card failed">
                  <div className="stat-number" data-test-id="job-failed-count">
                    {jobData.failed || 0}
                  </div>
                  <div className="stat-label">Failed</div>
                </div>
              </>
            ) : (
              <div className="empty-state">Job queue data unavailable</div>
            )}
          </div>
        </div>

        {/* Detailed Metrics */}
        <div className="status-section full-width">
          <h2 className="section-title">System Metrics</h2>
          <div className="metrics-table">
            <div className="metric-row">
              <div className="metric-label">Uptime Check</div>
              <div className="metric-value">
                {healthData?.uptime ? formatUptime(healthData.uptime) : '—'}
              </div>
            </div>
            {healthData?.last_health_check && (
              <div className="metric-row">
                <div className="metric-label">Last Check</div>
                <div className="metric-value">
                  {new Date(healthData.last_health_check).toLocaleString('en-IN')}
                </div>
              </div>
            )}
            {jobData?.total_jobs !== undefined && (
              <div className="metric-row">
                <div className="metric-label">Total Jobs Processed</div>
                <div className="metric-value">{jobData.total_jobs}</div>
              </div>
            )}
            {jobData?.success_rate !== undefined && (
              <div className="metric-row">
                <div className="metric-label">Success Rate</div>
                <div className="metric-value">{(jobData.success_rate * 100).toFixed(1)}%</div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="status-footer">
        <small>Data last updated: {new Date().toLocaleTimeString('en-IN')}</small>
      </div>
    </div>
  )
}

function formatUptime(seconds) {
  if (!seconds) return '—'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)

  if (days > 0) return `${days}d ${hours}h ${mins}m`
  if (hours > 0) return `${hours}h ${mins}m`
  return `${mins}m`
}

export default Status
