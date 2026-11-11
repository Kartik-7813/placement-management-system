import { AlertTriangle } from 'lucide-react';
import Button from './Button';

/**
 * ErrorState Component
 * Displays a clean error message when an API request fails with an optional retry button.
 *
 * @param {Object} props
 * @param {string} [props.title='Unable to Load Data'] - Error title
 * @param {string} props.message - Descriptive error message
 * @param {function} [props.onRetry] - Optional retry callback function
 */
function ErrorState({ title = 'Unable to Load Data', message, onRetry }) {
  return (
    <div className="state-container" style={{ borderColor: 'var(--danger-color)' }}>
      <div className="state-icon">
        <AlertTriangle size={40} strokeWidth={1.5} color="var(--danger-color)" />
      </div>
      <h3 className="state-title" style={{ color: 'var(--danger-color)' }}>{title}</h3>
      <p className="state-description">{message || 'An error occurred while fetching information from the server.'}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} label="Try Again" />
      )}
    </div>
  );
}

export default ErrorState;
