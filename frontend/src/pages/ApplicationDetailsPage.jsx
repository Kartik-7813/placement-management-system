import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Lock,
  Search,
  Check,
  FileText,
  User,
  Calendar,
  MessageSquare,
  Building2,
  Briefcase,
  IndianRupee,
  GraduationCap,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * ApplicationDetailsPage Component
 * Renders complete submission metadata, visual progress stepper,
 * resume document links, and associated interview evaluation rounds.
 * Strictly verifies ownership; 403 Forbidden displays clean access denied message.
 */
function ApplicationDetailsPage() {
  const { applicationId } = useParams();
  const { user } = useAuth();

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isCompany = user?.role === 'ROLE_COMPANY';
  const isPrivileged = isAdmin || isCompany;
  const defaultBackPath = isAdmin
    ? '/admin/interviews'
    : isCompany
    ? '/company/applicants'
    : '/student/applications';
  const defaultBackLabel = isAdmin
    ? 'Back to Interviews'
    : isCompany
    ? 'Back to Applicants'
    : 'Back to My Applications';

  const [application, setApplication] = useState(null);
  const [interviews, setInterviews] = useState([]);
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [accessDenied, setAccessDenied] = useState(false);
  const [notFound, setNotFound] = useState(false);

  const fetchApplicationDetails = async () => {
    setLoading(true);
    setError(null);
    setAccessDenied(false);
    setNotFound(false);

    try {
      // 1. Fetch Application Record
      const appRes = await api.get(API_ENDPOINTS.APPLICATIONS.BY_ID(applicationId));
      const appData = appRes.data;
      setApplication(appData);

      // 2. Fetch associated interviews if available
      try {
        const interviewRes = await api.get(
          API_ENDPOINTS.INTERVIEWS.BY_APPLICATION(applicationId)
        );
        setInterviews(interviewRes.data || []);
      } catch (iErr) {
        // If interviews endpoint fails or has no access, safely ignore
        console.warn('Interviews could not be loaded for application:', iErr);
      }

      // 3. Fetch Job Opportunity snapshot
      if (appData.jobId) {
        try {
          const jobRes = await api.get(API_ENDPOINTS.JOBS.BY_ID(appData.jobId));
          setJob(jobRes.data);
        } catch {
          // Non-critical, ignore
        }
      }
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true);
      } else if (err.response?.status === 404) {
        setNotFound(true);
      } else {
        setError(
          err.response?.data?.message || err.message || 'Failed to load application details.'
        );
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (applicationId) {
      fetchApplicationDetails();
    }
  }, [applicationId]);

  if (loading) {
    return <LoadingState message="Loading application details and recruitment status..." />;
  }

  if (accessDenied) {
    return (
      <EmptyState
        title="Access Denied"
        message="You do not have permission to view this application record. Candidate submissions are restricted based on authorized roles."
        icon={<Lock size={40} />}
        action={
          <Link to={defaultBackPath} className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
            &larr; {defaultBackLabel}
          </Link>
        }
      />
    );
  }

  if (notFound) {
    return (
      <EmptyState
        title="Application Not Found"
        message={`The application record with ID #${applicationId} does not exist or has been removed.`}
        icon={<Search size={40} />}
        action={
          <Link to={defaultBackPath} className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
            &larr; {defaultBackLabel}
          </Link>
        }
      />
    );
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchApplicationDetails} />;
  }

  if (!application) {
    return null;
  }

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

  // Determine Stepper Progress
  const status = application.status;
  const isTerminalFailure = status === 'REJECTED' || status === 'WITHDRAWN';

  const steps = [
    { key: 'APPLIED', label: '1. Applied', desc: 'Submission received' },
    { key: 'SHORTLISTED', label: '2. Shortlisted', desc: 'Screening passed' },
    { key: 'INTERVIEWING', label: '3. Interviewing', desc: 'Evaluation rounds' },
    { key: 'SELECTED', label: '4. Selected', desc: 'Offer confirmed' },
  ];

  const getStepStatus = (stepKey, index) => {
    if (isTerminalFailure) {
      if (status === 'REJECTED' && index === 2) return 'rejected';
      if (status === 'WITHDRAWN' && index === 2) return 'withdrawn';
      return 'completed';
    }

    const order = ['APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'SELECTED'];
    const currentIndex = order.indexOf(status);

    if (index < currentIndex) return 'completed';
    if (index === currentIndex) return 'active';
    return 'pending';
  };

  const getProgressWidth = () => {
    if (status === 'APPLIED') return '10%';
    if (status === 'SHORTLISTED') return '35%';
    if (status === 'INTERVIEWING') return '65%';
    if (status === 'SELECTED') return '100%';
    return '50%';
  };

  return (
    <div>
      {/* Back Link */}
      <div style={{ marginBottom: '1.25rem' }}>
        <Link
          to={defaultBackPath}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            fontSize: '0.875rem',
            fontWeight: 500,
            color: 'var(--text-muted)',
            textDecoration: 'none',
          }}
        >
          &larr; {defaultBackLabel}
        </Link>
      </div>

      {/* Header Banner */}
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
                fontSize: '0.8125rem',
                fontWeight: 600,
                color: 'var(--primary-color)',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                marginBottom: '0.35rem',
              }}
            >
              {application.companyName}
            </div>
            <h1
              style={{
                fontSize: '1.625rem',
                fontWeight: 700,
                color: 'var(--secondary-color)',
                margin: '0 0 0.5rem',
              }}
            >
              {application.jobTitle}
            </h1>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Application: <code>#{application.applicationId}</code>
              </span>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Submitted:{' '}
                <strong>
                  {application.appliedOn
                    ? new Date(application.appliedOn).toLocaleString()
                    : '—'}
                </strong>
              </span>
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
              Current Status
            </div>
            {getStatusBadge(application.status)}
          </div>
        </div>
      </div>

      {/* Visual Status Stepper / Progress Timeline */}
      <div className="stepper-card">
        <h2 className="job-details-section-title" style={{ marginBottom: '1.25rem' }}>
          Recruitment Workflow Progression
        </h2>

        {isTerminalFailure ? (
          <div className="alert alert-error" style={{ marginBottom: '1rem' }}>
            <strong>Application Status: {status}</strong> &bull;{' '}
            {status === 'REJECTED'
              ? 'Candidate application was not selected for this drive.'
              : 'Application was voluntarily withdrawn by candidate.'}
          </div>
        ) : null}

        <div className="stepper-container">
          <div className="stepper-track-bg" />
          <div className="stepper-track-fill" style={{ width: getProgressWidth() }} />

          {steps.map((step, idx) => {
            const stepState = getStepStatus(step.key, idx);
            return (
              <div key={step.key} className="stepper-step">
                <div className={`stepper-node ${stepState}`}>
                  {stepState === 'completed' ? <Check size={14} /> : idx + 1}
                </div>
                <div className="stepper-label">{step.label}</div>
                <div className="stepper-desc">{step.desc}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Two-Column Details Grid */}
      <div className="job-details-grid">
        {/* Left Column: Submission Details */}
        <div>
          <div className="job-details-card">
            <h2 className="job-details-section-title">Submission Details</h2>

            <div className="job-spec-list">
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <FileText size={15} /> Application ID:
                  </span>
                </span>
                <span className="job-spec-row-value">#{application.applicationId}</span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <User size={15} /> Candidate Name:
                  </span>
                </span>
                <span className="job-spec-row-value">{application.studentName}</span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Calendar size={15} /> Submitted On:
                  </span>
                </span>
                <span className="job-spec-row-value">
                  {application.appliedOn
                    ? new Date(application.appliedOn).toLocaleString()
                    : '—'}
                </span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <FileText size={15} /> Resume Document:
                  </span>
                </span>
                <span className="job-spec-row-value">
                  {application.resumeUrl ? (
                    <a
                      href={application.resumeUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-secondary"
                      style={{
                        padding: '0.25rem 0.65rem',
                        fontSize: '0.75rem',
                        textDecoration: 'none',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                      }}
                    >
                      <span>View Resume &nearr;</span>
                    </a>
                  ) : (
                    <span style={{ color: 'var(--text-muted)' }}>Not attached</span>
                  )}
                </span>
              </div>
            </div>

            {/* Candidate Remarks */}
            <div style={{ marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', marginBottom: '0.5rem' }}>
                Candidate Cover Note / Remarks
              </div>
              <div
                style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1rem',
                  fontSize: '0.875rem',
                  color: application.remarks ? 'var(--text-main)' : 'var(--text-muted)',
                  fontStyle: application.remarks ? 'normal' : 'italic',
                  lineHeight: 1.6,
                  whiteSpace: 'pre-wrap',
                }}
              >
                {application.remarks || 'No remarks or cover note provided during application submission.'}
              </div>
            </div>
          </div>

          {/* Interview Rounds */}
          <div className="job-details-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)', flexWrap: 'wrap', gap: '0.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <h2 className="job-details-section-title" style={{ margin: 0, paddingBottom: 0, borderBottom: 'none' }}>
                  Interview Evaluations
                </h2>
                {interviews.length > 0 && (
                  <span className="badge badge-info" style={{ fontSize: '0.6875rem' }}>
                    {interviews.length} {interviews.length === 1 ? 'Round' : 'Rounds'}
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {isPrivileged && (
                  <Link
                    to={`${isAdmin ? '/admin/interviews' : '/company/interviews'}?applicationId=${application.applicationId}`}
                    className="btn btn-primary"
                    style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem', textDecoration: 'none' }}
                  >
                    + Schedule Round
                  </Link>
                )}
                <Link
                  to={isAdmin ? '/admin/interviews' : isCompany ? '/company/interviews' : '/student/interviews'}
                  className="btn btn-secondary"
                  style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem', textDecoration: 'none' }}
                >
                  All Interviews &rarr;
                </Link>
              </div>
            </div>

            {interviews.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {interviews.map((iv) => (
                  <div
                    key={iv.interviewId}
                    style={{
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '1.125rem',
                      backgroundColor: '#ffffff',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.625rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span className="interview-round-pill">
                          Round #{iv.roundNumber}
                        </span>
                        <span style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--secondary-color)' }}>
                          {iv.roundType || 'General Round'}
                        </span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        {iv.result === 'PASSED' ? (
                          <span className="badge badge-result-passed">PASSED</span>
                        ) : iv.result === 'FAILED' ? (
                          <span className="badge badge-result-failed">FAILED</span>
                        ) : (
                          <span className="badge badge-result-pending">PENDING</span>
                        )}
                        {isPrivileged && (
                          <Link
                            to={`${isAdmin ? '/admin/interviews' : '/company/interviews'}?evaluateId=${iv.interviewId}`}
                            className="btn btn-secondary"
                            style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', textDecoration: 'none' }}
                          >
                            Evaluate &rarr;
                          </Link>
                        )}
                      </div>
                    </div>

                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '0.625rem', display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                        <User size={13} /> Interviewer: <strong style={{ color: 'var(--text-main)' }}>{iv.interviewer || 'TBD'}</strong>
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                        <Calendar size={13} /> Scheduled: <strong style={{ color: 'var(--text-main)' }}>
                          {iv.scheduledOn
                            ? new Date(iv.scheduledOn).toLocaleString(undefined, {
                                weekday: 'short',
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                              })
                            : 'Pending'}
                        </strong>
                      </span>
                    </div>

                    {iv.feedback && (
                      <div className="interview-feedback-box" style={{ marginTop: '0.5rem' }}>
                        <div className="interview-feedback-label" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                          <MessageSquare size={13} /> Interviewer Feedback:
                        </div>
                        <div style={{ fontStyle: 'italic' }}>
                          "{iv.feedback}"
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: 0 }}>
                No interview rounds scheduled yet. If your application is shortlisted by the recruiter, scheduled rounds and evaluator feedback will appear here.
              </p>
            )}
          </div>
        </div>

        {/* Right Column: Job Snapshot */}
        <div>
          <div className="job-details-card">
            <h2 className="job-details-section-title">Drive Overview</h2>
            <div className="job-spec-list">
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Building2 size={15} /> Recruiter:
                  </span>
                </span>
                <span className="job-spec-row-value">{application.companyName}</span>
              </div>
              <div className="job-spec-row">
                <span className="job-spec-row-label">
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Briefcase size={15} /> Position:
                  </span>
                </span>
                <span className="job-spec-row-value">{application.jobTitle}</span>
              </div>
              {job && (
                <>
                  <div className="job-spec-row">
                    <span className="job-spec-row-label">
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        <IndianRupee size={15} /> CTC Package:
                      </span>
                    </span>
                    <span className="job-spec-row-value">
                      {job.packageAmount ? `₹ ${Number(job.packageAmount).toLocaleString()}` : 'Negotiable'}
                    </span>
                  </div>
                  <div className="job-spec-row">
                    <span className="job-spec-row-label">
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        <GraduationCap size={15} /> Min CGPA:
                      </span>
                    </span>
                    <span className="job-spec-row-value">{job.minCgpa ?? 'None'}</span>
                  </div>
                </>
              )}
            </div>

            <div style={{ marginTop: '1.25rem' }}>
              <Link
                to={`/student/jobs/${application.jobId}`}
                className="btn btn-secondary btn-block"
                style={{ textAlign: 'center', textDecoration: 'none' }}
              >
                View Full Job Opportunity &rarr;
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ApplicationDetailsPage;
