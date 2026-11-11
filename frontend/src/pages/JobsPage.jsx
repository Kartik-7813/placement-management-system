import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Briefcase, Calendar, X } from 'lucide-react';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import Button from '../components/Button';

/**
 * JobsPage Component
 * Provides campus placement opportunity browsing with search, multi-field filtering,
 * debouncing, and server-side pagination.
 */
function JobsPage() {
  // Job listings data
  const [jobs, setJobs] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isFirst, setIsFirst] = useState(true);
  const [isLast, setIsLast] = useState(true);

  // Filter state
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedCompany, setSelectedCompany] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('ACTIVE');
  const [minCgpa, setMinCgpa] = useState('');

  // Debounce search input by 400ms to avoid excessive API requests
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm.trim());
      setPage(0); // Reset to page 0 on search query change
    }, 400);

    return () => {
      clearTimeout(handler);
    };
  }, [searchTerm]);

  // Load companies for the filter dropdown
  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const response = await api.get(`${API_ENDPOINTS.COMPANIES.BASE}?size=100&sort=name,asc`);
        setCompanies(response.data.content || []);
      } catch {
        // Companies list is non-critical for basic job listing
      }
    };
    fetchCompanies();
  }, []);

  // Fetch paginated jobs with active filters
  const fetchJobs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.append('page', page);
      params.append('size', size);
      params.append('sort', 'jobId,desc');

      if (debouncedSearch) {
        params.append('title', debouncedSearch);
      }
      if (selectedCompany) {
        params.append('companyId', selectedCompany);
      }
      if (selectedStatus) {
        params.append('status', selectedStatus);
      }
      if (minCgpa !== '') {
        params.append('minCgpa', minCgpa);
      }

      const response = await api.get(`${API_ENDPOINTS.JOBS.BASE}?${params.toString()}`);
      const pageData = response.data;

      setJobs(pageData.content || []);
      setTotalElements(pageData.totalElements ?? 0);
      setTotalPages(pageData.totalPages ?? 0);
      setIsFirst(pageData.first ?? (page === 0));
      setIsLast(pageData.last ?? (page >= (pageData.totalPages ?? 1) - 1));
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to load placement job opportunities.'
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, debouncedSearch, selectedCompany, selectedStatus, minCgpa]);

  useEffect(() => {
    fetchJobs();
  }, [fetchJobs]);

  const handleResetFilters = () => {
    setSearchTerm('');
    setDebouncedSearch('');
    setSelectedCompany('');
    setSelectedStatus('ACTIVE');
    setMinCgpa('');
    setPage(0);
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

  return (
    <div>
      <PageHeader
        title="Browse Placement Opportunities"
        subtitle="Explore active campus recruitment drives, review eligibility criteria, and discover career opportunities"
        actions={
          <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Total Listed: <strong>{totalElements}</strong>
          </div>
        }
      />

      {/* Filter and Search Bar */}
      <div className="filter-card">
        <div className="filter-grid">
          {/* Search by Title */}
          <div className="filter-item">
            <label htmlFor="search-title" className="filter-label">
              Search Job Title
            </label>
            <div className="search-input-group">
              <input
                id="search-title"
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="e.g. Backend Developer..."
                className="form-input"
              />
              {searchTerm && (
                <button
                  type="button"
                  className="search-clear-btn"
                  onClick={() => setSearchTerm('')}
                  title="Clear search"
                  aria-label="Clear search"
                  style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
                >
                  <X size={14} />
                </button>
              )}
            </div>
          </div>

          {/* Filter by Company */}
          <div className="filter-item">
            <label htmlFor="filter-company" className="filter-label">
              Company
            </label>
            <select
              id="filter-company"
              value={selectedCompany}
              onChange={(e) => {
                setSelectedCompany(e.target.value);
                setPage(0);
              }}
              className="form-select"
            >
              <option value="">All Companies</option>
              {companies.map((c) => (
                <option key={c.companyId} value={c.companyId}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* Filter by Status */}
          <div className="filter-item">
            <label htmlFor="filter-status" className="filter-label">
              Status
            </label>
            <select
              id="filter-status"
              value={selectedStatus}
              onChange={(e) => {
                setSelectedStatus(e.target.value);
                setPage(0);
              }}
              className="form-select"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active Drives</option>
              <option value="CLOSED">Closed Drives</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>

          {/* Filter by Max Allowed Cutoff / Student CGPA */}
          <div className="filter-item">
            <label htmlFor="filter-cgpa" className="filter-label">
              Max Required CGPA
            </label>
            <input
              id="filter-cgpa"
              type="number"
              step="0.1"
              min="0"
              max="10"
              value={minCgpa}
              onChange={(e) => {
                setMinCgpa(e.target.value);
                setPage(0);
              }}
              placeholder="e.g. 7.5"
              className="form-input"
            />
          </div>

          {/* Reset Filters */}
          <div className="filter-actions">
            <Button
              type="button"
              variant="secondary"
              onClick={handleResetFilters}
              label="Reset"
              title="Clear all filters"
            />
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      {loading ? (
        <LoadingState message="Searching placement opportunities..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchJobs} />
      ) : jobs.length === 0 ? (
        <EmptyState
          title="No Placement Opportunities Found"
          message="No recruitment drives match your current filter criteria. Try searching for a different keyword or clearing your filters."
          icon={<Briefcase size={40} strokeWidth={1.5} />}
          action={
            <Button
              type="button"
              variant="primary"
              onClick={handleResetFilters}
              label="Clear Filters"
              style={{ marginTop: '0.75rem' }}
            />
          }
        />
      ) : (
        <>
          {/* Job Cards Grid */}
          <div className="job-cards-grid">
            {jobs.map((job) => {
              const skills = formatSkills(job.requiredSkills);
              const isClosed = job.status === 'CLOSED';
              const isDeadlinePassed =
                job.deadline && new Date(job.deadline) < new Date().setHours(0, 0, 0, 0);

              return (
                <div key={job.jobId} className="job-card">
                  <div className="job-card-header">
                    <div className="job-card-meta">
                      <span className="job-card-company">{job.companyName}</span>
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
                    </div>
                    <h3 className="job-card-title">{job.title}</h3>
                  </div>

                  <div className="job-card-body">
                    <div className="job-card-specs">
                      <div className="job-card-spec-item">
                        <span className="job-card-spec-label">CTC:</span>
                        <strong>{formatPackage(job.packageAmount)}</strong>
                      </div>
                      <div className="job-card-spec-item">
                        <span className="job-card-spec-label">Min CGPA:</span>
                        <strong>{job.minCgpa != null ? job.minCgpa : 'None'}</strong>
                      </div>
                      {job.jobType && (
                        <div className="job-card-spec-item">
                          <span className="job-card-spec-label">Type:</span>
                          <span>{job.jobType}</span>
                        </div>
                      )}
                    </div>

                    {skills.length > 0 && (
                      <div className="job-skills-container">
                        {skills.slice(0, 4).map((skill, index) => (
                          <span key={index} className="skill-pill">
                            {skill}
                          </span>
                        ))}
                        {skills.length > 4 && (
                          <span className="skill-pill" style={{ color: 'var(--text-muted)' }}>
                            +{skills.length - 4} more
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="job-card-footer">
                    <span className="job-card-deadline">
                      {job.deadline ? (
                        <span style={{ color: isDeadlinePassed ? 'var(--danger-color)' : 'inherit', display: 'inline-flex', alignItems: 'center' }}>
                          <Calendar size={13} className="inline-icon" />
                          <span>{isDeadlinePassed ? 'Expired: ' : 'Deadline: '} {job.deadline}</span>
                        </span>
                      ) : (
                        <span style={{ display: 'inline-flex', alignItems: 'center' }}>
                          <Calendar size={13} className="inline-icon" />
                          <span>No deadline</span>
                        </span>
                      )}
                    </span>
                    <Link
                      to={`/student/jobs/${job.jobId}`}
                      className="btn btn-primary"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                    >
                      View Details &rarr;
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Pagination Controls */}
          {totalPages > 0 && (
            <div className="pagination-card">
              <div className="pagination-info">
                Showing page <strong>{page + 1}</strong> of <strong>{totalPages}</strong> (
                {totalElements} total {totalElements === 1 ? 'opening' : 'openings'})
              </div>

              <div className="pagination-controls">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={isFirst || page === 0}
                  onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                  label="&larr; Previous"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                />
                <Button
                  type="button"
                  variant="secondary"
                  disabled={isLast || page >= totalPages - 1}
                  onClick={() => setPage((prev) => prev + 1)}
                  label="Next &rarr;"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                />
              </div>

              <div className="pagination-size-select">
                <span>Per page:</span>
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="form-select"
                  style={{ padding: '0.25rem 0.5rem', width: 'auto' }}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default JobsPage;
