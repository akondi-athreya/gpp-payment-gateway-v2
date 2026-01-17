import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './components/Dashboard'
import Transactions from './pages/Transactions'
import Webhooks from './pages/Webhooks'
import Docs from './pages/Docs'
import Refunds from './pages/Refunds'
import Status from './pages/Status'

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/dashboard/transactions" element={<Transactions />} />
        <Route path="/dashboard/webhooks" element={<Webhooks />} />
        <Route path="/dashboard/docs" element={<Docs />} />
        <Route path="/dashboard/refunds" element={<Refunds />} />
        <Route path="/dashboard/status" element={<Status />} />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  )
}
