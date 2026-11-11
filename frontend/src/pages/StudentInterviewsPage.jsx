import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  CalendarDays,
  Calendar,
  CheckCircle2,
  Clock,
  User,
  FileText,
  MessageSquare,
  Building2,
  Briefcase,
  Hash,
  Tag,
  X,
} from 'lucide-react';
import { fetchStudentInterviews } from '../services/interviewService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * StudentInterviewsPage Component
 * Provides authenticated students with a centralized hub for tracking scheduled
 * technical and HR interview rounds, evaluator feedback, upcoming schedules, and round results.
 */
function StudentInterviewsPage() {
  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active filter tab: 'ALL' | 'UPCOMING' | 'COMPLETED' | 'PENDING'
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Selected interview for details modal
  const [selectedInterview, setSelectedInterview] = useState(null);

  const loadInterviews = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchStudentInterviews();
      setInterviews(data.interviews || []);
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve your scheduled interviews.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInterviews();
  }, []);

  // Compute KPI metrics
  const now = new Date();
  const totalCount = interviews.length;

  const upcomingInterviews = interviews.filter((iv) => {
    if (!iv.scheduledOn) return false;
    return new Date(iv.scheduledOn) >= now && iv.result === 'PENDING';
  });

  const completedInterviews = interviews.filter((iv) => {
    return iv.result === 'PASSED' || iv.result === 'FAILED';
  });

  const pendingResultInterviews = interviews.filter((iv) => iv.result === 'PENDING');

  // Filter interviews according to active tab
  const getTabFilteredInterviews = () => {
    switch (activeTab) {
      case 'UPCOMING':
        return upcomingInterviews;
      case 'COMPLETED':
        return completedInterviews;
      case 'PENDING':
        return pendingResultInterviews;
      case 'ALL':
      default:
        return interviews;
    }
  };

  // Text search filter
  const displayedInterviews = getTabFilteredInterviews().filter((iv) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    const titleMatch = iv.jobTitle?.toLowerCase().includes(query);
    const companyMatch = iv.companyName?.toLowerCase().includes(query);
    const interviewerMatch = iv.interviewer?.toLowerCase().includes(query);
    const roundMatch = iv.roundType?.toLowerCase().includes(query);
    return titleMatch || companyMatch || interviewerMatch || roundMatch;
  });

  const formatScheduledDate = (isoString) => {
    if (!isoString) return 'Pending Schedule';
    const d = new Date(isoString);
    return d.toLocaleDateString(undefined, {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatScheduledTime = (isoString) => {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return d.toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getResultBadge = (result) => {
    switch (result) {
      case 'PASSED':
        return <span className="badge badge-result-passed">PASSED</span>;
      case 'FAILED':
        return <span className="badge badge-result-failed">FAILED</span>;
      case 'PENDING':
      default:
        return <span className="badge badge-result-pending">PENDING</span>;
    }
  };

  return (
    <div>
      <PageHeader
        title="My Interview Evaluations"
        subtitle="Track technical, HR, and managerial interview schedules, interviewer feedback, and round progressions"
        actions={
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Button
              type="button"
              variant="secondary"
              onClick={loadInterviews}
              label="Refresh"
            />
            <Link to="/student/applications" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
              My Applications &rarr;
            </Link>
          </div>
        }
      />

      {/* KPI Overview Cards */}
      <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
        <StatCard
          label="Total Interviews"
          value={totalCount}
          context="Scheduled across all drives"
          icon={<CalendarDays size={18} />}
        />
        <StatCard
          label="Upcoming Rounds"
          value={upcomingInterviews.length}
          context="Awaiting evaluation date"
          icon={<Calendar size={18} />}
        />
        <StatCard
          label="Completed Rounds"
          value={completedInterviews.length}
          context="Evaluations concluded"
          icon={<CheckCircle2 size={18} />}
        />
        <StatCard
          label="Pending Results"
          value={pendingResultInterviews.length}
          context="Decision in progress"
          icon={<Clock size={18} />}
        />
      </div>

      {/* Filter Tabs and Search Bar */}
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
          {/* Filter Tab Buttons */}
          <div className="filter-tab-bar">
            <button
              type="button"
              className={`filter-tab ${activeTab === 'ALL' ? 'active' : ''}`}
              onClick={() => setActiveTab('ALL')}
            >
              All Interviews ({totalCount})
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'UPCOMING' ? 'active' : ''}`}
              onClick={() => setActiveTab('UPCOMING')}
            >
              Upcoming ({upcomingInterviews.length})
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'COMPLETED' ? 'active' : ''}`}
              onClick={() => setActiveTab('COMPLETED')}
            >
              Completed ({completedInterviews.length})
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'PENDING' ? 'active' : ''}`}
              onClick={() => setActiveTab('PENDING')}
            >
              Pending Result ({pendingResultInterviews.length})
            </button>
          </div>

          {/* Search Input */}
          <div style={{ flex: '1 1 240px', maxWidth: '340px' }}>
            <input
              type="text"
              className="form-input"
              placeholder="Search by job, company, or interviewer..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ fontSize: '0.875rem' }}
            />
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      {loading ? (
        <LoadingState message="Loading your interview schedules and evaluation records..." />
      ) : error ? (
        <ErrorState message={error} onRetry={loadInterviews} />
      ) : displayedInterviews.length > 0 ? (
        <div className="interview-grid">
          {displayedInterviews.map((iv) => {
            const isUpcoming =
              iv.scheduledOn && new Date(iv.scheduledOn) >= now && iv.result === 'PENDING';

            return (
              <div key={iv.interviewId} className="interview-card">
                <div>
                  <div className="interview-card-header">
                    <div>
                      <div className="interview-card-company">{iv.companyName}</div>
                      <h3 className="interview-card-title">{iv.jobTitle}</h3>
                    </div>
                    {getResultBadge(iv.result)}
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <span className="interview-round-pill">
                      Round {iv.roundNumber} &bull; {iv.roundType || 'General'}
                    </span>
                    {isUpcoming ? (
                      <span className="badge badge-info" style={{ fontSize: '0.6875rem' }}>
                        UPCOMING
                      </span>
                    ) : iv.result === 'PENDING' ? (
                      <span className="badge badge-warning" style={{ fontSize: '0.6875rem' }}>
                        EVALUATION IN PROGRESS
                      </span>
                    ) : null}
                  </div>

                  <div className="interview-meta-list">
                    <div className="interview-meta-item">
                      <span className="interview-meta-label">
                        <Calendar size={13} className="inline-icon" />
                        Date:
                      </span>
                      <span className="interview-meta-val">
                        {formatScheduledDate(iv.scheduledOn)}
                      </span>
                    </div>
                    <div className="interview-meta-item">
                      <span className="interview-meta-label">
                        <Clock size={13} className="inline-icon" />
                        Time:
                      </span>
                      <span className="interview-meta-val">
                        {formatScheduledTime(iv.scheduledOn)}
                      </span>
                    </div>
                    <div className="interview-meta-item">
                      <span className="interview-meta-label">
                        <User size={13} className="inline-icon" />
                        Interviewer:
                      </span>
                      <span className="interview-meta-val">
                        {iv.interviewer || 'To be announced'}
                      </span>
                    </div>
                    <div className="interview-meta-item">
                      <span className="interview-meta-label">
                        <FileText size={13} className="inline-icon" />
                        Application:
                      </span>
                      <span className="interview-meta-val">
                        <Link
                          to={`/student/applications/${iv.applicationId}`}
                          style={{ color: 'var(--primary-color)', fontWeight: 600 }}
                        >
                          #{iv.applicationId}
                        </Link>
                      </span>
                    </div>
                  </div>

                  {/* Feedback snippet if available */}
                  {iv.feedback && (
                    <div className="interview-feedback-box">
                      <div className="interview-feedback-label" style={{ display: 'flex', alignItems: 'center' }}>
                        <MessageSquare size={13} className="inline-icon" />
                        <span>Evaluator Feedback:</span>
                      </div>
                      <div style={{ fontStyle: 'italic' }}>
                        "{iv.feedback}"
                      </div>
                    </div>
                  )}
                </div>

                <div className="interview-card-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setSelectedInterview(iv)}
                    style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                  >
                    Inspect Details &rarr;
                  </button>
                  <Link
                    to={`/student/applications/${iv.applicationId}`}
                    className="btn btn-secondary"
                    style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', textDecoration: 'none' }}
                  >
                    View Application
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <EmptyState
          title={
            searchQuery || activeTab !== 'ALL'
              ? 'No Matching Interviews'
              : 'No Interviews Scheduled'
          }
          message={
            searchQuery || activeTab !== 'ALL'
              ? 'No interview records match your active search or filter selection.'
              : 'You do not have any scheduled interview rounds yet. Once a visiting company shortlists your application, scheduled technical and HR rounds will appear here.'
          }
          icon={<CalendarDays size={40} strokeWidth={1.5} />}
          action={
            searchQuery || activeTab !== 'ALL' ? (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setActiveTab('ALL');
                  setSearchQuery('');
                }}
                style={{ marginTop: '0.75rem' }}
              >
                Clear Search & Filters
              </button>
            ) : (
              <Link
                to="/student/applications"
                className="btn btn-primary"
                style={{ marginTop: '0.75rem', textDecoration: 'none' }}
              >
                Check My Applications &rarr;
              </Link>
            )
          }
        />
      )}

      {/* Inspect Interview Details Modal */}
      {selectedInterview && (
        <div
          className="modal-backdrop"
          onClick={() => setSelectedInterview(null)}
        >
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '580px' }}
          >
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Interview Round Specifications</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Round {selectedInterview.roundNumber} &bull; {selectedInterview.roundType || 'General Round'}
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setSelectedInterview(null)}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <div>
              {/* Status Header */}
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '0.875rem 1rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: '1.25rem',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Evaluation Outcome
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--secondary-color)' }}>
                    {selectedInterview.result === 'PASSED'
                      ? 'Round Cleared (Passed)'
                      : selectedInterview.result === 'FAILED'
                      ? 'Round Not Cleared'
                      : 'Pending Evaluation'}
                  </div>
                </div>
                {getResultBadge(selectedInterview.result)}
              </div>

              {/* Specification Table */}
              <div className="job-spec-list">
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Building2 size={15} className="inline-icon" />
                    Recruiting Company:
                  </span>
                  <span className="job-spec-row-value">{selectedInterview.companyName}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Briefcase size={15} className="inline-icon" />
                    Position / Drive:
                  </span>
                  <span className="job-spec-row-value">{selectedInterview.jobTitle}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Hash size={15} className="inline-icon" />
                    Round Sequence:
                  </span>
                  <span className="job-spec-row-value">Round #{selectedInterview.roundNumber}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Tag size={15} className="inline-icon" />
                    Evaluation Category:
                  </span>
                  <span className="job-spec-row-value">{selectedInterview.roundType || 'Technical / HR'}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Calendar size={15} className="inline-icon" />
                    Scheduled Date:
                  </span>
                  <span className="job-spec-row-value">
                    {formatScheduledDate(selectedInterview.scheduledOn)}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Clock size={15} className="inline-icon" />
                    Scheduled Time:
                  </span>
                  <span className="job-spec-row-value">
                    {formatScheduledTime(selectedInterview.scheduledOn)}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <User size={15} className="inline-icon" />
                    Interviewer:
                  </span>
                  <span className="job-spec-row-value">
                    {selectedInterview.interviewer || 'Assigned Recruiter'}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <FileText size={15} className="inline-icon" />
                    Associated Application:
                  </span>
                  <span className="job-spec-row-value">
                    <Link
                      to={`/student/applications/${selectedInterview.applicationId}`}
                      style={{ color: 'var(--primary-color)', fontWeight: 600 }}
                    >
                      Application #{selectedInterview.applicationId}
                    </Link>
                  </span>
                </div>
              </div>

              {/* Feedback Block */}
              <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', marginBottom: '0.4rem' }}>
                  Interviewer Assessment & Feedback
                </div>
                <div
                  style={{
                    backgroundColor: '#f8fafc',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.875rem 1rem',
                    fontSize: '0.875rem',
                    color: selectedInterview.feedback ? 'var(--text-main)' : 'var(--text-muted)',
                    fontStyle: selectedInterview.feedback ? 'normal' : 'italic',
                    lineHeight: 1.6,
                  }}
                >
                  {selectedInterview.feedback ||
                    'No formal feedback or notes have been logged for this interview round yet.'}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setSelectedInterview(null)}
                label="Close"
              />
              <Link
                to={`/student/applications/${selectedInterview.applicationId}`}
                className="btn btn-primary"
                style={{ textDecoration: 'none' }}
              >
                Go to Application &rarr;
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default StudentInterviewsPage;
