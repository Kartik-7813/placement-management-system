import { LoaderCircle } from 'lucide-react';

/**
 * LoadingState Component
 * Displays a clean, professional loading state indicator.
 *
 * @param {Object} props
 * @param {string} [props.message='Loading data...'] - Explanatory loading message
 */
function LoadingState({ message = 'Loading dashboard metrics...' }) {
  return (
    <div className="state-container">
      <div className="state-icon">
        <LoaderCircle size={36} className="spin" color="var(--primary-color)" strokeWidth={2} />
      </div>
      <h3 className="state-title">Please Wait</h3>
      <p className="state-description">{message}</p>
    </div>
  );
}

export default LoadingState;
