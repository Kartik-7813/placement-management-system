import { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Building2,
  Briefcase,
  CalendarDays,
  RotateCw,
  X,
  Users,
  Inbox,
  Award,
  CheckCircle2,
  User,
  FileText,
  Eye,
  Check,
  Plus,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import {
  fetchCompanyPipelineApplications,
  calculatePipelineMetrics,
  updateApplicationStatus,
  APPLICATION_STATUSES,
} from '../services/applicationService';
import { fetchMyCompanyProfile, fetchCompanyJobs } from '../services/companyService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * CompanyApplicantsPage Component
 * Provides corporate recruiters with complete candidate pipeline oversight across all company openings:
 * - Scoped retrieval of candidates for company-owned jobs
 * - Dynamic job and status filtering
 * - Real-time pipeline KPI metrics
 * - Candidate profile and resume inspection
 * - Recruitment status progression state machine (Shortlist, Interview, Select, Reject)
 * - Seamless integration with Interview Scheduling Hub
 */
function CompanyApplicantsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Organization & Jobs State
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [actionError, setActionError] = useState(null);

  const [company, setCompany] = useState(null);
  const [companyJobs, setCompanyJobs] = useState([]);
  const [allApplications, setAllApplications] = useState([]);

  // Filter & Search State
  const [selectedJobId, setSelectedJobId] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Pagination State
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Confirmation Modal State
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    application: null,
    targetStatus: null,
    loading: false,
  });

  // Inspection Details Modal State
  const [inspectModal, setInspectModal] = useState({
    isOpen: false,
    application: null,
    interviews: [],
    loadingInterviews: false,
  });

  /**
   * Loads recruiter company profile, company jobs, and application pipeline.
   */
  const loadRecruiterData = async (isSilentRefresh = false) => {
    if (!isSilentRefresh) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      // 1. Resolve recruiter company
      const comp = await fetchMyCompanyProfile(user?.email);
      setCompany(comp);

      if (!comp?.companyId) {
        return;
      }

      // 2. Fetch jobs owned by this company
      const jobsPage = await fetchCompanyJobs(comp.companyId, { size: 100 });
      const jobs = jobsPage?.content || [];
      setCompanyJobs(jobs);

      // 3. Fetch applications across all owned jobs
      const apps = await fetchCompanyPipelineApplications(jobs);
      setAllApplications(apps);
    } catch (err) {
      console.error('Failed to load company applicants data:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to load recruitment pipeline.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadRecruiterData();
  }, [user?.email]);

  /**
   * Filtered applications based on selected Job, Status, and Search Query.
   */
  const filteredApplications = useMemo(() => {
    return allApplications.filter((app) => {
      // Job filter
      if (selectedJobId !== 'ALL' && String(app.jobId) !== String(selectedJobId)) {
        return false;
      }

      // Status filter
      if (selectedStatus !== 'ALL' && app.status !== selectedStatus) {
        return false;
      }

      // Text search
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const studentMatch = app.studentName?.toLowerCase().includes(q);
        const jobMatch = app.jobTitle?.toLowerCase().includes(q);
        const idMatch = String(app.applicationId).includes(q);
        if (!studentMatch && !jobMatch && !idMatch) {
          return false;
        }
      }

      return true;
    });
  }, [allApplications, selectedJobId, selectedStatus, searchQuery]);

  // Reset to page 1 whenever filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [selectedJobId, selectedStatus, searchQuery]);

  // Pipeline metrics computed across all company applications
  const pipelineMetrics = useMemo(() => {
    return calculatePipelineMetrics(allApplications);
  }, [allApplications]);

  // Paginated slice
  const totalItems = filteredApplications.length;
  const totalPages = Math.ceil(totalItems / pageSize) || 1;
  const paginatedApplications = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredApplications.slice(start, start + pageSize);
  }, [filteredApplications, currentPage, pageSize]);

  /**
   * Status Badge renderer with consistent styling
   */
  const renderStatusBadge = (status) => {
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

  /**
   * Initiates confirmation modal for status update.
   */
  const handleOpenConfirm = (application, targetStatus) => {
    setActionError(null);
    setConfirmModal({
      isOpen: true,
      application,
      targetStatus,
      loading: false,
    });
  };

  /**
   * Executes status update via PUT /api/applications/{id}/status?status=...
   */
  const handleExecuteStatusUpdate = async () => {
    const { application, targetStatus } = confirmModal;
    if (!application || !targetStatus) return;

    setConfirmModal((prev) => ({ ...prev, loading: true }));
    setActionError(null);
    setActionSuccess(null);

    try {
      const updated = await updateApplicationStatus(application.applicationId, targetStatus);
      setActionSuccess(
        `Application #${application.applicationId} for ${application.studentName} updated to ${targetStatus} successfully.`
      );

      // Close confirm modal
      setConfirmModal({ isOpen: false, application: null, targetStatus: null, loading: false });

      // If inspect modal is open with this app, update it
      if (inspectModal.isOpen && inspectModal.application?.applicationId === application.applicationId) {
        setInspectModal((prev) => ({
          ...prev,
          application: { ...prev.application, status: updated.status },
        }));
      }

      // Refresh applications data
      await loadRecruiterData(true);
    } catch (err) {
      console.error('Failed to update candidate status:', err);
      const errMsg =
        err.response?.data?.message || err.message || 'Failed to update recruitment status.';
      setActionError(errMsg);
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  /**
   * Opens the rich Candidate Inspection Modal and loads interview evaluation records.
   */
  const handleInspectApplication = async (application) => {
    setInspectModal({
      isOpen: true,
      application,
      interviews: [],
      loadingInterviews: true,
    });

    try {
      const res = await api.get(API_ENDPOINTS.INTERVIEWS.BY_APPLICATION(application.applicationId));
      setInspectModal((prev) => ({
        ...prev,
        interviews: res.data || [],
        loadingInterviews: false,
      }));
    } catch (err) {
      console.warn('Interviews could not be loaded for application:', err);
      setInspectModal((prev) => ({
        ...prev,
        interviews: [],
        loadingInterviews: false,
      }));
    }
  };

  if (loading) {
    return <LoadingState message="Loading candidate applications and recruitment pipeline..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={() => loadRecruiterData()} />;
  }

  if (!company) {
    return (
      <EmptyState
        title="Company Profile Required"
        message={`Logged in as ${user?.email}. Your recruiter account is not yet associated with a corporate profile in the institutional database.`}
        icon={<Building2 size={40} strokeWidth={1.5} />}
        action={
          <Link to="/company/profile" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
            View Profile Status &rarr;
          </Link>
        }
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Applicant Management & Recruitment Pipeline"
        subtitle={`${company.name} &bull; Manage student candidates, review resumes, evaluate interview progression, and issue hiring decisions.`}
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <Link to="/company/jobs" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
              <Briefcase size={15} className="inline-icon" />
              <span>Manage Jobs</span>
            </Link>
            <Link to="/company/interviews" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
              <CalendarDays size={15} className="inline-icon" />
              <span>Interviews</span>
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadRecruiterData(true)}
              disabled={refreshing}
              style={{ display: 'inline-flex', alignItems: 'center' }}
            >
              <RotateCw size={14} className={`inline-icon ${refreshing ? 'spin' : ''}`} />
              <span>{refreshing ? 'Refreshing...' : 'Refresh'}</span>
            </Button>
          </div>
        }
      />

      {/* Action Feedback Alerts */}
      {actionSuccess && (
        <div className="alert alert-success" style={{ marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>Success:</strong> {actionSuccess}
          </div>
          <button
            onClick={() => setActionSuccess(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', display: 'inline-flex', alignItems: 'center' }}
            aria-label="Dismiss alert"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {actionError && (
        <div className="alert alert-error" style={{ marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>Error:</strong> {actionError}
          </div>
          <button
            onClick={() => setActionError(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', display: 'inline-flex', alignItems: 'center' }}
            aria-label="Dismiss alert"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Recruitment Pipeline Summary Cards */}
      <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
        <div onClick={() => setSelectedStatus('ALL')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Total Applicants"
            value={pipelineMetrics.total}
            context="Across all company job openings"
            icon={<Users size={18} />}
          />
        </div>
        <div onClick={() => setSelectedStatus('APPLIED')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Pending Review"
            value={pipelineMetrics.applied}
            context="Awaiting initial screening"
            icon={<Inbox size={18} />}
          />
        </div>
        <div onClick={() => setSelectedStatus('SHORTLISTED')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Shortlisted"
            value={pipelineMetrics.shortlisted}
            context="Qualified for interviews"
            icon={<CheckCircle2 size={18} />}
          />
        </div>
        <div onClick={() => setSelectedStatus('INTERVIEWING')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Interviewing"
            value={pipelineMetrics.interviewing}
            context="Active evaluation rounds"
            icon={<CalendarDays size={18} />}
          />
        </div>
        <div onClick={() => setSelectedStatus('SELECTED')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Selected / Hired"
            value={pipelineMetrics.selected}
            context="Offers confirmed"
            icon={<Award size={18} />}
          />
        </div>
        <div onClick={() => setSelectedStatus('REJECTED')} style={{ cursor: 'pointer' }}>
          <StatCard
            label="Rejected"
            value={pipelineMetrics.rejected}
            context="Terminal non-selection"
            icon={<X size={18} />}
          />
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          {/* Job Opening Filter */}
          <div>
            <label className="form-label" htmlFor="jobFilterSelect">
              Filter by Job Opening
            </label>
            <select
              id="jobFilterSelect"
              className="form-input"
              value={selectedJobId}
              onChange={(e) => setSelectedJobId(e.target.value)}
            >
              <option value="ALL">All Company Openings ({companyJobs.length})</option>
              {companyJobs.map((j) => (
                <option key={j.jobId} value={j.jobId}>
                  #{j.jobId} &bull; {j.title} ({j.status})
                </option>
              ))}
            </select>
          </div>

          {/* Pipeline Status Filter */}
          <div>
            <label className="form-label" htmlFor="statusFilterSelect">
              Filter by Pipeline Status
            </label>
            <select
              id="statusFilterSelect"
              className="form-input"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <option value="ALL">All Statuses ({allApplications.length})</option>
              <option value="APPLIED">APPLIED ({pipelineMetrics.applied})</option>
              <option value="SHORTLISTED">SHORTLISTED ({pipelineMetrics.shortlisted})</option>
              <option value="INTERVIEWING">INTERVIEWING ({pipelineMetrics.interviewing})</option>
              <option value="SELECTED">SELECTED ({pipelineMetrics.selected})</option>
              <option value="REJECTED">REJECTED ({pipelineMetrics.rejected})</option>
              <option value="WITHDRAWN">WITHDRAWN ({pipelineMetrics.withdrawn})</option>
            </select>
          </div>

          {/* Search Input */}
          <div>
            <label className="form-label" htmlFor="applicantSearchInput">
              Search Candidates / Roles
            </label>
            <input
              id="applicantSearchInput"
              type="text"
              className="form-input"
              placeholder="Search by student name or role..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {/* Clear Filters Button */}
          <div>
            <Button
              type="button"
              variant="secondary"
              label="Reset Filters"
              onClick={() => {
                setSelectedJobId('ALL');
                setSelectedStatus('ALL');
                setSearchQuery('');
              }}
              style={{ width: '100%' }}
            />
          </div>
        </div>

        {/* Quick status filter pills */}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontWeight: 500 }}>Quick Status:</span>
          {['ALL', 'APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'SELECTED', 'REJECTED'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setSelectedStatus(st)}
              style={{
                border: '1px solid var(--border-color)',
                borderRadius: '999px',
                padding: '0.2rem 0.65rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                cursor: 'pointer',
                backgroundColor: selectedStatus === st ? 'var(--primary-color)' : '#f8fafc',
                color: selectedStatus === st ? '#ffffff' : 'var(--text-main)',
                transition: 'all 0.15s ease',
              }}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Applications Data Table */}
      {filteredApplications.length === 0 ? (
        <EmptyState
          title="No Applicants Found"
          message={
            allApplications.length === 0
              ? 'No students have applied to your published openings yet. Ensure your jobs are active and public.'
              : 'No applicants matched your current job or status filter criteria.'
          }
          icon={<Users size={40} strokeWidth={1.5} />}
          action={
            (selectedJobId !== 'ALL' || selectedStatus !== 'ALL' || searchQuery) ? (
              <Button
                variant="secondary"
                label="Clear All Filters"
                onClick={() => {
                  setSelectedJobId('ALL');
                  setSelectedStatus('ALL');
                  setSearchQuery('');
                }}
                style={{ marginTop: '0.75rem' }}
              />
            ) : null
          }
        />
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
              Showing {paginatedApplications.length} of {totalItems} {totalItems === 1 ? 'Applicant' : 'Applicants'}
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Page {currentPage} of {totalPages}
            </div>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ margin: 0, width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left', borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>ID</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Candidate</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Applied Job</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Applied Date</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Resume</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {paginatedApplications.map((app) => (
                  <tr key={app.applicationId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    {/* Application ID */}
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{app.applicationId}
                    </td>

                    {/* Candidate Name */}
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <User size={15} className="inline-icon" />
                        <span>{app.studentName || 'Student'}</span>
                      </div>
                      {app.studentId && (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                          Student #{app.studentId}
                        </span>
                      )}
                    </td>

                    {/* Job Title */}
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                      <div style={{ fontWeight: 500, color: 'var(--text-main)' }}>{app.jobTitle}</div>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Job #{app.jobId}
                      </span>
                    </td>

                    {/* Applied Date */}
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                      {app.appliedOn ? new Date(app.appliedOn).toLocaleDateString() : '—'}
                    </td>

                    {/* Status Badge */}
                    <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>
                      {renderStatusBadge(app.status)}
                    </td>

                    {/* Resume Document Link */}
                    <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>
                      {app.resumeUrl ? (
                        <a
                          href={app.resumeUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="btn btn-secondary"
                          style={{
                            padding: '0.2rem 0.55rem',
                            fontSize: '0.75rem',
                            textDecoration: 'none',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                          }}
                          title={app.resumeUrl}
                        >
                          <FileText size={13} className="inline-icon" />
                          <span>Resume &nearr;</span>
                        </a>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>None</span>
                      )}
                    </td>

                    {/* Pipeline Actions */}
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        {/* Inspect Details */}
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center' }}
                          onClick={() => handleInspectApplication(app)}
                          title="Inspect application details and interview history"
                        >
                          <Eye size={13} className="inline-icon" />
                          <span>Inspect</span>
                        </button>

                        {/* State-specific transition actions */}
                        {app.status === 'APPLIED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center' }}
                              onClick={() => handleOpenConfirm(app, 'SHORTLISTED')}
                              title="Shortlist candidate for interview round scheduling"
                            >
                              <Check size={13} className="inline-icon" />
                              <span>Shortlist</span>
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={13} className="inline-icon" />
                              <span>Reject</span>
                            </button>
                          </>
                        )}

                        {app.status === 'SHORTLISTED' && (
                          <>
                            <Link
                              to={`/company/interviews?applicationId=${app.applicationId}`}
                              className="btn btn-primary"
                              style={{
                                padding: '0.25rem 0.55rem',
                                fontSize: '0.75rem',
                                textDecoration: 'none',
                                display: 'inline-flex',
                                alignItems: 'center',
                              }}
                              title="Schedule interview round in Interview Management Hub"
                            >
                              <CalendarDays size={13} className="inline-icon" />
                              <span>+ Schedule Round</span>
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={13} className="inline-icon" />
                              <span>Reject</span>
                            </button>
                          </>
                        )}

                        {app.status === 'INTERVIEWING' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary"
                              style={{
                                padding: '0.25rem 0.55rem',
                                fontSize: '0.75rem',
                                backgroundColor: '#16a34a',
                                borderColor: '#16a34a',
                                display: 'inline-flex',
                                alignItems: 'center',
                              }}
                              onClick={() => handleOpenConfirm(app, 'SELECTED')}
                              title="Confirm hiring selection for candidate"
                            >
                              <Award size={13} className="inline-icon" />
                              <span>Select / Offer</span>
                            </button>
                            <Link
                              to={`/company/interviews?applicationId=${app.applicationId}`}
                              className="btn btn-secondary"
                              style={{
                                padding: '0.25rem 0.55rem',
                                fontSize: '0.75rem',
                                textDecoration: 'none',
                                display: 'inline-flex',
                                alignItems: 'center',
                              }}
                              title="View or add interview rounds"
                            >
                              <CalendarDays size={13} className="inline-icon" />
                              <span>Manage Rounds</span>
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={13} className="inline-icon" />
                              <span>Reject</span>
                            </button>
                          </>
                        )}

                        {(app.status === 'SELECTED' || app.status === 'REJECTED' || app.status === 'WITHDRAWN') && (
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic', padding: '0 0.25rem' }}>
                            Terminal
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div
              style={{
                padding: '0.875rem 1.25rem',
                borderTop: '1px solid var(--border-color)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                backgroundColor: '#f8fafc',
                flexWrap: 'wrap',
                gap: '0.5rem',
              }}
            >
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Showing {(currentPage - 1) * pageSize + 1} to{' '}
                {Math.min(currentPage * pageSize, totalItems)} of {totalItems} candidates
              </div>
              <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                <Button
                  type="button"
                  variant="secondary"
                  label="&larr; Previous"
                  onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                  disabled={currentPage === 1}
                  style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }}
                />
                <span style={{ fontSize: '0.8125rem', fontWeight: 600, padding: '0 0.5rem' }}>
                  {currentPage} / {totalPages}
                </span>
                <Button
                  type="button"
                  variant="secondary"
                  label="Next &rarr;"
                  onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }}
                />
              </div>
            </div>
          )}
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmModal.isOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title">
                {confirmModal.targetStatus === 'SHORTLISTED' && 'Confirm Candidate Shortlisting'}
                {confirmModal.targetStatus === 'SELECTED' && 'Confirm Final Selection / Offer'}
                {confirmModal.targetStatus === 'REJECTED' && 'Confirm Candidate Rejection'}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() =>
                  !confirmModal.loading &&
                  setConfirmModal({ isOpen: false, application: null, targetStatus: null, loading: false })
                }
                disabled={confirmModal.loading}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <p style={{ margin: '0 0 1rem', fontSize: '0.9375rem', lineHeight: 1.5 }}>
                Are you sure you want to update the recruitment status for candidate{' '}
                <strong>{confirmModal.application?.studentName}</strong> (Application #{confirmModal.application?.applicationId})?
              </p>

              <div
                style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.875rem',
                  fontSize: '0.8125rem',
                  lineHeight: 1.6,
                  marginBottom: '1rem',
                }}
              >
                <div><strong>Role Applied:</strong> {confirmModal.application?.jobTitle}</div>
                <div><strong>Current Status:</strong> {confirmModal.application?.status}</div>
                <div><strong>Target Status:</strong> <code>{confirmModal.targetStatus}</code></div>
              </div>

              {confirmModal.targetStatus === 'SHORTLISTED' && (
                <div className="alert alert-info" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  Shortlisting indicates this candidate meets basic screening requirements and qualifies for technical / HR interview scheduling.
                </div>
              )}

              {confirmModal.targetStatus === 'SELECTED' && (
                <div className="alert alert-success" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Important:</strong> <code>SELECTED</code> is a terminal status representing a final placement offer. Once confirmed, this status cannot be reverted.
                </div>
              )}

              {confirmModal.targetStatus === 'REJECTED' && (
                <div className="alert alert-error" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Warning:</strong> <code>REJECTED</code> is a terminal status. The candidate will no longer progress in this recruitment drive.
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                label="Cancel"
                onClick={() =>
                  setConfirmModal({ isOpen: false, application: null, targetStatus: null, loading: false })
                }
                disabled={confirmModal.loading}
              />
              <Button
                type="button"
                variant={confirmModal.targetStatus === 'REJECTED' ? 'danger' : 'primary'}
                label={
                  confirmModal.loading
                    ? 'Updating...'
                    : confirmModal.targetStatus === 'SHORTLISTED'
                    ? 'Confirm Shortlist'
                    : confirmModal.targetStatus === 'SELECTED'
                    ? 'Confirm Selection'
                    : 'Confirm Rejection'
                }
                onClick={handleExecuteStatusUpdate}
                disabled={confirmModal.loading}
              />
            </div>
          </div>
        </div>
      )}

      {/* Candidate Application Inspection Modal */}
      {inspectModal.isOpen && inspectModal.application && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '680px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <h3 className="modal-title" style={{ margin: 0 }}>
                  Candidate Application #{inspectModal.application.applicationId}
                </h3>
                {renderStatusBadge(inspectModal.application.status)}
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() =>
                  setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false })
                }
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              {/* Profile Details Grid */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                  gap: '0.875rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1rem',
                  marginBottom: '1.25rem',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Candidate Name</span>
                  <strong style={{ fontSize: '0.9375rem', color: 'var(--secondary-color)', display: 'inline-flex', alignItems: 'center' }}>
                    <User size={15} className="inline-icon" />
                    <span>{inspectModal.application.studentName}</span>
                  </strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Student ID</span>
                  <span style={{ fontSize: '0.875rem' }}>#{inspectModal.application.studentId}</span>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Job Opening</span>
                  <strong style={{ fontSize: '0.875rem', color: 'var(--text-main)' }}>
                    {inspectModal.application.jobTitle} (Job #{inspectModal.application.jobId})
                  </strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Submission Timestamp</span>
                  <span style={{ fontSize: '0.8125rem' }}>
                    {inspectModal.application.appliedOn
                      ? new Date(inspectModal.application.appliedOn).toLocaleString()
                      : '—'}
                  </span>
                </div>

                <div style={{ gridColumn: '1 / -1' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>
                    Attached Resume Document
                  </span>
                  {inspectModal.application.resumeUrl ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <a
                        href={inspectModal.application.resumeUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-secondary"
                        style={{
                          padding: '0.3rem 0.75rem',
                          fontSize: '0.8125rem',
                          textDecoration: 'none',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                        }}
                      >
                        <FileText size={14} className="inline-icon" />
                        <span>View Resume Document &nearr;</span>
                      </a>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', wordBreak: 'break-all' }}>
                        ({inspectModal.application.resumeUrl})
                      </span>
                    </div>
                  ) : (
                    <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      No resume document attached to this application.
                    </span>
                  )}
                </div>
              </div>

              {/* Candidate Cover Remarks */}
              <div style={{ marginBottom: '1.25rem' }}>
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.5rem' }}>
                  Candidate Cover Note / Statement
                </h4>
                <div
                  style={{
                    backgroundColor: '#ffffff',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.875rem',
                    fontSize: '0.875rem',
                    color: inspectModal.application.remarks ? 'var(--text-main)' : 'var(--text-muted)',
                    fontStyle: inspectModal.application.remarks ? 'normal' : 'italic',
                    whiteSpace: 'pre-wrap',
                    lineHeight: 1.5,
                  }}
                >
                  {inspectModal.application.remarks || 'No applicant remarks submitted.'}
                </div>
              </div>

              {/* Associated Interview Rounds History */}
              <div style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: 0 }}>
                    Interview Evaluation History ({inspectModal.interviews.length})
                  </h4>
                  {(inspectModal.application.status === 'SHORTLISTED' || inspectModal.application.status === 'INTERVIEWING') && (
                    <Link
                      to={`/company/interviews?applicationId=${inspectModal.application.applicationId}`}
                      className="btn btn-primary"
                      style={{ padding: '0.2rem 0.55rem', fontSize: '0.75rem', textDecoration: 'none' }}
                      onClick={() =>
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false })
                      }
                    >
                      + Schedule Round &rarr;
                    </Link>
                  )}
                </div>

                {inspectModal.loadingInterviews ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', padding: '0.75rem 0' }}>
                    Loading round evaluations...
                  </div>
                ) : inspectModal.interviews.length === 0 ? (
                  <div
                    style={{
                      padding: '0.75rem',
                      border: '1px dashed var(--border-color)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.8125rem',
                      color: 'var(--text-muted)',
                      textAlign: 'center',
                    }}
                  >
                    No interview rounds scheduled yet for this application.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
                    {inspectModal.interviews.map((round) => (
                      <div
                        key={round.interviewId}
                        style={{
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          padding: '0.75rem',
                          backgroundColor: '#ffffff',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                          <div>
                            <strong>Round {round.roundNumber}: {round.roundType}</strong>
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.5rem' }}>
                              #{round.interviewId} &bull; {round.scheduledTime ? new Date(round.scheduledTime).toLocaleString() : 'Time not set'}
                            </span>
                          </div>
                          <span className={`badge ${round.status === 'CLEARED' ? 'badge-success' : round.status === 'FAILED' ? 'badge-danger' : 'badge-info'}`} style={{ fontSize: '0.6875rem' }}>
                            {round.status}
                          </span>
                        </div>

                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                          <span>Interviewer: <strong>{round.interviewerName || 'Assigned Evaluator'}</strong></span>
                          {round.score != null && (
                            <span style={{ marginLeft: '1rem' }}>Score: <strong>{round.score} / 100</strong></span>
                          )}
                        </div>

                        {round.feedback && (
                          <div style={{ marginTop: '0.4rem', fontSize: '0.8125rem', backgroundColor: '#f8fafc', padding: '0.4rem 0.6rem', borderRadius: '4px', borderLeft: '3px solid var(--primary-color)' }}>
                            <strong>Interviewer Feedback:</strong> {round.feedback}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: '0.4rem' }}>
                {inspectModal.application.status === 'APPLIED' && (
                  <>
                    <button
                      type="button"
                      className="btn btn-primary"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'SHORTLISTED');
                      }}
                    >
                      <Check size={14} className="inline-icon" />
                      <span>Shortlist Candidate</span>
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'REJECTED');
                      }}
                    >
                      <X size={14} className="inline-icon" />
                      <span>Reject</span>
                    </button>
                  </>
                )}

                {inspectModal.application.status === 'SHORTLISTED' && (
                  <>
                    <button
                      type="button"
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'REJECTED');
                      }}
                    >
                      <X size={14} className="inline-icon" />
                      <span>Reject Candidate</span>
                    </button>
                  </>
                )}

                {inspectModal.application.status === 'INTERVIEWING' && (
                  <>
                    <button
                      type="button"
                      className="btn btn-primary"
                      style={{
                        padding: '0.35rem 0.75rem',
                        fontSize: '0.8125rem',
                        backgroundColor: '#16a34a',
                        borderColor: '#16a34a',
                        display: 'inline-flex',
                        alignItems: 'center',
                      }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'SELECTED');
                      }}
                    >
                      <Award size={14} className="inline-icon" />
                      <span>Select / Offer Candidate</span>
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'REJECTED');
                      }}
                    >
                      <X size={14} className="inline-icon" />
                      <span>Reject Candidate</span>
                    </button>
                  </>
                )}
              </div>

              <Button
                type="button"
                variant="secondary"
                label="Close"
                onClick={() =>
                  setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false })
                }
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default CompanyApplicantsPage;
