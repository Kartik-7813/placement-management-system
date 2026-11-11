import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  GraduationCap,
  Building2,
  Briefcase,
  FileText,
  Award,
  CalendarDays,
  RefreshCw,
} from 'lucide-react';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import Button from '../components/Button';

/**
 * AdminDashboardPage Component
 * Displays institutional recruitment metrics and pipeline breakdowns
 * fetched directly from the verified GET /api/dashboard/summary endpoint.
 */
function AdminDashboardPage() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get(API_ENDPOINTS.DASHBOARD.SUMMARY);
      setSummary(response.data);
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve administrator metrics.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, []);

  if (loading) {
    return <LoadingState message="Fetching campus placement summary statistics..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchSummary} />;
  }

  return (
    <div>
      <PageHeader
        title="Placement Analytics & Administration"
        subtitle="Live aggregated statistics across campus placement drives, recruiters, and candidates"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <Link
              to="/admin/students"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <GraduationCap size={15} /> Students
            </Link>
            <Link
              to="/admin/companies"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Building2 size={15} /> Companies
            </Link>
            <Link
              to="/admin/jobs"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Briefcase size={15} /> Jobs
            </Link>
            <Link
              to="/admin/applications"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <FileText size={15} /> Applications
            </Link>
            <Link
              to="/admin/placements"
              className="btn btn-secondary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Award size={15} /> Placements
            </Link>
            <Link
              to="/admin/interviews"
              className="btn btn-primary"
              style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <CalendarDays size={15} /> Interviews &rarr;
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={fetchSummary}
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <RefreshCw size={14} /> Refresh
                </span>
              }
            />
          </div>
        }
      />

      {/* Primary KPI Cards (Clickable Direct Links) */}
      <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
        <Link to="/admin/students" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Total Registered Students"
            value={summary?.totalStudents}
            context={`${summary?.activeStudents ?? 0} active, ${summary?.inactiveStudents ?? 0} inactive`}
            icon={<GraduationCap size={22} />}
          />
        </Link>
        <Link to="/admin/companies" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Recruiting Companies"
            value={summary?.totalCompanies}
            context={`${summary?.activeCompanies ?? 0} active organizations`}
            icon={<Building2 size={22} />}
          />
        </Link>
        <Link to="/admin/jobs" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Placement Jobs Posted"
            value={summary?.totalJobs}
            context={`${summary?.activeJobs ?? 0} active drives, ${summary?.closedJobs ?? 0} closed`}
            icon={<Briefcase size={22} />}
          />
        </Link>
        <Link to="/admin/applications" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Student Applications"
            value={summary?.totalApplications}
            context={`${summary?.selectedApplications ?? 0} selected, ${summary?.shortlistedApplications ?? 0} shortlisted`}
            icon={<FileText size={22} />}
          />
        </Link>
        <Link to="/admin/placements" style={{ textDecoration: 'none', color: 'inherit' }}>
          <StatCard
            label="Confirmed Placements"
            value={summary?.totalPlacements}
            context={`${summary?.confirmedPlacements ?? 0} confirmed, ${summary?.joinedPlacements ?? 0} joined`}
            icon={<Award size={22} />}
          />
        </Link>
      </div>

      {/* Institutional Management Modules */}
      <div style={{ marginBottom: '1.75rem' }}>
        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.875rem' }}>
          Institutional Administration Consoles
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
                <GraduationCap size={20} />
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                  Student Records
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Profiles, CGPA & Skills</div>
              </div>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
              Verify student academic records, search candidates by name, filter by department, review skill inventories, and manage account statuses.
            </p>
            <Link to="/admin/students" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
              Open Student Directory &rarr;
            </Link>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
                <Building2 size={20} />
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                  Corporate Registry
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Recruiting Partners & Contacts</div>
              </div>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
              Register new recruiting partners, maintain corporate contact records, activate partnerships, and manage organization authorizations.
            </p>
            <Link to="/admin/companies" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
              Manage Companies &rarr;
            </Link>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
                <Briefcase size={20} />
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                  Job Openings
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Campus Drives & Criteria</div>
              </div>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
              Monitor cross-company job postings, inspect salary packages, check CGPA eligibility thresholds, and close or re-open placement drives.
            </p>
            <Link to="/admin/jobs" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
              Inspect Job Openings &rarr;
            </Link>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
                <FileText size={20} />
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                  Applications Pipeline
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Submissions & Hiring Decisions</div>
              </div>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
              Track candidate applications platform-wide, review student resumes and statements, verify interview outcomes, and advance hiring decisions.
            </p>
            <Link to="/admin/applications" className="btn btn-secondary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
              Review Applications &rarr;
            </Link>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <span style={{ display: 'inline-flex', padding: '0.5rem', borderRadius: 'var(--radius)', background: 'var(--primary-bg)', color: 'var(--primary-color)' }}>
                <Award size={20} />
              </span>
              <div>
                <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                  Placement Records
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Offers & Joining Verification</div>
              </div>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: '0 0 1rem' }}>
              Record confirmed job offers for selected candidates, manage onboarding statuses, verify compensation packages, and track joined students.
            </p>
            <Link to="/admin/placements" className="btn btn-primary btn-block" style={{ textAlign: 'center', textDecoration: 'none', fontSize: '0.8125rem' }}>
              Manage Placements &rarr;
            </Link>
          </div>
        </div>
      </div>

      {/* Application Workflow Pipeline Breakdown */}
      <div className="data-table-card">
        <div className="data-table-header">
          <h2 className="data-table-title">Application Pipeline Breakdown</h2>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Real-time status counts
          </span>
        </div>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Workflow Status</th>
                <th>Count</th>
                <th>Pipeline Phase</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <span className="badge-status badge-applied">APPLIED</span>
                </td>
                <td><strong>{summary?.appliedApplications ?? 0}</strong></td>
                <td>Initial Screening</td>
                <td>Candidate applications submitted awaiting company review</td>
              </tr>
              <tr>
                <td>
                  <span className="badge-status badge-shortlisted">SHORTLISTED</span>
                </td>
                <td><strong>{summary?.shortlistedApplications ?? 0}</strong></td>
                <td>Profile Cleared</td>
                <td>Candidates shortlisted by recruiters based on CGPA and skills</td>
              </tr>
              <tr>
                <td>
                  <span className="badge-status badge-interviewing">INTERVIEWING</span>
                </td>
                <td><strong>{summary?.interviewingApplications ?? 0}</strong></td>
                <td>Evaluation</td>
                <td>Candidates currently undergoing multi-round interview evaluations</td>
              </tr>
              <tr>
                <td>
                  <span className="badge-status badge-selected">SELECTED</span>
                </td>
                <td><strong>{summary?.selectedApplications ?? 0}</strong></td>
                <td>Offer Stage</td>
                <td>Candidates offered placement offers</td>
              </tr>
              <tr>
                <td>
                  <span className="badge-status badge-rejected">REJECTED</span>
                </td>
                <td><strong>{summary?.rejectedApplications ?? 0}</strong></td>
                <td>Concluded</td>
                <td>Applications not meeting requirements during screening or interviews</td>
              </tr>
              <tr>
                <td>
                  <span className="badge-status badge-withdrawn">WITHDRAWN</span>
                </td>
                <td><strong>{summary?.withdrawnApplications ?? 0}</strong></td>
                <td>Concluded</td>
                <td>Applications retracted prior to placement offer finalization</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Placement Status Summary */}
      <div className="data-table-card">
        <div className="data-table-header">
          <h2 className="data-table-title">Placement Confirmation Status</h2>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Institutional metrics
          </span>
        </div>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Placement Stage</th>
                <th>Total Count</th>
                <th>Significance</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><span className="badge badge-success">CONFIRMED</span></td>
                <td><strong>{summary?.confirmedPlacements ?? 0}</strong></td>
                <td>Official placement letters recorded in the institutional database</td>
              </tr>
              <tr>
                <td><span className="badge badge-info">JOINED</span></td>
                <td><strong>{summary?.joinedPlacements ?? 0}</strong></td>
                <td>Graduates who have successfully reported to company workplace</td>
              </tr>
              <tr>
                <td><span className="badge badge-warning">CANCELLED</span></td>
                <td><strong>{summary?.cancelledPlacements ?? 0}</strong></td>
                <td>Revoked or declined placement offers</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboardPage;
