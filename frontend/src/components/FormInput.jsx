/**
 * FormInput Component
 * Controlled form input field supporting labels, required indicators,
 * validation error states, helper text, and end adornments (e.g. password visibility toggles).
 *
 * @param {Object} props
 * @param {string} props.label - Display label for the field
 * @param {string} props.name - HTML name attribute and state key
 * @param {string} [props.type='text'] - Input type (text, email, password, number, date, tel)
 * @param {string|number} props.value - Controlled input value
 * @param {function} props.onChange - Change handler function
 * @param {string} [props.placeholder=''] - Input placeholder
 * @param {boolean} [props.required=false] - Whether the field is mandatory
 * @param {string} [props.error=''] - Validation error message to display
 * @param {string} [props.helperText=''] - Optional instructional text below input
 * @param {React.ReactNode} [props.endAdornment=null] - Optional element placed inside the right edge of input
 * @param {string} [props.id] - Optional custom element id
 * @param {string} [props.className=''] - Additional CSS classes
 */
function FormInput({
  label,
  name,
  type = 'text',
  value,
  onChange,
  placeholder = '',
  required = false,
  error = '',
  helperText = '',
  endAdornment = null,
  id,
  className = '',
  ...rest
}) {
  const inputId = id || `input-${name}`;
  const hasError = Boolean(error);

  return (
    <div className={`form-group ${className}`.trim()}>
      {label && (
        <label htmlFor={inputId} className="form-label">
          {label}
          {required && <span className="form-required" title="Required">*</span>}
        </label>
      )}

      <div className={endAdornment ? 'input-with-adornment' : ''}>
        <input
          id={inputId}
          name={name}
          type={type}
          value={value ?? ''}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
          className={`form-input ${hasError ? 'has-error' : ''}`}
          aria-invalid={hasError}
          aria-describedby={hasError ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined}
          {...rest}
        />
        {endAdornment}
      </div>

      {hasError && (
        <span id={`${inputId}-error`} className="form-error" role="alert">
          {error}
        </span>
      )}

      {!hasError && helperText && (
        <span id={`${inputId}-helper`} className="form-helper">
          {helperText}
        </span>
      )}
    </div>
  );
}

export default FormInput;
