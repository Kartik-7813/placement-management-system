import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import FormInput from '../components/FormInput';
import Button from '../components/Button';
import FormAlert from '../components/FormAlert';
import useAuth from '../hooks/useAuth';

/**
 * LoginPage Component
 * Provides authentication interface for students, recruiters, and administrators.
 * Calls backend POST /api/auth/login, manages loading states, extracts JWT,
 * and seamlessly redirects upon successful authentication.
 */
function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  // Controlled input form state matching LoginRequest DTO
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });

  // Validation errors state
  const [errors, setErrors] = useState({});

  // Password visibility toggle state
  const [showPassword, setShowPassword] = useState(false);

  // Form submission status banner
  const [alert, setAlert] = useState(null);

  // Submitting / loading state
  const [submitting, setSubmitting] = useState(false);

  // Destination path after successful login
  const from = location.state?.from?.pathname || '/dashboard';

  // Display initial success message if redirected from registration
  useEffect(() => {
    if (location.state?.successMessage) {
      setAlert({
        type: 'success',
        message: location.state.successMessage,
      });
    }
  }, [location.state]);

  // If already authenticated, redirect to destination
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  /**
   * Generic input change handler for controlled inputs
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    // Clear field-level error as user types
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  /**
   * Client-side form validation
   * @returns {boolean} true if valid, false otherwise
   */
  const validateForm = () => {
    const newErrors = {};

    // Email validation
    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required.';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.email.trim())) {
        newErrors.email = 'Please enter a valid email address (e.g. user@example.com).';
      }
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Form submission handler calling POST /api/auth/login
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setAlert(null);

    const isValid = validateForm();
    if (!isValid) {
      return;
    }

    setSubmitting(true);

    try {
      await login(formData.email.trim(), formData.password);
      // On success, redirect to the originally requested URL or dashboard
      navigate(from, { replace: true });
    } catch (err) {
      if (err.response) {
        const { status, data } = err.response;
        if (status === 401) {
          setAlert({
            type: 'error',
            message: 'Invalid email or password. Please verify your credentials and try again.',
          });
        } else if (status === 429) {
          setAlert({
            type: 'error',
            message: 'Too many login attempts. Please wait a moment before trying again.',
          });
        } else {
          setAlert({
            type: 'error',
            message: data?.message || `Authentication failed (Status ${status}). Please try again.`,
          });
        }
      } else if (err.request) {
        setAlert({
          type: 'error',
          message: 'Unable to connect to backend server. Please check your network connection and try again.',
        });
      } else {
        setAlert({
          type: 'error',
          message: err.message || 'An unexpected error occurred. Please try again.',
        });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1 className="auth-title">Sign In</h1>
          <p className="auth-subtitle">
            Access the Placement Management System with your university credentials
          </p>
        </div>

        {alert && <FormAlert type={alert.type} message={alert.message} />}

        <form onSubmit={handleSubmit} noValidate>
          <FormInput
            label="Email Address"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="student@university.edu"
            required
            error={errors.email}
            autoComplete="email"
            disabled={submitting}
          />

          <FormInput
            label="Password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            value={formData.password}
            onChange={handleChange}
            placeholder="Enter your password"
            required
            error={errors.password}
            autoComplete="current-password"
            disabled={submitting}
            endAdornment={
              <button
                type="button"
                className="adornment-btn"
                onClick={() => setShowPassword((prev) => !prev)}
                title={showPassword ? 'Hide password' : 'Show password'}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                disabled={submitting}
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            }
          />

          <Button
            type="submit"
            variant="primary"
            className="btn-block"
            loading={submitting}
            disabled={submitting}
            style={{ marginTop: '0.5rem' }}
          >
            Sign In
          </Button>
        </form>

        <div className="auth-footer">
          Don't have a student account?{' '}
          <Link to="/register" style={{ fontWeight: 600 }}>
            Register as Student
          </Link>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
