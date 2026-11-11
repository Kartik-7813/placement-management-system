import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Briefcase,
  Users,
  CalendarDays,
  Award,
  Clock,
  ShieldCheck,
  CheckCircle2,
  Inbox,
  RefreshCw,
  Plus,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import { fetchMyCompanyProfile, fetchCompanyMetrics, fetchCompanyJobs } from '../services/companyService';
import { fetchCompanyPipelineApplications, calculatePipelineMetrics } from '../services/applicationService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * CompanyDashboardPage Component
 * Provides an authorized recruitment coordinator dashboard displaying real,
 * company-specific placement drive metrics, active hiring pipelines, and quick workflow actions.
 */
function CompanyDashboardPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [company, setCompany] = useState(null);
  const [metrics, setMetrics] = useState({
    totalJobs: 0,
    activeJobs: 0,
    closedJobs: 0,
    nearingDeadline: 0,
    recentJobs: [],
  });
  const [pipelineMetrics, setPipelineMetrics] = useState({
    total: 0,
    applied: 0,
    shortlisted: 0,
    interviewing: 0,
    selected: 0,
    rejected: 0,
    withdrawn: 0,
  });

  const loadDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Resolve recruiter's associated company profile
      const comp = await fetchMyCompanyProfile(user?.email);
      setCompany(comp);

      // 2. If company profile exists, calculate verified organization metrics
      if (comp?.companyId) {
        const stats = await fetchCompanyMetrics(comp.companyId);
        setMetrics(stats);

        // 3. Fetch applications across company jobs for pipeline metrics
        try {
          const jobsRes = await fetchCompanyJobs(comp.companyId, { size: 100 });
          const jobs = jobsRes?.content || [];
          const apps = await fetchCompanyPipelineApplications(jobs);
          setPipelineMetrics(calculatePipelineMetrics(apps));
        } catch (appErr) {
          console.warn('Failed to calculate pipeline metrics:', appErr);
        }
      }
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to load company recruiter dashboard.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, [user?.email]);

  if (loading) {
    return <LoadingState message="Loading organization recruitment dashboard..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadDashboardData} />;
  }

  if (!company) {
    return (
      <EmptyState
        title="Company Profile Required"
        message={`Logged in as ${user?.email}. Your recruiter account is not yet associated with a corporate profile in the institutional database.`}
        icon={<Building2 size={40} />}
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
        title={company.name}
        subtitle={`Recruiter: ${user?.email} • Org ID #${company.companyId} • Campus Recruitment Portal`}
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <Link
              to="/company/applicants"
              className="btn btn-primary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Users size={16} /> Review Applicants &rarr;
            </Link>
            <Link
              to="/company/jobs"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Briefcase size={16} /> Manage Jobs
            </Link>
            <Link
              to="/company/interviews"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <CalendarDays size={16} /> Interviews
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={loadDashboardData}
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <RefreshCw size={14} /> Refresh
                </span>
              }
            />
          </div>
        }
      />

      {/* Real Company-Specific Job Openings KPI Cards */}
      <div className="stat-grid" style={{ marginBottom: '1.25rem' }}>
        <Link to="/company/jobs" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Total Drives Posted"
            value={metrics.totalJobs}
            context="Lifetime openings for your company"
            icon={<Briefcase size={22} />}
          />
        </Link>
        <Link to="/company/jobs" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Active Recruitment Drives"
            value={metrics.activeJobs}
            context="Currently accepting student applications"
            icon={<CheckCircle2 size={22} />}
          />
        </Link>
        <Link to="/company/jobs" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Concluding Soon"
            value={metrics.nearingDeadline}
            context="Deadline within the next 7 days"
            icon={<Clock size={22} />}
          />
        </Link>
        <Link to="/company/profile" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Organization Status"
            value={company.status || 'ACTIVE'}
            context="Institutional recruitment authorization"
            icon={<ShieldCheck size={22} />}
          />
        </Link>
      </div>

      {/* Candidate Recruitment Pipeline Overview */}
      <div style={{ marginBottom: '1.75rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
          <h2 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--secondary-color)', margin: 0, display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <Users size={18} /> Candidate Recruitment Pipeline
          </h2>
          <Link
            to="/company/applicants"
            style={{ fontSize: '0.8125rem', color: 'var(--primary-color)', fontWeight: 600, textDecoration: 'none' }}
          >
            View All Applicants &rarr;
          </Link>
        </div>
        <div className="stat-grid">
          <Link to="/company/applicants" style={{ textDecoration: 'none', color: 'inherit' }}>
            <StatCard
              label="Total Applicants"
              value={pipelineMetrics.total}
              context="Candidates across all openings"
              icon={<Users size={22} />}
            />
          </Link>
          <Link to="/company/applicants" style={{ textDecoration: 'none', color: 'inherit' }}>
            <StatCard
              label="Pending Screening"
              value={pipelineMetrics.applied}
              context="Awaiting initial shortlist"
              icon={<Inbox size={22} />}
            />
          </Link>
          <Link to="/company/applicants" style={{ textDecoration: 'none', color: 'inherit' }}>
            <StatCard
              label="Shortlisted / In Rounds"
              value={pipelineMetrics.shortlisted + pipelineMetrics.interviewing}
              context="Qualified for evaluation"
              icon={<CalendarDays size={22} />}
            />
          </Link>
          <Link to="/company/applicants" style={{ textDecoration: 'none', color: 'inherit' }}>
            <StatCard
              label="Offers / Selected"
              value={pipelineMetrics.selected}
              context="Hired candidates"
              icon={<Award size={22} />}
            />
          </Link>
        </div>
      </div>

      {/* Quick Navigation Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginBottom: '1.75rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
              <Users size={20} />
            </span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                Candidate Pipeline
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Review applicants & resumes</div>
            </div>
          </div>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
            Inspect student submissions, download resume links, advance shortlisted candidates, and issue final offers.
          </p>
          <Link to="/company/applicants" className="btn btn-primary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
            Manage Candidates &rarr;
          </Link>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
              <Building2 size={20} />
            </span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                Company Profile
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Corporate information & presence</div>
            </div>
          </div>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
            Manage official website, recruiter contact person, phone number, and physical headquarters location.
          </p>
          <Link to="/company/profile" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
            Edit Corporate Profile &rarr;
          </Link>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
              <Briefcase size={20} />
            </span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                Recruitment Openings
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Publish & manage placement drives</div>
            </div>
          </div>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
            Create new job postings, configure minimum CGPA criteria, specify salary packages, and update application deadlines.
          </p>
          <Link to="/company/jobs" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
            Open Job Console &rarr;
          </Link>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
              <CalendarDays size={20} />
            </span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                Interview Hub
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Multi-round candidate evaluation</div>
            </div>
          </div>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
            Schedule technical rounds, assign interviewers, record PASSED/FAILED decisions, and provide evaluation feedback.
          </p>
          <Link to="/company/interviews" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
            Manage Interviews &rarr;
          </Link>
        </div>
      </div>

      {/* Recent Drives Table */}
      <div className="data-table-card">
        <div className="data-table-header">
          <div>
            <h2 className="data-table-title">Your Published Recruitment Openings</h2>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Showing {metrics.recentJobs.length} of {metrics.totalJobs} total opening(s)
            </span>
          </div>
          <Link
            to="/company/jobs"
            className="btn btn-secondary"
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', textDecoration: 'none' }}
          >
            All Openings &rarr;
          </Link>
        </div>

        {metrics.recentJobs.length > 0 ? (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Job ID</th>
                  <th>Job Title</th>
                  <th>Type</th>
                  <th>Min CGPA</th>
                  <th>Package (CTC)</th>
                  <th>Deadline</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {metrics.recentJobs.map((job) => (
                  <tr key={job.jobId}>
                    <td><code>#{job.jobId}</code></td>
                    <td>
                      <strong>{job.title}</strong>
                      {job.requiredSkills && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {job.requiredSkills}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="badge badge-info" style={{ fontSize: '0.6875rem' }}>
                        {job.jobType || 'FULL_TIME'}
                      </span>
                    </td>
                    <td>{job.minCgpa != null ? job.minCgpa : 'Open'}</td>
                    <td>
                      {job.packageAmount != null
                        ? `₹ ${Number(job.packageAmount).toLocaleString()}`
                        : 'Negotiable'}
                    </td>
                    <td>{job.deadline || 'No Deadline'}</td>
                    <td>
                      <span className={`badge ${job.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                        {job.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <p style={{ margin: '0 0 1rem', fontSize: '0.875rem' }}>
              No campus placement openings published yet for {company.name}.
            </p>
            <Link to="/company/jobs" className="btn btn-primary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
              <Plus size={16} /> Create Your First Job Opening
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default CompanyDashboardPage;
