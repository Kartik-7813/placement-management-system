import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Briefcase, FileText, CheckCircle2, CalendarDays, Award } from 'lucide-react';
import useAuth from '../hooks/useAuth';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * StudentDashboardPage Component
 * Provides a student-specific overview of active placement opportunities,
 * submitted applications, and application statuses fetched from backend APIs.
 */
function StudentDashboardPage() {
  const { user } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [totalJobs, setTotalJobs] = useState(0);
  const [applications, setApplications] = useState([]);
  const [totalApplications, setTotalApplications] = useState(0);

  const fetchStudentData = async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch available jobs (PermitAll GET /api/jobs?size=5&sort=jobId,desc)
      const jobsPromise = api.get(`${API_ENDPOINTS.JOBS.BASE}?size=5&sort=jobId,desc`);

      // 2. Fetch authenticated student's applications (GET /api/applications?sort=applicationId,desc)
      // The backend ApplicationService automatically scopes this to the authenticated student's profile.
      const appsPromise = api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?sort=applicationId,desc`);

      const [jobsResponse, appsResponse] = await Promise.all([jobsPromise, appsPromise]);

      setJobs(jobsResponse.data.content || []);
      setTotalJobs(jobsResponse.data.totalElements ?? 0);

      const appsList = appsResponse.data.content || [];
      setApplications(appsList);
      setTotalApplications(appsResponse.data.totalElements ?? 0);
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve student dashboard data.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentData();
  }, []);

  if (loading) {
    return <LoadingState message="Loading your placement applications and available drives..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchStudentData} />;
  }

  // Calculate metrics from student applications
  const shortlistedCount = applications.filter((app) => app.status === 'SHORTLISTED').length;
  const interviewingCount = applications.filter((app) => app.status === 'INTERVIEWING').length;
  const selectedCount = applications.filter((app) => app.status === 'SELECTED').length;

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
        title={`Welcome back, ${user?.email}`}
        subtitle="Track your campus placement applications, shortlisted drives, and interview evaluations"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Link to="/student/placement" className="btn btn-primary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
              <Award size={16} className="inline-icon" />
              <span>My Placement</span>
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={fetchStudentData}
              label="Refresh"
            />
          </div>
        }
      />

      {/* KPI Overview Cards */}
      <div className="stat-grid">
        <Link to="/student/jobs" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Available Job Openings"
            value={totalJobs}
            context="Active campus recruitment listings"
            icon={<Briefcase size={18} />}
          />
        </Link>
        <Link to="/student/applications" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="My Applications"
            value={totalApplications}
            context={selectedCount > 0 ? `${selectedCount} offer(s) received` : 'Submitted to recruiters'}
            icon={<FileText size={18} />}
          />
        </Link>
        <Link to="/student/applications" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Shortlisted Drives"
            value={shortlistedCount}
            context="Cleared initial academic screening"
            icon={<CheckCircle2 size={18} />}
          />
        </Link>
        <Link to="/student/interviews" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Interviews In Progress"
            value={interviewingCount}
            context="Active evaluation rounds"
            icon={<CalendarDays size={18} />}
          />
        </Link>
      </div>

      {/* Recent Applications Section */}
      <div className="data-table-card">
        <div className="data-table-header">
          <div>
            <h2 className="data-table-title">My Recent Applications</h2>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Showing {applications.length} of {totalApplications} submission(s)
            </span>
          </div>
          <Link
            to="/student/applications"
            className="btn btn-secondary"
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', textDecoration: 'none' }}
          >
            View All Applications &rarr;
          </Link>
        </div>
        {applications.length > 0 ? (
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
                {applications.map((app) => (
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
                        to={`/student/applications/${app.applicationId}`}
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
                        View &rarr;
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No Applications Submitted"
            message="You have not applied to any campus placement jobs yet. Browse available jobs below to submit your resume."
            icon={<FileText size={40} strokeWidth={1.5} />}
          />
        )}
      </div>

      {/* Available Jobs Preview */}
      <div className="data-table-card">
        <div className="data-table-header">
          <div>
            <h2 className="data-table-title">Recent Campus Job Openings</h2>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Showing latest {jobs.length} of {totalJobs} opportunities
            </span>
          </div>
          <Link
            to="/student/jobs"
            className="btn btn-secondary"
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
          >
            Browse All Jobs &rarr;
          </Link>
        </div>
        {jobs.length > 0 ? (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Job Title</th>
                  <th>Recruiting Company</th>
                  <th>Min CGPA</th>
                  <th>Package (CTC)</th>
                  <th>Application Deadline</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.jobId}>
                    <td>
                      <Link
                        to={`/student/jobs/${job.jobId}`}
                        style={{ fontWeight: 600, color: 'var(--primary-color)' }}
                      >
                        {job.title}
                      </Link>
                      {job.jobType && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {job.jobType}
                        </div>
                      )}
                    </td>
                    <td>{job.companyName}</td>
                    <td>{job.minCgpa != null ? job.minCgpa : 'None'}</td>
                    <td>
                      {job.packageAmount != null
                        ? `₹ ${Number(job.packageAmount).toLocaleString()}`
                        : 'Negotiable'}
                    </td>
                    <td>{job.deadline || 'Open'}</td>
                    <td>
                      <span className={`badge ${job.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                        {job.status}
                      </span>
                    </td>
                    <td>
                      <Link
                        to={`/student/jobs/${job.jobId}`}
                        className="btn btn-secondary"
                        style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
                      >
                        View &rarr;
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No Jobs Available"
            message="There are currently no active campus placement drives listed. Please check back soon."
            icon={<Briefcase size={40} strokeWidth={1.5} />}
          />
        )}
      </div>
    </div>
  );
}

export default StudentDashboardPage;
