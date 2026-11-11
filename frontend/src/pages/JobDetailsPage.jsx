import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Search, IndianRupee, GraduationCap, Calendar, Briefcase, Building2, X } from 'lucide-react';
import useAuth from '../hooks/useAuth';
import api from '../services/api';
import { API_ENDPOINTS, STORAGE_KEYS } from '../utils/constants';
import {
  resolveCurrentStudentId,
  fetchCurrentStudentProfile,
  submitApplication,
} from '../services/studentService';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import Button from '../components/Button';
import FormAlert from '../components/FormAlert';
import FormInput from '../components/FormInput';

/**
 * JobDetailsPage Component
 * Displays complete placement opportunity specifications, eligibility criteria,
 * skill requirements, compensation details, and active application submission workflow.
 */
function JobDetailsPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);

  // Application workflow state
  const [existingApplication, setExistingApplication] = useState(null);
  const [checkingApp, setCheckingApp] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [studentProfile, setStudentProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);

  // Form state
  const [resumeUrl, setResumeUrl] = useState('');
  const [remarks, setRemarks] = useState('');
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submitSuccess, setSubmitSuccess] = useState(null);

  const fetchJobDetails = async () => {
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const response = await api.get(API_ENDPOINTS.JOBS.BY_ID(jobId));
      setJob(response.data);
    } catch (err) {
      if (err.response && err.response.status === 404) {
        setNotFound(true);
      } else {
        setError(
          err.response?.data?.message || err.message || 'Failed to load job opportunity details.'
        );
      }
    } finally {
      setLoading(false);
    }
  };

  // Check if student has already applied to this specific job
  const checkExistingApplication = async () => {
    if (user?.role !== 'ROLE_STUDENT') {
      setCheckingApp(false);
      return;
    }
    setCheckingApp(true);
    try {
      const response = await api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?jobId=${jobId}`);
      const apps = response.data?.content || [];
      if (apps.length > 0) {
        setExistingApplication(apps[0]);
        if (apps[0].studentId) {
          sessionStorage.setItem(STORAGE_KEYS.STUDENT_ID, String(apps[0].studentId));
        }
      } else {
        setExistingApplication(null);
      }
    } catch (err) {
      console.warn('Could not check existing application status:', err);
    } finally {
      setCheckingApp(false);
    }
  };

  useEffect(() => {
    if (jobId) {
      fetchJobDetails();
      checkExistingApplication();
    }
  }, [jobId]);

  const handleOpenApplyModal = async () => {
    setIsModalOpen(true);
    setSubmitError(null);
    setSubmitSuccess(null);
    setFormErrors({});

    if (!studentProfile) {
      setLoadingProfile(true);
      try {
        const profile = await fetchCurrentStudentProfile();
        setStudentProfile(profile);
      } catch (err) {
        console.warn('Could not load student profile for CGPA preview:', err);
      } finally {
        setLoadingProfile(false);
      }
    }
  };

  const handleCloseApplyModal = () => {
    if (!submitting) {
      setIsModalOpen(false);
      setSubmitError(null);
      setFormErrors({});
    }
  };

  const validateForm = () => {
    const errors = {};
    const trimmedUrl = resumeUrl.trim();

    if (!trimmedUrl) {
      errors.resumeUrl = 'Resume URL is required';
    } else if (!/^https?:\/\/.+/i.test(trimmedUrl)) {
      errors.resumeUrl = 'Resume URL must start with http:// or https://';
    } else if (trimmedUrl.length > 255) {
      errors.resumeUrl = 'Resume URL must not exceed 255 characters';
    }

    if (remarks && remarks.length > 1000) {
      errors.remarks = 'Remarks must not exceed 1000 characters';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmitApplication = async (e) => {
    e.preventDefault();
    setSubmitError(null);
    setSubmitSuccess(null);

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      // Resolve student identifier
      let studentId = studentProfile?.studentId;
      if (!studentId) {
        studentId = await resolveCurrentStudentId();
      }

      if (!studentId) {
        throw new Error('Unable to resolve authenticated student account. Please re-login.');
      }

      const response = await submitApplication({
        studentId,
        jobId: job.jobId,
        resumeUrl,
        remarks,
      });

      setSubmitSuccess('Application submitted successfully! Redirecting...');
      setExistingApplication(response);

      setTimeout(() => {
        setIsModalOpen(false);
        navigate(`/student/applications/${response.applicationId}`);
      }, 1200);
    } catch (err) {
      const serverMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to submit application. Please verify drive eligibility and try again.';
      setSubmitError(serverMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const formatPackage = (amount) => {
    if (amount == null) return 'Negotiable';
    const num = Number(amount);
    if (isNaN(num)) return 'Negotiable';
    if (num >= 100000) {
      return `₹ ${(num / 100000).toFixed(2)} LPA`;
    }
    return `₹ ${num.toLocaleString()}`;
  };

  const formatSkills = (skillsString) => {
    if (!skillsString) return [];
    return skillsString
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPLIED':
        return <span className="badge-status badge-applied">APPLIED</span>;
      case 'SHORTLISTED':
        return <span className="badge-status badge-shortlisted">SHORTLISTED</span>;
      case 'INTERVIEWING':
        return <span className="badge-status badge-interviewing">INTERVIEWING</span>;
      case 'SELECTED':
        return <span className="badge-status badge-selected">SELECTED</span>;
      case 'REJECTED':
        return <span className="badge-status badge-rejected">REJECTED</span>;
      case 'WITHDRAWN':
        return <span className="badge-status badge-withdrawn">WITHDRAWN</span>;
      default:
        return <span className="badge badge-info">{status}</span>;
    }
  };

  if (loading) {
    return <LoadingState message="Loading job opening specifications..." />;
  }

  if (notFound) {
    return (
      <EmptyState
        title="Job Opening Not Found"
        message={`The campus placement drive with ID #${jobId} does not exist or may have been unlisted.`}
        icon={<Search size={40} strokeWidth={1.5} />}
        action={
          <Link
            to="/student/jobs"
            className="btn btn-primary"
            style={{ marginTop: '0.75rem' }}
          >
            &larr; Back to Job Opportunities
          </Link>
        }
      />
    );
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchJobDetails} />;
  }

  if (!job) {
    return null;
  }

  const skills = formatSkills(job.requiredSkills);
  const isDeadlinePassed =
    job.deadline && new Date(job.deadline) < new Date().setHours(0, 0, 0, 0);
  const isClosed = job.status === 'CLOSED';

  // Academic eligibility check preview
  const isCgpaIneligible =
    studentProfile &&
    job.minCgpa != null &&
    studentProfile.cgpa != null &&
    Number(studentProfile.cgpa) < Number(job.minCgpa);

  return (
    <div>
      {/* Back Link */}
      <div style={{ marginBottom: '1.25rem' }}>
        <Link
          to="/student/jobs"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            fontSize: '0.875rem',
            fontWeight: 500,
            color: 'var(--text-muted)',
          }}
        >
          &larr; Back to Job Listings
        </Link>
      </div>

      {/* Hero Overview Card */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <div
              style={{
                fontSize: '0.875rem',
                fontWeight: 600,
                color: 'var(--primary-color)',
                textTransform: 'uppercase',
                letterSpacing: '0.025em',
                marginBottom: '0.35rem',
              }}
            >
              {job.companyName}
            </div>
            <h1
              style={{
                fontSize: '1.75rem',
                fontWeight: 700,
                color: 'var(--secondary-color)',
                margin: '0 0 0.5rem',
              }}
            >
              {job.title}
            </h1>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <span
                className={`badge ${
                  job.status === 'ACTIVE'
                    ? 'badge-success'
                    : job.status === 'CLOSED'
                    ? 'badge-warning'
                    : 'badge-info'
                }`}
              >
                {job.status}
              </span>
              {job.jobType && (
                <span className="badge badge-info">{job.jobType}</span>
              )}
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Job ID: <code>#{job.jobId}</code>
              </span>
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Offered CTC Package
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--secondary-color)' }}>
              {formatPackage(job.packageAmount)}
            </div>
          </div>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="job-details-grid">
        {/* Left Column: Job Description & Detailed Specs */}
        <div>
          {/* About the Role */}
          <div className="job-details-card">
            <h2 className="job-details-section-title">About the Role & Job Description</h2>
            <div className="job-details-text">
              {job.description || 'No detailed job description was provided by the recruiter.'}
            </div>
          </div>

          {/* Eligibility Criteria */}
          <div className="job-details-card">
            <h2 className="job-details-section-title">Eligibility Criteria</h2>
            <div className="job-details-text">
              {job.eligibilityCriteria || 'No additional departmental or academic restrictions specified.'}
            </div>
          </div>

          {/* Required Skills */}
          <div className="job-details-card">
            <h2 className="job-details-section-title">Required Skills & Technologies</h2>
            {skills.length > 0 ? (
              <div className="job-skills-container">
                {skills.map((skill, index) => (
                  <span
                    key={index}
                    className="skill-pill"
                    style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                  >
                    {skill}
                  </span>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                Open to general skill competencies.
              </p>
            )}
          </div>
        </div>

        {/* Right Column: Highlights, Prerequisite Checks & Application CTA */}
        <div>
          {/* Key Job Specifications */}
          <div className="job-details-card">
            <h2 className="job-details-section-title">Drive Overview</h2>
            <div className="job-spec-list">
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <IndianRupee size={15} className="inline-icon" />
                  CTC Package:
                </span>
                <span className="job-spec-row-value">{formatPackage(job.packageAmount)}</span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <GraduationCap size={15} className="inline-icon" />
                  Minimum CGPA:
                </span>
                <span className="job-spec-row-value">
                  {job.minCgpa != null ? `${job.minCgpa} / 10.00` : 'No CGPA Cutoff'}
                </span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <Calendar size={15} className="inline-icon" />
                  Deadline:
                </span>
                <span
                  className="job-spec-row-value"
                  style={{ color: isDeadlinePassed ? 'var(--danger-color)' : 'inherit' }}
                >
                  {job.deadline ? job.deadline : 'Open'}
                </span>
              </div>
              {job.jobType && (
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Briefcase size={15} className="inline-icon" />
                    Employment:
                  </span>
                  <span className="job-spec-row-value">{job.jobType}</span>
                </div>
              )}
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <Building2 size={15} className="inline-icon" />
                  Recruiter ID:
                </span>
                <span className="job-spec-row-value">#{job.companyId}</span>
              </div>
            </div>
          </div>

          {/* Application Status / Action Card */}
          <div className="job-details-card">
            <h2 className="job-details-section-title">Application Status</h2>

            {checkingApp ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: 'var(--text-muted)' }}>
                Checking application records...
              </div>
            ) : existingApplication ? (
              <div>
                <div className="alert alert-info" style={{ marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                    <strong>Application Submitted</strong>
                    {getStatusBadge(existingApplication.status)}
                  </div>
                  <div style={{ fontSize: '0.8125rem' }}>
                    You applied for this role on{' '}
                    {existingApplication.appliedOn
                      ? new Date(existingApplication.appliedOn).toLocaleDateString()
                      : 'record'}
                    .
                  </div>
                </div>
                <Link
                  to={`/student/applications/${existingApplication.applicationId}`}
                  className="btn btn-primary btn-block"
                  style={{ textAlign: 'center', textDecoration: 'none' }}
                >
                  View Application Status &rarr;
                </Link>
              </div>
            ) : isClosed ? (
              <div className="alert alert-error" style={{ marginBottom: 0 }}>
                <strong>Recruitment Closed:</strong> This drive is no longer accepting new candidate applications.
              </div>
            ) : isDeadlinePassed ? (
              <div className="alert alert-error" style={{ marginBottom: 0 }}>
                <strong>Deadline Passed:</strong> The registration deadline for this job was {job.deadline}.
              </div>
            ) : (
              <div>
                <Button
                  type="button"
                  variant="primary"
                  className="btn-block"
                  onClick={handleOpenApplyModal}
                  label="Apply for this Job"
                />
                <div className="cta-alert-card" style={{ marginTop: '0.75rem', padding: '0.875rem' }}>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: 0, lineHeight: 1.4 }}>
                    Submit your resume URL and remarks to be considered for evaluation rounds.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Application Submission Modal Dialog */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={handleCloseApplyModal}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Apply for Job Opportunity</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  {job.title} &bull; {job.companyName}
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseApplyModal}
                disabled={submitting}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSubmitApplication} noValidate>
              {submitError && <FormAlert type="error" message={submitError} />}
              {submitSuccess && <FormAlert type="success" message={submitSuccess} />}

              {/* Eligibility Preview Banner */}
              {loadingProfile ? (
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                  Verifying student academic eligibility...
                </div>
              ) : isCgpaIneligible ? (
                <div className="alert alert-error" style={{ marginBottom: '1rem', fontSize: '0.8125rem' }}>
                  <strong>CGPA Warning:</strong> Your recorded CGPA is{' '}
                  <strong>{studentProfile.cgpa}</strong>, but this drive requires a minimum of{' '}
                  <strong>{job.minCgpa}</strong>. Submissions below the cutoff will be rejected by backend policy.
                </div>
              ) : (
                <div className="alert alert-info" style={{ marginBottom: '1rem', fontSize: '0.8125rem' }}>
                  Drive cutoff: <strong>{job.minCgpa ? `${job.minCgpa} CGPA` : 'Open'}</strong> &bull; Deadline:{' '}
                  <strong>{job.deadline || 'Open'}</strong>
                </div>
              )}

              {/* Resume URL Field */}
              <FormInput
                label="Resume / Portfolio Document URL"
                name="resumeUrl"
                type="url"
                value={resumeUrl}
                onChange={(e) => {
                  setResumeUrl(e.target.value);
                  if (formErrors.resumeUrl) {
                    setFormErrors((prev) => ({ ...prev, resumeUrl: null }));
                  }
                }}
                placeholder="https://drive.google.com/file/d/... or https://github.com/..."
                required
                error={formErrors.resumeUrl}
                hint="Provide a publicly viewable link to your PDF resume or portfolio."
              />

              {/* Remarks Field */}
              <div className="form-group">
                <label className="form-label" htmlFor="remarks">
                  Candidate Remarks / Cover Note <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(Optional)</span>
                </label>
                <textarea
                  id="remarks"
                  className="form-textarea"
                  rows={4}
                  value={remarks}
                  onChange={(e) => {
                    setRemarks(e.target.value);
                    if (formErrors.remarks) {
                      setFormErrors((prev) => ({ ...prev, remarks: null }));
                    }
                  }}
                  placeholder="Share your key projects, tech stack interests, or a brief introduction for the recruiting team..."
                  maxLength={1000}
                />
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                  {formErrors.remarks ? (
                    <span style={{ color: 'var(--danger-color)' }}>{formErrors.remarks}</span>
                  ) : (
                    <span>Max 1000 characters</span>
                  )}
                  <span>{remarks.length} / 1000</span>
                </div>
              </div>

              {/* Modal Footer Controls */}
              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleCloseApplyModal}
                  disabled={submitting}
                  label="Cancel"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={submitting}
                  disabled={submitting || Boolean(submitSuccess)}
                  label={submitting ? 'Submitting Application...' : 'Confirm & Apply'}
                />
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default JobDetailsPage;
