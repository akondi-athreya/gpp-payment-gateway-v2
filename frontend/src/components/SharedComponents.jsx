import { Link } from 'react-router-dom';
import './SharedComponents.css';

/**
 * Card component for content sections
 */
export const Card = ({ children, className = '', title, actions, dataTestId }) => {
  return (
    <div className={`card ${className}`} data-test-id={dataTestId}>
      {(title || actions) && (
        <div className="card-header">
          {title && <h3 className="card-title">{title}</h3>}
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className="card-body">{children}</div>
    </div>
  );
};

/**
 * Status badge component
 */
export const StatusBadge = ({ status, children }) => {
  const getStatusClass = () => {
    const statusMap = {
      success: 'badge-success',
      completed: 'badge-success',
      pending: 'badge-pending',
      processing: 'badge-processing',
      failed: 'badge-failed',
      error: 'badge-error',
      cancelled: 'badge-cancelled'
    };
    return statusMap[status?.toLowerCase()] || 'badge-default';
  };

  return (
    <span className={`status-badge ${getStatusClass()}`}>
      {children || status}
    </span>
  );
};

/**
 * Loading spinner component
 */
export const LoadingSpinner = ({ message = 'Loading...' }) => {
  return (
    <div className="loading-container">
      <div className="spinner"></div>
      <p className="loading-message">{message}</p>
    </div>
  );
};

/**
 * Empty state component
 */
export const EmptyState = ({ icon = '📭', title, message, action }) => {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      {title && <h3 className="empty-title">{title}</h3>}
      {message && <p className="empty-message">{message}</p>}
      {action && <div className="empty-action">{action}</div>}
    </div>
  );
};

/**
 * Alert/Message component
 */
export const Alert = ({ type = 'info', children, onClose, dataTestId }) => {
  const getAlertClass = () => {
    const typeMap = {
      success: 'alert-success',
      error: 'alert-error',
      warning: 'alert-warning',
      info: 'alert-info'
    };
    return typeMap[type] || 'alert-info';
  };

  return (
    <div className={`alert ${getAlertClass()}`} data-test-id={dataTestId}>
      <div className="alert-content">{children}</div>
      {onClose && (
        <button className="alert-close" onClick={onClose} type="button">
          ×
        </button>
      )}
    </div>
  );
};

/**
 * Button component
 */
export const Button = ({
  children,
  variant = 'primary',
  size = 'medium',
  disabled = false,
  loading = false,
  onClick,
  type = 'button',
  dataTestId,
  className = ''
}) => {
  return (
    <button
      type={type}
      className={`btn btn-${variant} btn-${size} ${className}`}
      disabled={disabled || loading}
      onClick={onClick}
      data-test-id={dataTestId}
    >
      {loading ? <span className="btn-spinner">⏳</span> : children}
    </button>
  );
};

/**
 * Table component
 */
export const Table = ({ columns, data, emptyMessage = 'No data available', dataTestId }) => {
  if (!data || data.length === 0) {
    return (
      <div className="table-empty">
        <EmptyState message={emptyMessage} />
      </div>
    );
  }

  return (
    <div className="table-container">
      <table className="table" data-test-id={dataTestId}>
        <thead>
          <tr>
            {columns.map((col, idx) => (
              <th key={idx} className={col.className}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, rowIdx) => (
            <tr key={rowIdx} data-test-id={row.testId}>
              {columns.map((col, colIdx) => (
                <td key={colIdx} className={col.className} data-test-id={col.dataTestId}>
                  {col.render ? col.render(row) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

/**
 * Navigation component
 */
export const Navigation = ({ links }) => {
  return (
    <nav className="navigation">
      {links.map((link, idx) => (
        <Link
          key={idx}
          to={link.to}
          className="nav-link"
          data-test-id={link.dataTestId}
        >
          {link.icon && <span className="nav-icon">{link.icon}</span>}
          <span className="nav-label">{link.label}</span>
        </Link>
      ))}
    </nav>
  );
};

/**
 * Modal component
 */
export const Modal = ({ isOpen, onClose, title, children, footer, size = 'medium' }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div 
        className={`modal-dialog modal-${size}`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button className="modal-close" onClick={onClose} type="button">
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
};

/**
 * Input field with label
 */
export const InputField = ({
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  required = false,
  disabled = false,
  error,
  dataTestId,
  ...props
}) => {
  return (
    <div className="input-field">
      {label && (
        <label className="input-label">
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        className={`input ${error ? 'input-error' : ''}`}
        data-test-id={dataTestId}
        {...props}
      />
      {error && <span className="input-error-message">{error}</span>}
    </div>
  );
};

/**
 * Select dropdown field
 */
export const SelectField = ({
  label,
  value,
  onChange,
  options,
  required = false,
  disabled = false,
  error,
  dataTestId,
  placeholder = 'Select an option'
}) => {
  return (
    <div className="input-field">
      {label && (
        <label className="input-label">
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <select
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        className={`input select ${error ? 'input-error' : ''}`}
        data-test-id={dataTestId}
      >
        <option value="">{placeholder}</option>
        {options.map((opt, idx) => (
          <option key={idx} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error && <span className="input-error-message">{error}</span>}
    </div>
  );
};

/**
 * Stats card component
 */
export const StatsCard = ({ icon, label, value, trend, dataTestId }) => {
  return (
    <div className="stats-card" data-test-id={dataTestId}>
      <div className="stats-icon">{icon}</div>
      <div className="stats-content">
        <div className="stats-label">{label}</div>
        <div className="stats-value">{value}</div>
        {trend && <div className="stats-trend">{trend}</div>}
      </div>
    </div>
  );
};
