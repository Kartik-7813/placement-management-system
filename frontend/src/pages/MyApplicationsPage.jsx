import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FileText, Clock, CheckCircle2, CalendarDays, Award } from 'lucide-react';
import api from '../services/api';
import { API_ENDPOINTS, STORAGE_KEYS } from '../utils/constants';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * MyApplicationsPage Component
 * Provides authenticated students with a complete listing of submitted placement applications,
 * status filters, summary KPI metrics, pagination, and direct links to individual application details.
 */
function MyApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [allApplications, setAllApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchApplications = async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch paginated applications matching status filter
      let url = `${API_ENDPOINTS.APPLICATIONS.BASE}?page=${page}&size=${pageSize}&sort=applicationId,desc`;
      if (statusFilter) {
        url += `&status=${statusFilter}`;
      }

      const response = await api.get(url);
      const content = response.data?.content || [];
      setApplications(content);
      setTotalPages(response.data?.totalPages || 0);
      setTotalElements(response.data?.totalElements ?? 0);

      // Cache studentId from application if available
      if (content.length > 0 && content[0].studentId) {
        sessionStorage.setItem(STORAGE_KEYS.STUDENT_ID, String(content[0].studentId));
      }

      // 2. Fetch full unscoped list for metrics if filter is active, or use content
      if (!statusFilter) {
        setAllApplications(content);
      } else {
        const fullRes = await api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?size=100`);
        setAllApplications(fullRes.data?.content || []);
      }
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve your placement applications.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, [page, pageSize, statusFilter]);

  // Compute status metric counts across all applications
  const totalCount = allApplications.length;
  const appliedCount = allApplications.filter((a) => a.status === 'APPLIED').length;
  const shortlistedCount = allApplications.filter((a) => a.status === 'SHORTLISTED').length;
  const interviewingCount = allApplications.filter((a) => a.status === 'INTERVIEWING').length;
  const selectedCount = allApplications.filter((a) => a.status === 'SELECTED').length;

  // Client-side text filter on current page
  const filteredApplications = applications.filter((app) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    const titleMatch = app.jobTitle?.toLowerCase().includes(query);
    const companyMatch = app.companyName?.toLowerCase().includes(query);
    return titleMatch || companyMatch;
  });

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

  return (
    <div>
      <PageHeader
        title="My Placement Applications"
        subtitle="Track submitted drive resumes, academic screenings, evaluation rounds, and final selections"
        actions={
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Button
              type="button"
              variant="secondary"
              onClick={fetchApplications}
              label="Refresh"
            />
            <Link to="/student/jobs" className="btn btn-primary" style={{ textDecoration: 'none' }}>
              Browse Jobs &rarr;
            </Link>
          </div>
        }
      />

      {/* KPI Overview Cards */}
      <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
        <StatCard
          label="Total Applications"
          value={totalCount}
          context="Across all recruitment drives"
          icon={<FileText size={18} />}
        />
        <StatCard
          label="Under Review"
          value={appliedCount}
          context="Awaiting initial screening"
          icon={<Clock size={18} />}
        />
        <StatCard
          label="Shortlisted"
          value={shortlistedCount}
          context="Cleared academic cutoff"
          icon={<CheckCircle2 size={18} />}
        />
        <StatCard
          label="Interviews"
          value={interviewingCount}
          context="Active technical / HR rounds"
          icon={<CalendarDays size={18} />}
        />
        <StatCard
          label="Offers Extended"
          value={selectedCount}
          context="Confirmed placement offers"
          icon={<Award size={18} />}
        />
      </div>

      {/* Filtering & Search Card */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem 1.5rem' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          {/* Search Input */}
          <div style={{ flex: '1 1 240px', maxWidth: '400px' }}>
            <input
              type="text"
              className="form-input"
              placeholder="Search by job title or company..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ fontSize: '0.875rem' }}
            />
          </div>

          {/* Status Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label htmlFor="status-select" style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontWeight: 600 }}>
              Status:
            </label>
            <select
              id="status-select"
              className="form-select"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              style={{ width: 'auto', padding: '0.4rem 0.75rem', fontSize: '0.875rem' }}
            >
              <option value="">All Statuses</option>
              <option value="APPLIED">APPLIED</option>
              <option value="SHORTLISTED">SHORTLISTED</option>
              <option value="INTERVIEWING">INTERVIEWING</option>
              <option value="SELECTED">SELECTED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="WITHDRAWN">WITHDRAWN</option>
            </select>

            {statusFilter && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setStatusFilter('');
                  setPage(0);
                }}
                style={{ padding: '0.4rem 0.6rem', fontSize: '0.75rem' }}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Table / Applications List */}
      {loading ? (
        <LoadingState message="Loading your placement applications..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchApplications} />
      ) : filteredApplications.length > 0 ? (
        <div className="data-table-card">
          <div className="data-table-header">
            <div>
              <h2 className="data-table-title">Application Records</h2>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Showing {filteredApplications.length} of {totalElements} application(s)
              </span>
            </div>
          </div>

          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Application ID</th>
                  <th>Job Title</th>
                  <th>Company</th>
                  <th>Applied On</th>
                  <th>Current Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredApplications.map((app) => (
                  <tr key={app.applicationId}>
                    <td>
                      <Link
                        to={`/student/applications/${app.applicationId}`}
                        style={{ fontWeight: 600, color: 'var(--primary-color)' }}
                      >
                        #{app.applicationId}
                      </Link>
                    </td>
                    <td>
                      <Link
                        to={`/student/jobs/${app.jobId}`}
                        style={{ fontWeight: 600, color: 'inherit', textDecoration: 'none' }}
                      >
                        {app.jobTitle}
                      </Link>
                    </td>
                    <td>{app.companyName}</td>
                    <td>
                      {app.appliedOn
                        ? new Date(app.appliedOn).toLocaleDateString()
                        : '—'}
                    </td>
                    <td>{getStatusBadge(app.status)}</td>
                    <td>
                      <Link
                        to={`/student/applications/${app.applicationId}`}
                        className="btn btn-secondary"
                        style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem', textDecoration: 'none' }}
                      >
                        View Details &rarr;
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="pagination-card" style={{ border: 'none', borderTop: '1px solid var(--border-color)', borderRadius: 0 }}>
              <div className="pagination-info">
                Page {page + 1} of {totalPages} &bull; Total {totalElements} applications
              </div>
              <div className="pagination-controls">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  label="&larr; Previous"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                />
                <Button
                  type="button"
                  variant="secondary"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  label="Next &rarr;"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                />
              </div>
            </div>
          )}
        </div>
      ) : (
        <EmptyState
          title={statusFilter ? 'No Applications Match Filter' : 'No Applications Submitted'}
          message={
            statusFilter
              ? `You do not have any applications currently marked as "${statusFilter}".`
              : 'You have not submitted any placement applications yet. Explore active campus drives and submit your resume.'
          }
          icon={<FileText size={40} strokeWidth={1.5} />}
          action={
            statusFilter ? (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setStatusFilter('')}
                style={{ marginTop: '0.75rem' }}
              >
                Clear Status Filter
              </button>
            ) : (
              <Link
                to="/student/jobs"
                className="btn btn-primary"
                style={{ marginTop: '0.75rem', textDecoration: 'none' }}
              >
                Browse Available Jobs &rarr;
              </Link>
            )
          }
        />
      )}
    </div>
  );
}

export default MyApplicationsPage;
