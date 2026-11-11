import { LoaderCircle } from 'lucide-react';

/**
 * Button Component
 * Reusable button with support for variants (primary/secondary),
 * loading state, disabled state, and accessible button types.
 *
 * @param {Object} props
 * @param {string|React.ReactNode} [props.label] - Button text label
 * @param {React.ReactNode} [props.children] - Child elements (overrides label)
 * @param {string} [props.type='button'] - Button type: 'button' | 'submit' | 'reset'
 * @param {boolean} [props.disabled=false] - Whether the button is disabled
 * @param {boolean} [props.loading=false] - Whether the button is in a loading/submitting state
 * @param {string} [props.variant='primary'] - Button style variant: 'primary' | 'secondary'
 * @param {string} [props.className=''] - Additional CSS classes
 * @param {function} [props.onClick] - Click event handler
 */
function Button({
  label,
  children,
  type = 'button',
  disabled = false,
  loading = false,
  variant = 'primary',
  className = '',
  onClick,
  ...rest
}) {
  const isActuallyDisabled = disabled || loading;
  const variantClass = variant === 'secondary' ? 'btn-secondary' : 'btn-primary';
  const disabledClass = isActuallyDisabled ? 'btn-disabled' : '';

  return (
    <button
      type={type}
      disabled={isActuallyDisabled}
      onClick={onClick}
      className={`btn ${variantClass} ${disabledClass} ${className}`.trim()}
      {...rest}
    >
      {loading ? (
        <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
          <LoaderCircle size={15} className="spin" style={{ marginRight: '0.4rem' }} />
          <span>{label || children || 'Loading...'}</span>
        </span>
      ) : (
        children || label
      )}
    </button>
  );
}

export default Button;
