import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Briefcase,
  Building2,
  FileText,
  RefreshCw,
  X,
  Search,
  Eye,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react';
import {
  fetchAllJobs,
  fetchCompanies,
  updateJob,
  closeJob,
} from '../services/adminService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * AdminJobsPage Component
 * Provides institutional administrators with cross-company job drive oversight:
 * - Search by job title
 * - Filter by recruiting company
 * - Filter by job status (ACTIVE / CLOSED)
 * - Detailed job criteria and package inspection
 * - Job closure (DELETE /api/jobs/{id}) with confirmation dialog
 * - Job re-opening (PUT /api/jobs/{id}) with deadline validation
 * - Direct shortcut to applications submitted for each job opening
 */
function AdminJobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialCompanyId = searchParams.get('companyId') || 'ALL';

  const [jobs, setJobs] = useState([]);
  const [companies, setCompanies] = useState([]);
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

  const [selectedCompanyId, setSelectedCompanyId] = useState(initialCompanyId);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTitle, setSearchTitle] = useState('');
  const [searchInput, setSearchInput] = useState('');

  // Modals
  const [selectedJob, setSelectedJob] = useState(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);

  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    job: null,
    actionType: null, // 'CLOSE' | 'REOPEN'
    loading: false,
  });

  // Load companies for the company filter dropdown
  useEffect(() => {
    fetchCompanies({ size: 100 })
      .then((res) => setCompanies(res?.content || []))
      .catch((err) => console.warn('Failed to load companies for filter:', err));
  }, []);

  const loadJobs = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await fetchAllJobs({
        title: searchTitle || undefined,
        companyId: selectedCompanyId !== 'ALL' ? selectedCompanyId : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        page,
        size: pageSize,
        sort: 'jobId,desc',
      });

      setJobs(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load jobs:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve job openings directory.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadJobs();
  }, [page, selectedCompanyId, statusFilter, searchTitle]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setSearchTitle(searchInput.trim());
  };

  const handleClearFilters = () => {
    setSelectedCompanyId('ALL');
    setStatusFilter('ALL');
    setSearchTitle('');
    setSearchInput('');
    setSearchParams({});
    setPage(0);
  };

  // Open confirmation modal
  const handleOpenConfirm = (job, actionType) => {
    setActionError(null);
    setActionSuccess(null);
    setConfirmModal({
      isOpen: true,
      job,
      actionType,
      loading: false,
    });
  };

  // Execute job status change
  const handleExecuteAction = async () => {
    const { job, actionType } = confirmModal;
    if (!job || !actionType) return;

    setConfirmModal((prev) => ({ ...prev, loading: true }));
    setActionError(null);
    setActionSuccess(null);

    try {
      if (actionType === 'CLOSE') {
        await closeJob(job.jobId);
        setActionSuccess(`Job opening #${job.jobId} ("${job.title}") was closed successfully.`);
      } else if (actionType === 'REOPEN') {
        await updateJob(job.jobId, {
          ...job,
          status: 'ACTIVE',
        });
        setActionSuccess(`Job opening #${job.jobId} ("${job.title}") was re-activated successfully.`);
      }

      setConfirmModal({ isOpen: false, job: null, actionType: null, loading: false });
      await loadJobs(true);
    } catch (err) {
      console.error('Failed to change job status:', err);
      setActionError(
        err.response?.data?.message || err.message || 'Failed to update job status.'
      );
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  if (loading && !refreshing) {
    return <LoadingState message="Loading campus job openings across all companies..." />;
  }

  if (error && jobs.length === 0) {
    return <ErrorState message={error} onRetry={() => loadJobs()} />;
  }

  return (
    <div>
      <PageHeader
        title="Platform Job Postings & Campus Drives"
        subtitle="Institution-wide recruitment drives, corporate eligibility standards, compensation packages, and drive status management"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Link to="/admin/applications" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
              <FileText size={15} /> Applications Oversight
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadJobs(true)}
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
        <form onSubmit={handleSearchSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label className="form-label" htmlFor="jobTitleSearchInput">
              Search by Job Title
            </label>
            <input
              id="jobTitleSearchInput"
              type="text"
              className="form-input"
              placeholder="e.g. Software Engineer..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="companySelectFilter">
              Filter by Company
            </label>
            <select
              id="companySelectFilter"
              className="form-input"
              value={selectedCompanyId}
              onChange={(e) => {
                setSelectedCompanyId(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Companies ({companies.length})</option>
              {companies.map((c) => (
                <option key={c.companyId} value={c.companyId}>
                  #{c.companyId} &bull; {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label" htmlFor="jobStatusFilter">
              Drive Status
            </label>
            <select
              id="jobStatusFilter"
              className="form-input"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button
              type="submit"
              variant="primary"
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Search size={14} /> Search
                </span>
              }
              style={{ flex: 1 }}
            />
            <Button
              type="button"
              variant="secondary"
              label="Reset"
              onClick={handleClearFilters}
            />
          </div>
        </form>
      </div>

      {/* Jobs Table */}
      {jobs.length === 0 ? (
        <EmptyState
          title="No Job Postings Found"
          message="No recruitment openings matched your specified search or company criteria."
          icon={<Briefcase size={40} />}
          action={
            (selectedCompanyId !== 'ALL' || statusFilter !== 'ALL' || searchTitle) ? (
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
              Showing {jobs.length} of {totalElements} campus job openings
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
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Job Title</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Company</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Type</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Package</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Min CGPA</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Deadline</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr key={j.jobId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{j.jobId}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      {j.title}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      <Link
                        to={`/admin/companies`}
                        style={{ color: 'var(--text-main)', textDecoration: 'none', fontWeight: 500, display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                        title={`Company #${j.companyId}`}
                      >
                        <Building2 size={13} /> {j.companyName}
                      </Link>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.75rem' }}>
                      <span className="badge badge-info">{j.jobType || 'FULL_TIME'}</span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: '#166534' }}>
                      {j.packageAmount != null ? `₹${j.packageAmount} LPA` : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      {j.minCgpa != null ? `${j.minCgpa.toFixed(2)}` : 'None'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {j.deadline ? new Date(j.deadline).toLocaleDateString() : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <span className={`badge ${j.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                        {j.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => {
                            setSelectedJob(j);
                            setDetailsModalOpen(true);
                          }}
                          title="View job details"
                        >
                          <Eye size={13} /> Details
                        </button>

                        {j.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            className="btn btn-danger"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                            onClick={() => handleOpenConfirm(j, 'CLOSE')}
                            title="Close this job opening"
                          >
                            Close Job
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-primary"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                            onClick={() => handleOpenConfirm(j, 'REOPEN')}
                            title="Re-open this job opening"
                          >
                            Re-open
                          </button>
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

      {/* Job Details Modal */}
      {detailsModalOpen && selectedJob && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '640px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <h3 className="modal-title" style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Briefcase size={18} /> {selectedJob.title}
                </h3>
                <span className={`badge ${selectedJob.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                  {selectedJob.status}
                </span>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setDetailsModalOpen(false)}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                  gap: '0.875rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1rem',
                  marginBottom: '1rem',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Job Opening ID</span>
                  <strong>#{selectedJob.jobId}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Recruiting Company</span>
                  <strong style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <Building2 size={14} /> {selectedJob.companyName} (#{selectedJob.companyId})
                  </strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Job Type</span>
                  <span>{selectedJob.jobType || 'FULL_TIME'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Salary Package</span>
                  <strong style={{ color: '#166534' }}>
                    {selectedJob.packageAmount != null ? `₹${selectedJob.packageAmount} LPA` : 'Not specified'}
                  </strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Minimum CGPA</span>
                  <span>{selectedJob.minCgpa != null ? `${selectedJob.minCgpa.toFixed(2)} / 10.0` : 'None required'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Application Deadline</span>
                  <span>{selectedJob.deadline ? new Date(selectedJob.deadline).toLocaleDateString() : 'No deadline'}</span>
                </div>
              </div>

              {selectedJob.description && (
                <div style={{ marginBottom: '1rem' }}>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.35rem' }}>
                    Job Description
                  </h4>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-main)', lineHeight: 1.5, margin: 0, whiteSpace: 'pre-wrap' }}>
                    {selectedJob.description}
                  </p>
                </div>
              )}

              {selectedJob.requiredSkills && (
                <div style={{ marginBottom: '1rem' }}>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.35rem' }}>
                    Required Skills
                  </h4>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-main)', margin: 0 }}>
                    {selectedJob.requiredSkills}
                  </p>
                </div>
              )}

              {selectedJob.eligibilityCriteria && (
                <div style={{ marginBottom: '1rem' }}>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.35rem' }}>
                    Eligibility Criteria
                  </h4>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-main)', margin: 0 }}>
                    {selectedJob.eligibilityCriteria}
                  </p>
                </div>
              )}
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <Link
                to={`/admin/applications?jobId=${selectedJob.jobId}`}
                className="btn btn-primary"
                style={{ fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                onClick={() => setDetailsModalOpen(false)}
              >
                <FileText size={14} /> View Applications for this Job &rarr;
              </Link>
              <Button
                type="button"
                variant="secondary"
                label="Close"
                onClick={() => setDetailsModalOpen(false)}
              />
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmModal.isOpen && confirmModal.job && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {confirmModal.actionType === 'CLOSE' ? (
                  <>
                    <AlertTriangle size={18} color="var(--warning-color)" /> Confirm Job Closure
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} color="var(--success-color)" /> Confirm Job Re-activation
                  </>
                )}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !confirmModal.loading && setConfirmModal({ isOpen: false, job: null, actionType: null, loading: false })}
                disabled={confirmModal.loading}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <p style={{ margin: '0 0 1rem', fontSize: '0.9375rem', lineHeight: 1.5 }}>
                Are you sure you want to {confirmModal.actionType === 'CLOSE' ? 'close' : 're-open'}{' '}
                <strong>{confirmModal.job.title}</strong> by <strong>{confirmModal.job.companyName}</strong> (Job #{confirmModal.job.jobId})?
              </p>

              {confirmModal.actionType === 'CLOSE' ? (
                <div className="alert alert-warning" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  Closing this job will prevent students from submitting new applications. Existing applications will remain intact.
                </div>
              ) : (
                <div className="alert alert-info" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  Re-opening sets the status to <code>ACTIVE</code>. Ensure the application deadline is current or in the future.
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                label="Cancel"
                onClick={() => setConfirmModal({ isOpen: false, job: null, actionType: null, loading: false })}
                disabled={confirmModal.loading}
              />
              <Button
                type="button"
                variant={confirmModal.actionType === 'CLOSE' ? 'danger' : 'primary'}
                label={confirmModal.loading ? 'Updating...' : confirmModal.actionType === 'CLOSE' ? 'Confirm Closure' : 'Confirm Re-open'}
                onClick={handleExecuteAction}
                disabled={confirmModal.loading}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminJobsPage;
