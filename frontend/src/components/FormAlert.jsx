import { CheckCircle2, AlertCircle, Info } from 'lucide-react';

/**
 * FormAlert Component
 * Displays informational, success, or error notification banners for forms.
 *
 * @param {Object} props
 * @param {'info'|'success'|'error'} [props.type='info'] - Alert variant type
 * @param {string|React.ReactNode} props.message - Message to display
 * @param {string} [props.className=''] - Additional CSS classes
 */
function FormAlert({ type = 'info', message, className = '' }) {
  if (!message) {
    return null;
  }

  const alertTypeClass =
    type === 'success' ? 'alert-success' : type === 'error' ? 'alert-error' : 'alert-info';

  const icon =
    type === 'success' ? (
      <CheckCircle2 size={16} className="inline-icon" />
    ) : type === 'error' ? (
      <AlertCircle size={16} className="inline-icon" />
    ) : (
      <Info size={16} className="inline-icon" />
    );

  return (
    <div className={`alert ${alertTypeClass} ${className}`.trim()} role="alert" style={{ display: 'flex', alignItems: 'center' }}>
      <span style={{ display: 'inline-flex', alignItems: 'center', marginRight: '0.5rem', flexShrink: 0 }}>
        {icon}
      </span>
      <span>{message}</span>
    </div>
  );
}

export default FormAlert;
