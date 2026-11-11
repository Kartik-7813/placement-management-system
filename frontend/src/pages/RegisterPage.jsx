import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import FormInput from '../components/FormInput';
import Button from '../components/Button';
import FormAlert from '../components/FormAlert';
import useAuth from '../hooks/useAuth';

/**
 * RegisterPage Component
 * Provides student registration interface matching backend RegisterRequest DTO.
 * Features controlled state, two-column responsive layout, client-side validation,
 * and live integration with POST /api/auth/register.
 */
function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();

  // Controlled form state matching RegisterRequest DTO
  const [formData, setFormData] = useState({
    name: '',
    rollNumber: '',
    email: '',
    password: '',
    phone: '',
    dob: '',
    gender: '',
    department: '',
    cgpa: '',
    passingYear: '',
    address: '',
  });

  // Validation error state
  const [errors, setErrors] = useState({});

  // Password visibility toggle state
  const [showPassword, setShowPassword] = useState(false);

  // Status banner for form actions
  const [alert, setAlert] = useState(null);

  // Submitting / loading state
  const [submitting, setSubmitting] = useState(false);

  /**
   * Generic change handler for controlled form inputs
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    // Clear error as user corrects the field
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  /**
   * Comprehensive client-side form validation
   * @returns {boolean} true if all fields are valid, false otherwise
   */
  const validateForm = () => {
    const newErrors = {};

    // 1. Full Name (Required)
    if (!formData.name.trim()) {
      newErrors.name = 'Full name is required.';
    } else if (formData.name.trim().length > 100) {
      newErrors.name = 'Full name cannot exceed 100 characters.';
    }

    // 2. Roll Number (Required)
    if (!formData.rollNumber.trim()) {
      newErrors.rollNumber = 'Roll number / Student ID is required.';
    } else if (formData.rollNumber.trim().length > 50) {
      newErrors.rollNumber = 'Roll number cannot exceed 50 characters.';
    }

    // 3. Email (Required & Valid Format)
    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required.';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.email.trim())) {
        newErrors.email = 'Please enter a valid email address (e.g. student@college.edu).';
      } else if (formData.email.trim().length > 100) {
        newErrors.email = 'Email cannot exceed 100 characters.';
      }
    }

    // 4. Password (Required, Min 6 chars)
    if (!formData.password) {
      newErrors.password = 'Password is required.';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters long.';
    } else if (formData.password.length > 50) {
      newErrors.password = 'Password cannot exceed 50 characters.';
    }

    // 5. Phone (Optional, max 15 digits)
    if (formData.phone && formData.phone.trim().length > 15) {
      newErrors.phone = 'Phone number cannot exceed 15 digits.';
    }

    // 6. CGPA (Optional, 0.00 to 10.00)
    if (formData.cgpa !== '' && formData.cgpa !== null && formData.cgpa !== undefined) {
      const numericCgpa = parseFloat(formData.cgpa);
      if (isNaN(numericCgpa) || numericCgpa < 0 || numericCgpa > 10) {
        newErrors.cgpa = 'CGPA must be a decimal between 0.00 and 10.00.';
      }
    }

    // 7. Passing Year (Optional, 1900 to 2100)
    if (formData.passingYear !== '' && formData.passingYear !== null && formData.passingYear !== undefined) {
      const year = parseInt(formData.passingYear, 10);
      if (isNaN(year) || year < 1900 || year > 2100) {
        newErrors.passingYear = 'Passing year must be between 1900 and 2100.';
      }
    }

    // 8. Address (Optional, max 255 chars)
    if (formData.address && formData.address.length > 255) {
      newErrors.address = 'Address cannot exceed 255 characters.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Form submission handler calling POST /api/auth/register
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setAlert(null);

    const isValid = validateForm();
    if (!isValid) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    setSubmitting(true);

    // Prepare clean payload conforming strictly to backend RegisterRequest DTO
    const payload = {
      email: formData.email.trim(),
      password: formData.password,
      rollNumber: formData.rollNumber.trim(),
      name: formData.name.trim(),
      phone: formData.phone.trim() || null,
      dob: formData.dob || null,
      gender: formData.gender || null,
      department: formData.department.trim() || null,
      cgpa: formData.cgpa !== '' && formData.cgpa !== null ? parseFloat(formData.cgpa) : null,
      passingYear: formData.passingYear !== '' && formData.passingYear !== null ? parseInt(formData.passingYear, 10) : null,
      address: formData.address.trim() || null,
    };

    try {
      await register(payload);
      // Navigate to login with success banner
      navigate('/login', {
        state: {
          successMessage: 'Student account registered successfully! You may now sign in with your credentials.',
        },
      });
    } catch (err) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      if (err.response) {
        const { status, data } = err.response;
        if (status === 409) {
          setAlert({
            type: 'error',
            message: data?.message || 'A student with this email or roll number is already registered.',
          });
        } else if (status === 400) {
          if (data?.validationErrors) {
            setErrors((prev) => ({ ...prev, ...data.validationErrors }));
            setAlert({
              type: 'error',
              message: 'Please correct the highlighted validation errors below.',
            });
          } else {
            setAlert({
              type: 'error',
              message: data?.message || 'Validation failed. Please verify all fields.',
            });
          }
        } else if (status === 429) {
          setAlert({
            type: 'error',
            message: 'Too many registration attempts. Please wait a moment before trying again.',
          });
        } else {
          setAlert({
            type: 'error',
            message: data?.message || `Registration failed (Status ${status}). Please try again.`,
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
          message: err.message || 'An unexpected error occurred during registration.',
        });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card auth-card-wide">
        <div className="auth-header">
          <h1 className="auth-title">Student Registration</h1>
          <p className="auth-subtitle">
            Create your student placement profile to apply for campus recruitment drives
          </p>
        </div>

        {alert && <FormAlert type={alert.type} message={alert.message} />}

        <form onSubmit={handleSubmit} noValidate>
          {/* Section: Account & Authentication */}
          <h2 className="form-section-title">Account Credentials</h2>

          <div className="form-grid-2">
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
              helperText="Official university or personal email"
            />

            <FormInput
              label="Password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
              onChange={handleChange}
              placeholder="Minimum 6 characters"
              required
              error={errors.password}
              autoComplete="new-password"
              disabled={submitting}
              helperText="Must be 6–50 characters"
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
          </div>

          {/* Section: Student Profile Information */}
          <h2 className="form-section-title">Academic & Profile Details</h2>

          <div className="form-grid-2">
            <FormInput
              label="Full Name"
              name="name"
              type="text"
              value={formData.name}
              onChange={handleChange}
              placeholder="e.g. John Doe"
              required
              error={errors.name}
              autoComplete="name"
              disabled={submitting}
            />

            <FormInput
              label="Roll Number / Student ID"
              name="rollNumber"
              type="text"
              value={formData.rollNumber}
              onChange={handleChange}
              placeholder="e.g. CS2022001"
              required
              error={errors.rollNumber}
              disabled={submitting}
              helperText="Must be unique across all students"
            />

            <FormInput
              label="Department / Branch"
              name="department"
              type="text"
              value={formData.department}
              onChange={handleChange}
              placeholder="e.g. Computer Science"
              error={errors.department}
              disabled={submitting}
            />

            <FormInput
              label="Cumulative GPA (CGPA)"
              name="cgpa"
              type="number"
              step="0.01"
              min="0"
              max="10"
              value={formData.cgpa}
              onChange={handleChange}
              placeholder="e.g. 8.75"
              error={errors.cgpa}
              disabled={submitting}
              helperText="Scale of 0.00 to 10.00"
            />

            <FormInput
              label="Passing Year"
              name="passingYear"
              type="number"
              min="1900"
              max="2100"
              value={formData.passingYear}
              onChange={handleChange}
              placeholder="e.g. 2026"
              error={errors.passingYear}
              disabled={submitting}
            />

            <FormInput
              label="Phone Number"
              name="phone"
              type="tel"
              value={formData.phone}
              onChange={handleChange}
              placeholder="e.g. +91 9876543210"
              error={errors.phone}
              autoComplete="tel"
              disabled={submitting}
            />

            <FormInput
              label="Date of Birth"
              name="dob"
              type="date"
              value={formData.dob}
              onChange={handleChange}
              error={errors.dob}
              disabled={submitting}
            />

            <div className="form-group">
              <label htmlFor="input-gender" className="form-label">
                Gender
              </label>
              <select
                id="input-gender"
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                className="form-select"
                disabled={submitting}
              >
                <option value="">Select Gender (Optional)</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>

          <FormInput
            label="Residential Address"
            name="address"
            type="text"
            value={formData.address}
            onChange={handleChange}
            placeholder="e.g. 123 University Campus Road, City"
            error={errors.address}
            autoComplete="street-address"
            disabled={submitting}
          />

          <Button
            type="submit"
            variant="primary"
            className="btn-block"
            loading={submitting}
            disabled={submitting}
            style={{ marginTop: '1rem', padding: '0.75rem' }}
          >
            Register Student Account
          </Button>
        </form>

        <div className="auth-footer">
          Already registered?{' '}
          <Link to="/login" style={{ fontWeight: 600 }}>
            Sign in to your account
          </Link>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
