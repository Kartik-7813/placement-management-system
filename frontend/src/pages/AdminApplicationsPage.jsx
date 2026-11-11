import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  FileText,
  CalendarDays,
  RefreshCw,
  X,
  Search,
  User,
  Building2,
  Eye,
  Award,
  ExternalLink,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import {
  fetchAllApplications,
  fetchAllJobs,
  updateApplicationStatus,
} from '../services/adminService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * AdminApplicationsPage Component
 * Provides institutional administrators with platform-wide candidate application oversight:
 * - Direct query filtering by jobId, studentId, or recruitment status
 * - Candidate resume document inspection
 * - Candidate statement / cover note review
 * - Full interview evaluation rounds and feedback inspection
 * - State machine status transitions (SHORTLISTED, SELECTED, REJECTED) with confirmation dialogs
 * - Shortcut navigation to Interview Scheduling Hub
 */
function AdminApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialJobId = searchParams.get('jobId') || 'ALL';
  const initialStudentId = searchParams.get('studentId') || '';

  const [applications, setApplications] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Pagination & Filters
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [selectedJobId, setSelectedJobId] = useState(initialJobId);
  const [selectedStudentId, setSelectedStudentId] = useState(initialStudentId);
  const [studentInput, setStudentInput] = useState(initialStudentId);
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modals
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    application: null,
    targetStatus: null,
    loading: false,
  });

  const [inspectModal, setInspectModal] = useState({
    isOpen: false,
    application: null,
    interviews: [],
    loadingInterviews: false,
  });

  // Load jobs for the job filter dropdown
  useEffect(() => {
    fetchAllJobs({ size: 100 })
      .then((res) => setJobs(res?.content || []))
      .catch((err) => console.warn('Failed to load jobs for filter:', err));
  }, []);

  const loadApplications = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await fetchAllApplications({
        jobId: selectedJobId !== 'ALL' ? selectedJobId : undefined,
        studentId: selectedStudentId ? selectedStudentId : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        page,
        size: pageSize,
        sort: 'applicationId,desc',
      });

      setApplications(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load applications:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve application registry.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadApplications();
  }, [page, selectedJobId, selectedStudentId, statusFilter]);

  const handleStudentFilterSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setSelectedStudentId(studentInput.trim());
  };

  const handleClearFilters = () => {
    setSelectedJobId('ALL');
    setSelectedStudentId('');
    setStudentInput('');
    setStatusFilter('ALL');
    setSearchParams({});
    setPage(0);
  };

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

  // Open confirmation modal
  const handleOpenConfirm = (application, targetStatus) => {
    setActionError(null);
    setActionSuccess(null);
    setConfirmModal({
      isOpen: true,
      application,
      targetStatus,
      loading: false,
    });
  };

  // Execute status update
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

      setConfirmModal({ isOpen: false, application: null, targetStatus: null, loading: false });

      if (inspectModal.isOpen && inspectModal.application?.applicationId === application.applicationId) {
        setInspectModal((prev) => ({
          ...prev,
          application: { ...prev.application, status: updated.status },
        }));
      }

      await loadApplications(true);
    } catch (err) {
      console.error('Failed to update application status:', err);
      setActionError(
        err.response?.data?.message || err.message || 'Failed to update recruitment status.'
      );
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  // Inspect application details and load interviews
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

  if (loading && !refreshing) {
    return <LoadingState message="Loading platform applications oversight..." />;
  }

  if (error && applications.length === 0) {
    return <ErrorState message={error} onRetry={() => loadApplications()} />;
  }

  return (
    <div>
      <PageHeader
        title="Institution-Wide Application Oversight"
        subtitle="Cross-company placement submissions, candidate screening, interview progression, and hiring decisions"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Link to="/admin/interviews" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
              <CalendarDays size={15} /> Interview Hub
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadApplications(true)}
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <RefreshCw size={14} className={refreshing ? 'spin' : ''} /> {refreshing ? 'Refreshing...' : 'Refresh'}
                </span>
              }
              disabled={refreshing}
            />
          </div>
        }
      />

      {/* Action Alerts */}
      {actionSuccess && (
        <div className="alert alert-success" style={{ marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div><strong>Success:</strong> {actionSuccess}</div>
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
          <div><strong>Error:</strong> {actionError}</div>
          <button
            onClick={() => setActionError(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', display: 'inline-flex', alignItems: 'center' }}
            aria-label="Dismiss alert"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Filter Card */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label className="form-label" htmlFor="jobFilter">
              Filter by Job Opening
            </label>
            <select
              id="jobFilter"
              className="form-input"
              value={selectedJobId}
              onChange={(e) => {
                setSelectedJobId(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Job Openings ({jobs.length})</option>
              {jobs.map((j) => (
                <option key={j.jobId} value={j.jobId}>
                  #{j.jobId} &bull; {j.title} ({j.companyName})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label" htmlFor="statusFilter">
              Pipeline Status
            </label>
            <select
              id="statusFilter"
              className="form-input"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="APPLIED">APPLIED</option>
              <option value="SHORTLISTED">SHORTLISTED</option>
              <option value="INTERVIEWING">INTERVIEWING</option>
              <option value="SELECTED">SELECTED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="WITHDRAWN">WITHDRAWN</option>
            </select>
          </div>

          <form onSubmit={handleStudentFilterSubmit}>
            <label className="form-label" htmlFor="studentIdFilter">
              Filter by Student ID
            </label>
            <div style={{ display: 'flex', gap: '0.4rem' }}>
              <input
                id="studentIdFilter"
                type="number"
                className="form-input"
                placeholder="e.g. 5"
                value={studentInput}
                onChange={(e) => setStudentInput(e.target.value)}
              />
              <Button type="submit" variant="primary" label="Go" style={{ padding: '0.4rem 0.75rem' }} />
            </div>
          </form>

          <div>
            <Button
              type="button"
              variant="secondary"
              label="Reset Filters"
              onClick={handleClearFilters}
              style={{ width: '100%' }}
            />
          </div>
        </div>

        {/* Quick status pills */}
        <div style={{ display: 'flex', gap: '0.4rem', marginTop: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontWeight: 500 }}>Quick Status:</span>
          {['ALL', 'APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'SELECTED', 'REJECTED'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => {
                setStatusFilter(st);
                setPage(0);
              }}
              style={{
                border: '1px solid var(--border-color)',
                borderRadius: '999px',
                padding: '0.2rem 0.6rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                cursor: 'pointer',
                backgroundColor: statusFilter === st ? 'var(--primary-color)' : '#f8fafc',
                color: statusFilter === st ? '#ffffff' : 'var(--text-main)',
                transition: 'all 0.15s ease',
              }}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Applications Table */}
      {applications.length === 0 ? (
        <EmptyState
          title="No Applications Found"
          message="No student applications matched your specified job, student ID, or status filter criteria."
          icon={<FileText size={40} />}
          action={
            (selectedJobId !== 'ALL' || selectedStudentId || statusFilter !== 'ALL') ? (
              <Button
                variant="secondary"
                label="Clear All Filters"
                onClick={handleClearFilters}
                style={{ marginTop: '0.75rem' }}
              />
            ) : null
          }
        />
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
              Showing {applications.length} of {totalElements} platform applications
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </div>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ margin: 0, width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left', borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>ID</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Student</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Company</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Applied Job</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Applied Date</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Resume</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((app) => (
                  <tr key={app.applicationId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{app.applicationId}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      <Link
                        to={`/admin/students`}
                        style={{ color: 'inherit', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                        title={`Student ID #${app.studentId}`}
                      >
                        <User size={13} /> {app.studentName}
                      </Link>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                        Student #{app.studentId}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                        <Building2 size={13} /> {app.companyName}
                      </span>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                      <div style={{ fontWeight: 500, color: 'var(--text-main)' }}>{app.jobTitle}</div>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Job #{app.jobId}
                      </span>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                      {app.appliedOn ? new Date(app.appliedOn).toLocaleDateString() : '—'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>
                      {renderStatusBadge(app.status)}
                    </td>

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
                        >
                          <FileText size={12} /> Resume &nearr;
                        </a>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>None</span>
                      )}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => handleInspectApplication(app)}
                          title="Inspect application details, cover note & interview rounds"
                        >
                          <Eye size={12} /> Inspect
                        </button>

                        {/* State-Machine Actions */}
                        {app.status === 'APPLIED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              onClick={() => handleOpenConfirm(app, 'SHORTLISTED')}
                              title="Shortlist candidate for interview round scheduling"
                            >
                              <CheckCircle2 size={12} /> Shortlist
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={12} /> Reject
                            </button>
                          </>
                        )}

                        {app.status === 'SHORTLISTED' && (
                          <>
                            <Link
                              to={`/admin/interviews?applicationId=${app.applicationId}`}
                              className="btn btn-primary"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              title="Schedule interview round in Interview Management Hub"
                            >
                              <CalendarDays size={12} /> Schedule Round
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={12} /> Reject
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
                                gap: '0.3rem',
                              }}
                              onClick={() => handleOpenConfirm(app, 'SELECTED')}
                              title="Confirm hiring selection for candidate"
                            >
                              <Award size={12} /> Select / Offer
                            </button>
                            <Link
                              to={`/admin/interviews?applicationId=${app.applicationId}`}
                              className="btn btn-secondary"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              title="Manage interview rounds"
                            >
                              <CalendarDays size={12} /> Manage Rounds
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              onClick={() => handleOpenConfirm(app, 'REJECTED')}
                              title="Reject application"
                            >
                              <X size={12} /> Reject
                            </button>
                          </>
                        )}

                        {app.status === 'SELECTED' && (
                          <Link
                            to={`/admin/placements?create=true&applicationId=${app.applicationId}&studentId=${app.studentId}`}
                            className="btn btn-primary"
                            style={{
                              padding: '0.25rem 0.55rem',
                              fontSize: '0.75rem',
                              backgroundColor: '#15803d',
                              borderColor: '#15803d',
                              textDecoration: 'none',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.3rem',
                            }}
                            title="Record confirmed placement offer for this selected student"
                          >
                            <Award size={12} /> Record Placement
                          </Link>
                        )}

                        {(app.status === 'REJECTED' || app.status === 'WITHDRAWN') && (
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
                Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements}
              </div>
              <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                <Button
                  type="button"
                  variant="secondary"
                  label="&larr; Previous"
                  onClick={() => setPage((p) => Math.max(p - 1, 0))}
                  disabled={page === 0}
                  style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }}
                />
                <span style={{ fontSize: '0.8125rem', fontWeight: 600, padding: '0 0.5rem' }}>
                  {page + 1} / {totalPages}
                </span>
                <Button
                  type="button"
                  variant="secondary"
                  label="Next &rarr;"
                  onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                  disabled={page >= totalPages - 1}
                  style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }}
                />
              </div>
            </div>
          )}
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmModal.isOpen && confirmModal.application && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {confirmModal.targetStatus === 'SHORTLISTED' && (
                  <>
                    <CheckCircle2 size={18} color="var(--primary-color)" /> Confirm Candidate Shortlisting
                  </>
                )}
                {confirmModal.targetStatus === 'SELECTED' && (
                  <>
                    <Award size={18} color="var(--success-color)" /> Confirm Final Selection / Offer
                  </>
                )}
                {confirmModal.targetStatus === 'REJECTED' && (
                  <>
                    <AlertTriangle size={18} color="var(--danger-color)" /> Confirm Candidate Rejection
                  </>
                )}
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
                <div><strong>Company:</strong> {confirmModal.application?.companyName}</div>
                <div><strong>Role Applied:</strong> {confirmModal.application?.jobTitle}</div>
                <div><strong>Current Status:</strong> {confirmModal.application?.status}</div>
                <div><strong>Target Status:</strong> <code>{confirmModal.targetStatus}</code></div>
              </div>

              {confirmModal.targetStatus === 'SHORTLISTED' && (
                <div className="alert alert-info" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  Shortlisting qualifies the candidate for technical and HR interview rounds.
                </div>
              )}

              {confirmModal.targetStatus === 'SELECTED' && (
                <div className="alert alert-success" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Important:</strong> <code>SELECTED</code> is a terminal state representing a final offer. Once confirmed, this status cannot be reverted.
                </div>
              )}

              {confirmModal.targetStatus === 'REJECTED' && (
                <div className="alert alert-error" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Warning:</strong> <code>REJECTED</code> is a terminal state. The candidate will no longer progress in this recruitment drive.
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
                  Application #{inspectModal.application.applicationId}
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
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Candidate</span>
                  <strong style={{ fontSize: '0.9375rem', color: 'var(--secondary-color)', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <User size={15} /> {inspectModal.application.studentName} (#{inspectModal.application.studentId})
                  </strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Recruiting Company</span>
                  <strong style={{ fontSize: '0.9375rem', color: 'var(--secondary-color)', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <Building2 size={15} /> {inspectModal.application.companyName}
                  </strong>
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
                        <FileText size={14} /> View Resume Document &nearr;
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

              {/* Remarks */}
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

              {/* Interview Rounds */}
              <div style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: 0 }}>
                    Interview Evaluation Rounds ({inspectModal.interviews.length})
                  </h4>
                  {(inspectModal.application.status === 'SHORTLISTED' || inspectModal.application.status === 'INTERVIEWING') && (
                    <Link
                      to={`/admin/interviews?applicationId=${inspectModal.application.applicationId}`}
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
                            <strong>Feedback:</strong> {round.feedback}
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
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'SHORTLISTED');
                      }}
                    >
                      <CheckCircle2 size={13} /> Shortlist
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'REJECTED');
                      }}
                    >
                      <X size={13} /> Reject
                    </button>
                  </>
                )}

                {inspectModal.application.status === 'SHORTLISTED' && (
                  <button
                    type="button"
                    className="btn btn-danger"
                    style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                    onClick={() => {
                      const app = inspectModal.application;
                      setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                      handleOpenConfirm(app, 'REJECTED');
                    }}
                  >
                    <X size={13} /> Reject Candidate
                  </button>
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
                        gap: '0.3rem',
                      }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'SELECTED');
                      }}
                    >
                      <Award size={13} /> Select / Offer
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                      onClick={() => {
                        const app = inspectModal.application;
                        setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false });
                        handleOpenConfirm(app, 'REJECTED');
                      }}
                    >
                      <X size={13} /> Reject Candidate
                    </button>
                  </>
                )}

                {inspectModal.application.status === 'SELECTED' && (
                  <Link
                    to={`/admin/placements?create=true&applicationId=${inspectModal.application.applicationId}&studentId=${inspectModal.application.studentId}`}
                    className="btn btn-primary"
                    style={{
                      padding: '0.35rem 0.75rem',
                      fontSize: '0.8125rem',
                      backgroundColor: '#15803d',
                      borderColor: '#15803d',
                      textDecoration: 'none',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                    }}
                    onClick={() =>
                      setInspectModal({ isOpen: false, application: null, interviews: [], loadingInterviews: false })
                    }
                  >
                    <Award size={13} /> Record Placement Offer &rarr;
                  </Link>
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

export default AdminApplicationsPage;
