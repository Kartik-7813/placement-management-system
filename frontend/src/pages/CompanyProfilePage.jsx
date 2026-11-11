import { useState, useEffect } from 'react';
import {
  Building2,
  Globe,
  Mail,
  Phone,
  User,
  MapPin,
  Pencil,
  KeyRound,
  ShieldCheck,
  Hash,
  Activity,
  Info,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import { fetchMyCompanyProfile, updateCompanyProfile } from '../services/companyService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';
import FormInput from '../components/FormInput';
import FormAlert from '../components/FormAlert';

/**
 * CompanyProfilePage Component
 * Enables recruiters to view and manage their organization's profile,
 * contact details, headquarters address, and institutional presence.
 */
function CompanyProfilePage() {
  const { user } = useAuth();

  const [company, setCompany] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Edit Mode state
  const [isEditing, setIsEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [successAlert, setSuccessAlert] = useState(null);
  const [errorAlert, setErrorAlert] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    website: '',
    contactPerson: '',
    email: '',
    phone: '',
    address: '',
    status: 'ACTIVE',
  });

  const loadProfile = async () => {
    setLoading(true);
    setError(null);
    setSuccessAlert(null);
    setErrorAlert(null);
    try {
      const data = await fetchMyCompanyProfile(user?.email);
      setCompany(data);
      if (data) {
        setFormData({
          name: data.name || '',
          website: data.website || '',
          contactPerson: data.contactPerson || '',
          email: data.email || '',
          phone: data.phone || '',
          address: data.address || '',
          status: data.status || 'ACTIVE',
        });
      }
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to load organization profile.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [user?.email]);

  const validateForm = () => {
    const errors = {};
    if (!formData.name?.trim()) {
      errors.name = 'Company name is required.';
    } else if (formData.name.trim().length > 150) {
      errors.name = 'Company name cannot exceed 150 characters.';
    }

    if (formData.website && formData.website.length > 200) {
      errors.website = 'Website URL cannot exceed 200 characters.';
    }

    if (formData.contactPerson && formData.contactPerson.length > 100) {
      errors.contactPerson = 'Contact person cannot exceed 100 characters.';
    }

    if (formData.email) {
      if (formData.email.length > 100) {
        errors.email = 'Email cannot exceed 100 characters.';
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
        errors.email = 'Please provide a valid email address.';
      }
    }

    if (formData.phone && formData.phone.length > 15) {
      errors.phone = 'Phone number cannot exceed 15 characters.';
    }

    if (formData.address && formData.address.length > 255) {
      errors.address = 'Address cannot exceed 255 characters.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorAlert(null);
    setSuccessAlert(null);

    if (!validateForm() || !company) {
      return;
    }

    setSubmitting(true);
    try {
      const updated = await updateCompanyProfile(company.companyId, formData);
      setCompany(updated);
      setIsEditing(false);
      setSuccessAlert('Organization profile updated successfully!');
    } catch (err) {
      setErrorAlert(
        err.response?.data?.message ||
          err.message ||
          'Failed to update profile. Ensure you have authorized ownership for this organization.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    setIsEditing(false);
    setFormErrors({});
    setErrorAlert(null);
    if (company) {
      setFormData({
        name: company.name || '',
        website: company.website || '',
        contactPerson: company.contactPerson || '',
        email: company.email || '',
        phone: company.phone || '',
        address: company.address || '',
        status: company.status || 'ACTIVE',
      });
    }
  };

  if (loading) {
    return <LoadingState message="Loading recruiting company profile..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadProfile} />;
  }

  if (!company) {
    return (
      <EmptyState
        title="Organization Profile Not Found"
        message={`No company profile is currently linked with account ${user?.email}. Please contact the placement administrator to register your company profile.`}
        icon={<Building2 size={40} />}
        action={
          <Button
            type="button"
            variant="secondary"
            onClick={loadProfile}
            label="Retry Resolution"
          />
        }
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Company Profile"
        subtitle={`Organization ID #${company.companyId} • Manage your corporate recruitment presence`}
        actions={
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            {!isEditing ? (
              <Button
                type="button"
                variant="primary"
                onClick={() => {
                  setIsEditing(true);
                  setSuccessAlert(null);
                  setErrorAlert(null);
                }}
                label={
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Pencil size={14} /> Edit Profile
                  </span>
                }
              />
            ) : (
              <Button
                type="button"
                variant="secondary"
                onClick={handleCancel}
                disabled={submitting}
                label="Cancel Editing"
              />
            )}
          </div>
        }
      />

      {successAlert && <FormAlert type="success" message={successAlert} />}
      {errorAlert && <FormAlert type="error" message={errorAlert} />}

      {/* Main Content Layout */}
      <div className="job-details-grid">
        {/* Left Column: Organization Details / Form */}
        <div>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 className="card-title" style={{ margin: 0 }}>
                {isEditing ? 'Edit Organization Details' : 'Company Overview'}
              </h2>
              <span
                className={`badge ${company.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}
                style={{ fontSize: '0.75rem' }}
              >
                {company.status || 'ACTIVE'}
              </span>
            </div>

            {isEditing ? (
              <form onSubmit={handleSubmit} noValidate>
                <FormInput
                  label="Company Name"
                  name="name"
                  type="text"
                  value={formData.name}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, name: e.target.value }));
                    if (formErrors.name) setFormErrors((prev) => ({ ...prev, name: null }));
                  }}
                  required
                  error={formErrors.name}
                  maxLength={150}
                />

                <FormInput
                  label="Official Website URL"
                  name="website"
                  type="url"
                  placeholder="https://example.com"
                  value={formData.website}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, website: e.target.value }));
                    if (formErrors.website) setFormErrors((prev) => ({ ...prev, website: null }));
                  }}
                  error={formErrors.website}
                  maxLength={200}
                />

                <div className="form-grid-2">
                  <FormInput
                    label="Official Recruitment Email"
                    name="email"
                    type="email"
                    placeholder="hr@company.com"
                    value={formData.email}
                    onChange={(e) => {
                      setFormData((prev) => ({ ...prev, email: e.target.value }));
                      if (formErrors.email) setFormErrors((prev) => ({ ...prev, email: null }));
                    }}
                    error={formErrors.email}
                    maxLength={100}
                  />

                  <FormInput
                    label="Contact Phone Number"
                    name="phone"
                    type="tel"
                    placeholder="+91 9876543210"
                    value={formData.phone}
                    onChange={(e) => {
                      setFormData((prev) => ({ ...prev, phone: e.target.value }));
                      if (formErrors.phone) setFormErrors((prev) => ({ ...prev, phone: null }));
                    }}
                    error={formErrors.phone}
                    maxLength={15}
                  />
                </div>

                <FormInput
                  label="Primary Contact Person / HR Coordinator"
                  name="contactPerson"
                  type="text"
                  placeholder="e.g. Talent Acquisition Lead"
                  value={formData.contactPerson}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, contactPerson: e.target.value }));
                    if (formErrors.contactPerson) setFormErrors((prev) => ({ ...prev, contactPerson: null }));
                  }}
                  error={formErrors.contactPerson}
                  maxLength={100}
                />

                <div className="form-group">
                  <label className="form-label" htmlFor="companyAddress">
                    Headquarters / Office Address
                  </label>
                  <textarea
                    id="companyAddress"
                    rows={3}
                    className={`form-input ${formErrors.address ? 'input-error' : ''}`}
                    placeholder="Enter corporate physical address..."
                    value={formData.address}
                    onChange={(e) => {
                      setFormData((prev) => ({ ...prev, address: e.target.value }));
                      if (formErrors.address) setFormErrors((prev) => ({ ...prev, address: null }));
                    }}
                    maxLength={255}
                    style={{ resize: 'vertical' }}
                  />
                  {formErrors.address && (
                    <div className="form-error" style={{ marginTop: '0.25rem' }}>
                      {formErrors.address}
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={handleCancel}
                    disabled={submitting}
                    label="Cancel"
                  />
                  <Button
                    type="submit"
                    variant="primary"
                    loading={submitting}
                    disabled={submitting}
                    label={submitting ? 'Saving Changes...' : 'Save Profile'}
                  />
                </div>
              </form>
            ) : (
              <div className="job-spec-list">
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Building2 size={15} /> Company Name:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    <strong>{company.name}</strong>
                  </span>
                </div>

                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Globe size={15} /> Website:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {company.website ? (
                      <a
                        href={company.website.startsWith('http') ? company.website : `https://${company.website}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ color: 'var(--primary-color)', textDecoration: 'none', fontWeight: 500 }}
                      >
                        {company.website} &nearr;
                      </a>
                    ) : (
                      <span style={{ color: 'var(--text-muted)' }}>Not configured</span>
                    )}
                  </span>
                </div>

                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Mail size={15} /> Official Email:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {company.email || <span style={{ color: 'var(--text-muted)' }}>Not configured</span>}
                  </span>
                </div>

                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Phone size={15} /> Phone:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {company.phone || <span style={{ color: 'var(--text-muted)' }}>Not configured</span>}
                  </span>
                </div>

                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <User size={15} /> Contact Person:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {company.contactPerson || <span style={{ color: 'var(--text-muted)' }}>Not configured</span>}
                  </span>
                </div>

                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <MapPin size={15} /> Office Address:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {company.address || <span style={{ color: 'var(--text-muted)' }}>Not configured</span>}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Account Status & Guidelines */}
        <div>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h3 className="card-title">Recruiter Account Specifications</h3>
            <div className="job-spec-list">
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <KeyRound size={15} /> Authenticated User:
                  </span>
                </span>
                <span className="job-spec-row-value"><code>{user?.email}</code></span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <ShieldCheck size={15} /> Role Authority:
                  </span>
                </span>
                <span className="job-spec-row-value"><span className="badge badge-warning">ROLE_COMPANY</span></span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Hash size={15} /> Linked Company ID:
                  </span>
                </span>
                <span className="job-spec-row-value"><code>#{company.companyId}</code></span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Activity size={15} /> Status:
                  </span>
                </span>
                <span className="job-spec-row-value">
                  <span className={`badge ${company.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                    {company.status}
                  </span>
                </span>
              </div>
            </div>

            <div style={{ marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
              <Info size={16} style={{ flexShrink: 0, marginTop: '0.125rem', color: 'var(--primary-color)' }} />
              <div>
                <strong>Profile Guidelines:</strong> Keep your company name, website, and recruiter contact information updated. Student applicants view these details when browsing campus recruitment drives.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CompanyProfilePage;
