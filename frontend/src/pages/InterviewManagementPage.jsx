import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  CalendarDays,
  Calendar,
  CheckCircle2,
  Clock,
  X,
  Search,
  RefreshCw,
  Plus,
  Eye,
  Pencil,
  Trash2,
  FileText,
  User,
  Building2,
  Briefcase,
  Hash,
  Tag,
  Award,
  AlertTriangle,
  AlertCircle,
  Check,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import {
  fetchAllInterviews,
  fetchInterviewById,
  scheduleInterview,
  updateInterview,
  submitEvaluation,
  deleteInterview,
  fetchEligibleApplications,
} from '../services/interviewService';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';
import FormInput from '../components/FormInput';
import FormAlert from '../components/FormAlert';

/**
 * InterviewManagementPage Component
 * Provides recruitment coordinators (ROLE_COMPANY) and administrators (ROLE_ADMIN)
 * with a unified console for scheduling evaluation rounds, managing interview timetables,
 * inspecting evaluator feedback, and updating interview schedules.
 */
function InterviewManagementPage() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const preSelectedAppId = searchParams.get('applicationId');

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isCompany = user?.role === 'ROLE_COMPANY';

  // Data states
  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [resultFilter, setResultFilter] = useState('');
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL' | 'UPCOMING' | 'COMPLETED' | 'PENDING'
  const [searchQuery, setSearchQuery] = useState('');

  // Eligible Applications for Scheduling Dropdown
  const [eligibleApps, setEligibleApps] = useState([]);
  const [loadingApps, setLoadingApps] = useState(false);

  // Application metadata lookup map: applicationId -> { studentName, jobTitle, companyName }
  const [appMetadataMap, setAppMetadataMap] = useState({});

  // Modals
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(Boolean(preSelectedAppId));
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedInterviewForEdit, setSelectedInterviewForEdit] = useState(null);
  const [inspectInterview, setInspectInterview] = useState(null);

  // Schedule Form State
  const [scheduleForm, setScheduleForm] = useState({
    applicationId: preSelectedAppId || '',
    roundNumber: 1,
    roundType: 'TECHNICAL',
    scheduledDate: '',
    scheduledTime: '',
    interviewer: '',
  });
  const [scheduleErrors, setScheduleErrors] = useState({});
  const [scheduleSubmitting, setScheduleSubmitting] = useState(false);
  const [scheduleErrorAlert, setScheduleErrorAlert] = useState(null);
  const [scheduleSuccessAlert, setScheduleSuccessAlert] = useState(null);

  // Edit Form State
  const [editForm, setEditForm] = useState({
    roundNumber: 1,
    roundType: 'TECHNICAL',
    scheduledDate: '',
    scheduledTime: '',
    interviewer: '',
  });
  const [editErrors, setEditErrors] = useState({});
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editErrorAlert, setEditErrorAlert] = useState(null);
  const [editSuccessAlert, setEditSuccessAlert] = useState(null);

  // Delete State
  const [deletingId, setDeletingId] = useState(null);

  // Evaluation Modal State
  const evaluateQueryId = searchParams.get('evaluateId');
  const [evalQueryHandled, setEvalQueryHandled] = useState(false);
  const [isEvalModalOpen, setIsEvalModalOpen] = useState(false);
  const [selectedInterviewForEval, setSelectedInterviewForEval] = useState(null);
  const [evalForm, setEvalForm] = useState({
    result: 'PENDING',
    feedback: '',
  });
  const [evalErrors, setEvalErrors] = useState({});
  const [evalSubmitting, setEvalSubmitting] = useState(false);
  const [evalErrorAlert, setEvalErrorAlert] = useState(null);
  const [evalSuccessAlert, setEvalSuccessAlert] = useState(null);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);

  // Load interviews and eligible applications
  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch paginated interviews
      const response = await fetchAllInterviews({
        page,
        size: pageSize,
        result: resultFilter || undefined,
        sort: 'interviewId,desc',
      });

      const items = response.content || [];
      setInterviews(items);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements ?? 0);

      // Check if URL contains evaluateId parameter to auto-open evaluation
      if (evaluateQueryId && !evalQueryHandled) {
        setEvalQueryHandled(true);
        const found = items.find((iv) => String(iv.interviewId) === String(evaluateQueryId));
        if (found) {
          handleOpenEvalModal(found);
        } else {
          try {
            const single = await fetchInterviewById(evaluateQueryId);
            if (single) {
              handleOpenEvalModal(single);
            }
          } catch (sErr) {
            console.warn('Could not load evaluateId interview:', sErr);
          }
        }
      }

      // 2. Resolve missing application metadata for these interviews
      const uniqueAppIds = [...new Set(items.map((iv) => iv.applicationId).filter(Boolean))];
      const metadataToFetch = uniqueAppIds.filter((appId) => !appMetadataMap[appId]);

      if (metadataToFetch.length > 0) {
        try {
          const appPromises = metadataToFetch.map((id) =>
            api.get(API_ENDPOINTS.APPLICATIONS.BY_ID(id)).catch(() => null)
          );
          const results = await Promise.all(appPromises);
          const newMap = { ...appMetadataMap };
          results.forEach((r) => {
            if (r?.data) {
              newMap[r.data.applicationId] = {
                studentName: r.data.studentName,
                jobTitle: r.data.jobTitle,
                companyName: r.data.companyName,
                status: r.data.status,
                jobId: r.data.jobId,
              };
            }
          });
          setAppMetadataMap(newMap);
        } catch (mErr) {
          console.warn('Could not enrich application metadata:', mErr);
        }
      }
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve interview records.'
      );
    } finally {
      setLoading(false);
    }
  };

  const loadEligibleApplications = async () => {
    setLoadingApps(true);
    try {
      const apps = await fetchEligibleApplications(user?.role, user?.email);
      setEligibleApps(apps);

      // Populate metadata map
      setAppMetadataMap((prev) => {
        const next = { ...prev };
        apps.forEach((a) => {
          next[a.applicationId] = {
            studentName: a.studentName,
            jobTitle: a.jobTitle,
            companyName: a.companyName,
            status: a.status,
            jobId: a.jobId,
          };
        });
        return next;
      });
    } catch (err) {
      console.warn('Failed to load eligible applications:', err);
    } finally {
      setLoadingApps(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [page, pageSize, resultFilter]);

  useEffect(() => {
    loadEligibleApplications();
  }, []);

  // Compute metrics
  const now = new Date();
  const totalCount = totalElements;

  const upcomingCount = interviews.filter(
    (iv) => iv.scheduledOn && new Date(iv.scheduledOn) >= now && iv.result === 'PENDING'
  ).length;

  const completedCount = interviews.filter(
    (iv) => iv.result === 'PASSED' || iv.result === 'FAILED'
  ).length;

  const pendingResultCount = interviews.filter((iv) => iv.result === 'PENDING').length;

  // Filter tabs
  const getTabFilteredInterviews = () => {
    switch (activeTab) {
      case 'UPCOMING':
        return interviews.filter(
          (iv) => iv.scheduledOn && new Date(iv.scheduledOn) >= now && iv.result === 'PENDING'
        );
      case 'COMPLETED':
        return interviews.filter(
          (iv) => iv.result === 'PASSED' || iv.result === 'FAILED'
        );
      case 'PENDING':
        return interviews.filter((iv) => iv.result === 'PENDING');
      case 'ALL':
      default:
        return interviews;
    }
  };

  // Search filter
  const displayedInterviews = getTabFilteredInterviews().filter((iv) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    const meta = appMetadataMap[iv.applicationId] || {};
    const candidateMatch = meta.studentName?.toLowerCase().includes(query);
    const jobMatch = meta.jobTitle?.toLowerCase().includes(query);
    const companyMatch = meta.companyName?.toLowerCase().includes(query);
    const interviewerMatch = iv.interviewer?.toLowerCase().includes(query);
    const roundTypeMatch = iv.roundType?.toLowerCase().includes(query);
    const idMatch = String(iv.interviewId).includes(query);
    const appMatch = String(iv.applicationId).includes(query);
    return (
      candidateMatch ||
      jobMatch ||
      companyMatch ||
      interviewerMatch ||
      roundTypeMatch ||
      idMatch ||
      appMatch
    );
  });

  // Date / Time formatting
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

  // =========================================================
  // SCHEDULE INTERVIEW ACTIONS
  // =========================================================
  const handleOpenScheduleModal = () => {
    setScheduleErrorAlert(null);
    setScheduleSuccessAlert(null);
    setScheduleErrors({});
    setIsScheduleModalOpen(true);
    loadEligibleApplications();
  };

  const handleCloseScheduleModal = () => {
    if (!scheduleSubmitting) {
      setIsScheduleModalOpen(false);
      setScheduleErrorAlert(null);
      setScheduleErrors({});
    }
  };

  const validateScheduleForm = () => {
    const errors = {};
    if (!scheduleForm.applicationId) {
      errors.applicationId = 'Application selection is required';
    }

    const roundNum = Number(scheduleForm.roundNumber);
    if (!roundNum || isNaN(roundNum) || roundNum <= 0) {
      errors.roundNumber = 'Round number must be a positive integer';
    }

    if (!scheduleForm.roundType?.trim()) {
      errors.roundType = 'Round type is required';
    } else if (scheduleForm.roundType.trim().length > 50) {
      errors.roundType = 'Round type cannot exceed 50 characters';
    }

    if (!scheduleForm.scheduledDate) {
      errors.scheduledDate = 'Scheduled date is required';
    }

    if (!scheduleForm.scheduledTime) {
      errors.scheduledTime = 'Scheduled time is required';
    }

    if (scheduleForm.scheduledDate && scheduleForm.scheduledTime) {
      const scheduledDateTime = new Date(
        `${scheduleForm.scheduledDate}T${scheduleForm.scheduledTime}:00`
      );
      if (scheduledDateTime < new Date()) {
        errors.scheduledDate = 'Interview cannot be scheduled in the past';
      }
    }

    if (scheduleForm.interviewer && scheduleForm.interviewer.length > 150) {
      errors.interviewer = 'Interviewer name cannot exceed 150 characters';
    }

    setScheduleErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    setScheduleErrorAlert(null);
    setScheduleSuccessAlert(null);

    if (!validateScheduleForm()) {
      return;
    }

    setScheduleSubmitting(true);
    try {
      const isoDateTime = `${scheduleForm.scheduledDate}T${scheduleForm.scheduledTime}:00`;

      await scheduleInterview({
        applicationId: scheduleForm.applicationId,
        roundNumber: scheduleForm.roundNumber,
        roundType: scheduleForm.roundType,
        scheduledOn: isoDateTime,
        interviewer: scheduleForm.interviewer,
        result: 'PENDING',
      });

      setScheduleSuccessAlert('Interview scheduled successfully!');
      setTimeout(() => {
        setIsScheduleModalOpen(false);
        loadData();
      }, 1000);
    } catch (err) {
      setScheduleErrorAlert(
        err.response?.data?.message ||
        err.message ||
        'Failed to schedule interview round. Please check application status and duplicate rounds.'
      );
    } finally {
      setScheduleSubmitting(false);
    }
  };

  // =========================================================
  // EDIT INTERVIEW ACTIONS
  // =========================================================
  const handleOpenEditModal = (iv) => {
    setSelectedInterviewForEdit(iv);
    setEditErrorAlert(null);
    setEditSuccessAlert(null);
    setEditErrors({});

    let datePart = '';
    let timePart = '';
    if (iv.scheduledOn) {
      const d = new Date(iv.scheduledOn);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const hours = String(d.getHours()).padStart(2, '0');
      const minutes = String(d.getMinutes()).padStart(2, '0');
      datePart = `${year}-${month}-${day}`;
      timePart = `${hours}:${minutes}`;
    }

    setEditForm({
      roundNumber: iv.roundNumber || 1,
      roundType: iv.roundType || 'TECHNICAL',
      scheduledDate: datePart,
      scheduledTime: timePart,
      interviewer: iv.interviewer || '',
    });
    setIsEditModalOpen(true);
  };

  const handleCloseEditModal = () => {
    if (!editSubmitting) {
      setIsEditModalOpen(false);
      setSelectedInterviewForEdit(null);
      setEditErrorAlert(null);
      setEditErrors({});
    }
  };

  const validateEditForm = () => {
    const errors = {};
    const roundNum = Number(editForm.roundNumber);
    if (!roundNum || isNaN(roundNum) || roundNum <= 0) {
      errors.roundNumber = 'Round number must be a positive integer';
    }

    if (!editForm.roundType?.trim()) {
      errors.roundType = 'Round type is required';
    } else if (editForm.roundType.trim().length > 50) {
      errors.roundType = 'Round type cannot exceed 50 characters';
    }

    if (!editForm.scheduledDate) {
      errors.scheduledDate = 'Scheduled date is required';
    }

    if (!editForm.scheduledTime) {
      errors.scheduledTime = 'Scheduled time is required';
    }

    if (editForm.interviewer && editForm.interviewer.length > 150) {
      errors.interviewer = 'Interviewer name cannot exceed 150 characters';
    }

    setEditErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditErrorAlert(null);
    setEditSuccessAlert(null);

    if (!validateEditForm() || !selectedInterviewForEdit) {
      return;
    }

    setEditSubmitting(true);
    try {
      const isoDateTime = `${editForm.scheduledDate}T${editForm.scheduledTime}:00`;

      await updateInterview(selectedInterviewForEdit.interviewId, {
        applicationId: selectedInterviewForEdit.applicationId,
        roundNumber: editForm.roundNumber,
        roundType: editForm.roundType,
        scheduledOn: isoDateTime,
        interviewer: editForm.interviewer,
        feedback: selectedInterviewForEdit.feedback,
        result: selectedInterviewForEdit.result,
      });

      setEditSuccessAlert('Interview schedule updated successfully!');
      setTimeout(() => {
        setIsEditModalOpen(false);
        setSelectedInterviewForEdit(null);
        loadData();
      }, 1000);
    } catch (err) {
      setEditErrorAlert(
        err.response?.data?.message ||
        err.message ||
        'Failed to update interview specifications. Duplicate round numbers are not permitted.'
      );
    } finally {
      setEditSubmitting(false);
    }
  };

  // =========================================================
  // DELETE INTERVIEW (ADMIN ONLY)
  // =========================================================
  const handleDelete = async (interviewId) => {
    if (!isAdmin) return;
    const confirmed = window.confirm(
      `Are you sure you want to permanently delete interview round #${interviewId}?`
    );
    if (!confirmed) return;

    setDeletingId(interviewId);
    try {
      await deleteInterview(interviewId);
      loadData();
    } catch (err) {
      alert(
        err.response?.data?.message || err.message || 'Failed to delete interview record.'
      );
    } finally {
      setDeletingId(null);
    }
  };

  // =========================================================
  // EVALUATION & RESULT SUBMISSION (COMPANY & ADMIN)
  // =========================================================
  const handleOpenEvalModal = (interview) => {
    setSelectedInterviewForEval(interview);
    setEvalForm({
      result: interview.result || 'PENDING',
      feedback: interview.feedback || '',
    });
    setEvalErrors({});
    setEvalErrorAlert(null);
    setEvalSuccessAlert(null);
    setShowConfirmDialog(false);
    setIsEvalModalOpen(true);
  };

  const handleCloseEvalModal = () => {
    if (evalSubmitting) return;
    setIsEvalModalOpen(false);
    setSelectedInterviewForEval(null);
    setEvalErrors({});
    setEvalErrorAlert(null);
    setEvalSuccessAlert(null);
    setShowConfirmDialog(false);
  };

  const validateEvalForm = () => {
    const errors = {};
    if (!['PENDING', 'PASSED', 'FAILED'].includes(evalForm.result)) {
      errors.result = 'Please select a valid evaluation result.';
    }
    if (evalForm.feedback && evalForm.feedback.length > 2000) {
      errors.feedback = `Feedback cannot exceed 2000 characters (currently ${evalForm.feedback.length} characters).`;
    }
    setEvalErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleEvalFormSubmit = (e) => {
    e.preventDefault();
    setEvalErrorAlert(null);
    setEvalSuccessAlert(null);

    if (!validateEvalForm()) {
      return;
    }

    // Require confirmation before submitting final PASSED or FAILED result
    if (evalForm.result === 'PASSED' || evalForm.result === 'FAILED') {
      setShowConfirmDialog(true);
    } else {
      executeEvalSubmission();
    }
  };

  const executeEvalSubmission = async () => {
    if (!selectedInterviewForEval) return;
    setEvalSubmitting(true);
    setEvalErrorAlert(null);

    try {
      const updated = await submitEvaluation(
        selectedInterviewForEval.interviewId,
        selectedInterviewForEval,
        evalForm
      );

      setEvalSuccessAlert(
        `Evaluation recorded successfully! Round #${updated.roundNumber} outcome is now marked as ${updated.result}.`
      );
      setShowConfirmDialog(false);

      // Refresh data
      await loadData();

      // Close modal after brief confirmation
      setTimeout(() => {
        handleCloseEvalModal();
      }, 1200);
    } catch (err) {
      setEvalErrorAlert(
        err.response?.data?.message ||
          err.message ||
          'Failed to record evaluation. Please check your network and authorization.'
      );
      setShowConfirmDialog(false);
    } finally {
      setEvalSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title={isAdmin ? 'Institutional Interview Administration' : 'Recruitment Interview Management'}
        subtitle={
          isAdmin
            ? 'Oversee campus-wide technical and HR interview rounds, evaluator schedules, and candidate progressions'
            : 'Schedule evaluation rounds, update interview timetables, and monitor candidate evaluations'
        }
        actions={
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Button
              type="button"
              variant="secondary"
              onClick={loadData}
              label="Refresh"
            />
            <Button
              type="button"
              variant="primary"
              onClick={handleOpenScheduleModal}
              label="+ Schedule Interview"
            />
          </div>
        }
      />

      {/* KPI Overview Cards */}
      <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
        <StatCard
          label="Total Scheduled Rounds"
          value={totalCount}
          context="System interview records"
          icon={<CalendarDays size={22} />}
        />
        <StatCard
          label="Upcoming Rounds"
          value={upcomingCount}
          context="Scheduled on current page"
          icon={<Calendar size={22} />}
        />
        <StatCard
          label="Completed Rounds"
          value={completedCount}
          context="Evaluations concluded"
          icon={<CheckCircle2 size={22} />}
        />
        <StatCard
          label="Pending Results"
          value={pendingResultCount}
          context="Awaiting evaluator decision"
          icon={<Clock size={22} />}
        />
      </div>

      {/* Filter Tabs & Search Controls */}
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
          {/* Tab Filter */}
          <div className="filter-tab-bar">
            <button
              type="button"
              className={`filter-tab ${activeTab === 'ALL' ? 'active' : ''}`}
              onClick={() => setActiveTab('ALL')}
            >
              All Rounds
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'UPCOMING' ? 'active' : ''}`}
              onClick={() => setActiveTab('UPCOMING')}
            >
              Upcoming ({upcomingCount})
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'COMPLETED' ? 'active' : ''}`}
              onClick={() => setActiveTab('COMPLETED')}
            >
              Completed ({completedCount})
            </button>
            <button
              type="button"
              className={`filter-tab ${activeTab === 'PENDING' ? 'active' : ''}`}
              onClick={() => setActiveTab('PENDING')}
            >
              Pending Result ({pendingResultCount})
            </button>
          </div>

          {/* Search & Result Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <select
              className="form-select"
              value={resultFilter}
              onChange={(e) => {
                setResultFilter(e.target.value);
                setPage(0);
              }}
              style={{ width: 'auto', padding: '0.4rem 0.75rem', fontSize: '0.875rem' }}
            >
              <option value="">All Results</option>
              <option value="PENDING">PENDING</option>
              <option value="PASSED">PASSED</option>
              <option value="FAILED">FAILED</option>
            </select>

            <input
              type="text"
              className="form-input"
              placeholder="Search candidate, job, interviewer..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: '220px', fontSize: '0.875rem' }}
            />
          </div>
        </div>
      </div>

      {/* Main Table Listing */}
      {loading ? (
        <LoadingState message="Loading interview schedule records..." />
      ) : error ? (
        <ErrorState message={error} onRetry={loadData} />
      ) : displayedInterviews.length > 0 ? (
        <div className="data-table-card">
          <div className="data-table-header">
            <div>
              <h2 className="data-table-title">Interview Timetable Records</h2>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Showing {displayedInterviews.length} of {totalElements} scheduled round(s)
              </span>
            </div>
          </div>

          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Round ID</th>
                  <th>Application</th>
                  <th>Candidate & Position</th>
                  <th>Round Specification</th>
                  <th>Scheduled Date & Time</th>
                  <th>Interviewer</th>
                  <th>Result</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {displayedInterviews.map((iv) => {
                  const meta = appMetadataMap[iv.applicationId] || {};
                  return (
                    <tr key={iv.interviewId}>
                      <td>
                        <code>#{iv.interviewId}</code>
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, color: 'var(--primary-color)' }}>
                          App #{iv.applicationId}
                        </span>
                        {meta.status && (
                          <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                            Status: {meta.status}
                          </div>
                        )}
                      </td>
                      <td>
                        <strong>{meta.studentName || `Student on App #${iv.applicationId}`}</strong>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {meta.jobTitle ? `${meta.jobTitle}` : 'Position details'}
                          {meta.companyName ? ` • ${meta.companyName}` : ''}
                        </div>
                      </td>
                      <td>
                        <span className="interview-round-pill">
                          Round #{iv.roundNumber} &bull; {iv.roundType || 'General'}
                        </span>
                      </td>
                      <td>
                        <div>{formatScheduledDate(iv.scheduledOn)}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {formatScheduledTime(iv.scheduledOn)}
                        </div>
                      </td>
                      <td>{iv.interviewer || <span style={{ color: 'var(--text-muted)' }}>TBD</span>}</td>
                      <td>{getResultBadge(iv.result)}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                          <button
                            type="button"
                            className="btn btn-primary"
                            onClick={() => handleOpenEvalModal(iv)}
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                          >
                            Evaluate
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => handleOpenEditModal(iv)}
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => setInspectInterview(iv)}
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                          >
                            Inspect
                          </button>
                          {isAdmin && (
                            <button
                              type="button"
                              className="btn btn-secondary"
                              onClick={() => handleDelete(iv.interviewId)}
                              disabled={deletingId === iv.interviewId}
                              style={{
                                padding: '0.25rem 0.55rem',
                                fontSize: '0.75rem',
                                color: 'var(--danger-color)',
                                borderColor: '#fecaca',
                              }}
                            >
                              {deletingId === iv.interviewId ? '...' : 'Delete'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div
              className="pagination-card"
              style={{
                border: 'none',
                borderTop: '1px solid var(--border-color)',
                borderRadius: 0,
              }}
            >
              <div className="pagination-info">
                Page {page + 1} of {totalPages} &bull; Total {totalElements} interview records
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
          title={resultFilter ? 'No Interviews Match Result Filter' : 'No Interviews Scheduled'}
          message={
            resultFilter
              ? `There are no interview records with result "${resultFilter}".`
              : 'No candidate interview rounds have been scheduled yet. Click "+ Schedule Interview" to set up a new round for an eligible application.'
          }
          icon={<CalendarDays size={40} />}
          action={
            <Button
              type="button"
              variant="primary"
              onClick={handleOpenScheduleModal}
              label="+ Schedule First Interview"
            />
          }
        />
      )}

      {/* =========================================================
          SCHEDULE INTERVIEW MODAL DIALOG
          ========================================================= */}
      {isScheduleModalOpen && (
        <div className="modal-backdrop" onClick={handleCloseScheduleModal}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Schedule New Interview Round</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Assign interview round, category, date, and evaluator for an eligible application
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseScheduleModal}
                disabled={scheduleSubmitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleScheduleSubmit} noValidate>
              {scheduleErrorAlert && <FormAlert type="error" message={scheduleErrorAlert} />}
              {scheduleSuccessAlert && <FormAlert type="success" message={scheduleSuccessAlert} />}

              {/* Application Selection Field */}
              <div className="form-group">
                <label className="form-label" htmlFor="appSelect">
                  Target Candidate Application <span className="form-required">*</span>
                </label>
                {loadingApps ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    Loading eligible applications (SHORTLISTED / INTERVIEWING)...
                  </div>
                ) : eligibleApps.length > 0 ? (
                  <select
                    id="appSelect"
                    className="form-select"
                    value={scheduleForm.applicationId}
                    onChange={(e) => {
                      setScheduleForm((prev) => ({ ...prev, applicationId: e.target.value }));
                      if (scheduleErrors.applicationId) {
                        setScheduleErrors((prev) => ({ ...prev, applicationId: null }));
                      }
                    }}
                  >
                    <option value="">-- Select Shortlisted or Interviewing Candidate --</option>
                    {eligibleApps.map((app) => (
                      <option key={app.applicationId} value={app.applicationId}>
                        App #{app.applicationId} — {app.studentName} ({app.jobTitle} at {app.companyName}) [{app.status}]
                      </option>
                    ))}
                  </select>
                ) : (
                  <div>
                    <input
                      type="number"
                      id="appSelect"
                      className="form-input"
                      placeholder="Enter Application ID (e.g. 1)"
                      value={scheduleForm.applicationId}
                      onChange={(e) => {
                        setScheduleForm((prev) => ({ ...prev, applicationId: e.target.value }));
                        if (scheduleErrors.applicationId) {
                          setScheduleErrors((prev) => ({ ...prev, applicationId: null }));
                        }
                      }}
                    />
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                      Notice: No applications currently in SHORTLISTED or INTERVIEWING status were found in the active dropdown. You may specify an existing application ID manually.
                    </div>
                  </div>
                )}
                {scheduleErrors.applicationId && (
                  <span style={{ fontSize: '0.75rem', color: 'var(--danger-color)', marginTop: '0.25rem' }}>
                    {scheduleErrors.applicationId}
                  </span>
                )}
              </div>

              {/* Round Number and Round Type Grid */}
              <div className="form-grid-2">
                <FormInput
                  label="Round Sequence Number"
                  name="roundNumber"
                  type="number"
                  min="1"
                  value={scheduleForm.roundNumber}
                  onChange={(e) => {
                    setScheduleForm((prev) => ({ ...prev, roundNumber: e.target.value }));
                    if (scheduleErrors.roundNumber) {
                      setScheduleErrors((prev) => ({ ...prev, roundNumber: null }));
                    }
                  }}
                  required
                  error={scheduleErrors.roundNumber}
                  hint="Positive integer (e.g., 1 for Round 1)"
                />

                <div className="form-group">
                  <label className="form-label" htmlFor="roundTypeSelect">
                    Evaluation Category <span className="form-required">*</span>
                  </label>
                  <select
                    id="roundTypeSelect"
                    className="form-select"
                    value={scheduleForm.roundType}
                    onChange={(e) => {
                      setScheduleForm((prev) => ({ ...prev, roundType: e.target.value }));
                      if (scheduleErrors.roundType) {
                        setScheduleErrors((prev) => ({ ...prev, roundType: null }));
                      }
                    }}
                  >
                    <option value="TECHNICAL">TECHNICAL (Coding / System Design)</option>
                    <option value="CODING">CODING (Hands-on Assessment)</option>
                    <option value="HR">HR (Behavioral / Cultural Fit)</option>
                    <option value="MANAGERIAL">MANAGERIAL (Leadership & Tech Fit)</option>
                    <option value="OTHER">OTHER (General Screening)</option>
                  </select>
                  {scheduleErrors.roundType && (
                    <span style={{ fontSize: '0.75rem', color: 'var(--danger-color)', marginTop: '0.25rem' }}>
                      {scheduleErrors.roundType}
                    </span>
                  )}
                </div>
              </div>

              {/* Scheduled Date and Time Grid */}
              <div className="form-grid-2">
                <FormInput
                  label="Scheduled Date"
                  name="scheduledDate"
                  type="date"
                  value={scheduleForm.scheduledDate}
                  onChange={(e) => {
                    setScheduleForm((prev) => ({ ...prev, scheduledDate: e.target.value }));
                    if (scheduleErrors.scheduledDate) {
                      setScheduleErrors((prev) => ({ ...prev, scheduledDate: null }));
                    }
                  }}
                  required
                  error={scheduleErrors.scheduledDate}
                />

                <FormInput
                  label="Scheduled Time"
                  name="scheduledTime"
                  type="time"
                  value={scheduleForm.scheduledTime}
                  onChange={(e) => {
                    setScheduleForm((prev) => ({ ...prev, scheduledTime: e.target.value }));
                    if (scheduleErrors.scheduledTime) {
                      setScheduleErrors((prev) => ({ ...prev, scheduledTime: null }));
                    }
                  }}
                  required
                  error={scheduleErrors.scheduledTime}
                />
              </div>

              {/* Interviewer Name Field */}
              <FormInput
                label="Assigned Interviewer / Panel"
                name="interviewer"
                type="text"
                placeholder="e.g. John Doe (Lead Engineer) or HR Panel"
                value={scheduleForm.interviewer}
                onChange={(e) => {
                  setScheduleForm((prev) => ({ ...prev, interviewer: e.target.value }));
                  if (scheduleErrors.interviewer) {
                    setScheduleErrors((prev) => ({ ...prev, interviewer: null }));
                  }
                }}
                maxLength={150}
                error={scheduleErrors.interviewer}
                hint="Optional interviewer name or panel designation (max 150 characters)"
              />

              {/* Modal Footer Controls */}
              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleCloseScheduleModal}
                  disabled={scheduleSubmitting}
                  label="Cancel"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={scheduleSubmitting}
                  disabled={scheduleSubmitting || Boolean(scheduleSuccessAlert)}
                  label={scheduleSubmitting ? 'Scheduling...' : 'Confirm Schedule'}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================
          EDIT SCHEDULE MODAL DIALOG
          ========================================================= */}
      {isEditModalOpen && selectedInterviewForEdit && (
        <div className="modal-backdrop" onClick={handleCloseEditModal}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Edit Interview Schedule #{selectedInterviewForEdit.interviewId}</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Update round number, category, scheduled timetable, or assigned interviewer
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseEditModal}
                disabled={editSubmitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleEditSubmit} noValidate>
              {editErrorAlert && <FormAlert type="error" message={editErrorAlert} />}
              {editSuccessAlert && <FormAlert type="success" message={editSuccessAlert} />}

              {/* Read-Only Context Information */}
              <div
                style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.875rem 1rem',
                  marginBottom: '1.25rem',
                  fontSize: '0.8125rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                  <span>
                    Application: <strong>#{selectedInterviewForEdit.applicationId}</strong>
                  </span>
                  <span>
                    Current Result: {getResultBadge(selectedInterviewForEdit.result)}
                  </span>
                </div>
                {appMetadataMap[selectedInterviewForEdit.applicationId] && (
                  <div style={{ color: 'var(--text-muted)' }}>
                    Candidate:{' '}
                    <strong style={{ color: 'var(--text-main)' }}>
                      {appMetadataMap[selectedInterviewForEdit.applicationId].studentName}
                    </strong>{' '}
                    &bull; Role:{' '}
                    <strong>{appMetadataMap[selectedInterviewForEdit.applicationId].jobTitle}</strong>
                  </div>
                )}
              </div>

              {/* Round Sequence & Category */}
              <div className="form-grid-2">
                <FormInput
                  label="Round Sequence Number"
                  name="editRoundNumber"
                  type="number"
                  min="1"
                  value={editForm.roundNumber}
                  onChange={(e) => {
                    setEditForm((prev) => ({ ...prev, roundNumber: e.target.value }));
                    if (editErrors.roundNumber) {
                      setEditErrors((prev) => ({ ...prev, roundNumber: null }));
                    }
                  }}
                  required
                  error={editErrors.roundNumber}
                />

                <div className="form-group">
                  <label className="form-label" htmlFor="editRoundTypeSelect">
                    Evaluation Category <span className="form-required">*</span>
                  </label>
                  <select
                    id="editRoundTypeSelect"
                    className="form-select"
                    value={editForm.roundType}
                    onChange={(e) => {
                      setEditForm((prev) => ({ ...prev, roundType: e.target.value }));
                      if (editErrors.roundType) {
                        setEditErrors((prev) => ({ ...prev, roundType: null }));
                      }
                    }}
                  >
                    <option value="TECHNICAL">TECHNICAL (Coding / System Design)</option>
                    <option value="CODING">CODING (Hands-on Assessment)</option>
                    <option value="HR">HR (Behavioral / Cultural Fit)</option>
                    <option value="MANAGERIAL">MANAGERIAL (Leadership & Tech Fit)</option>
                    <option value="OTHER">OTHER (General Screening)</option>
                  </select>
                  {editErrors.roundType && (
                    <span style={{ fontSize: '0.75rem', color: 'var(--danger-color)', marginTop: '0.25rem' }}>
                      {editErrors.roundType}
                    </span>
                  )}
                </div>
              </div>

              {/* Scheduled Date and Time Grid */}
              <div className="form-grid-2">
                <FormInput
                  label="Scheduled Date"
                  name="editScheduledDate"
                  type="date"
                  value={editForm.scheduledDate}
                  onChange={(e) => {
                    setEditForm((prev) => ({ ...prev, scheduledDate: e.target.value }));
                    if (editErrors.scheduledDate) {
                      setEditErrors((prev) => ({ ...prev, scheduledDate: null }));
                    }
                  }}
                  required
                  error={editErrors.scheduledDate}
                />

                <FormInput
                  label="Scheduled Time"
                  name="editScheduledTime"
                  type="time"
                  value={editForm.scheduledTime}
                  onChange={(e) => {
                    setEditForm((prev) => ({ ...prev, scheduledTime: e.target.value }));
                    if (editErrors.scheduledTime) {
                      setEditErrors((prev) => ({ ...prev, scheduledTime: null }));
                    }
                  }}
                  required
                  error={editErrors.scheduledTime}
                />
              </div>

              {/* Assigned Interviewer */}
              <FormInput
                label="Assigned Interviewer / Panel"
                name="editInterviewer"
                type="text"
                value={editForm.interviewer}
                onChange={(e) => {
                  setEditForm((prev) => ({ ...prev, interviewer: e.target.value }));
                  if (editErrors.interviewer) {
                    setEditErrors((prev) => ({ ...prev, interviewer: null }));
                  }
                }}
                maxLength={150}
                error={editErrors.interviewer}
              />

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleCloseEditModal}
                  disabled={editSubmitting}
                  label="Cancel"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={editSubmitting}
                  disabled={editSubmitting || Boolean(editSuccessAlert)}
                  label={editSubmitting ? 'Saving Changes...' : 'Save Schedule'}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================
          INSPECT DETAILS MODAL DIALOG
          ========================================================= */}
      {inspectInterview && (
        <div className="modal-backdrop" onClick={() => setInspectInterview(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '580px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Interview Round #{inspectInterview.interviewId}</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Round {inspectInterview.roundNumber} &bull; {inspectInterview.roundType || 'General Round'}
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setInspectInterview(null)}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div>
              {/* Outcome Header */}
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
                    {inspectInterview.result === 'PASSED'
                      ? 'Round Cleared (Passed)'
                      : inspectInterview.result === 'FAILED'
                        ? 'Round Not Cleared'
                        : 'Pending Evaluation'}
                  </div>
                </div>
                {getResultBadge(inspectInterview.result)}
              </div>

              {/* Specification Table */}
              <div className="job-spec-list">
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <FileText size={15} /> Application ID:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    <Link
                      to={`/student/applications/${inspectInterview.applicationId}`}
                      style={{ color: 'var(--primary-color)', fontWeight: 600 }}
                    >
                      Application #{inspectInterview.applicationId}
                    </Link>
                  </span>
                </div>
                {appMetadataMap[inspectInterview.applicationId] && (
                  <>
                    <div className="job-spec-row">
                      <span className="job-spec-row-label">
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                          <User size={15} /> Candidate Name:
                        </span>
                      </span>
                      <span className="job-spec-row-value">
                        {appMetadataMap[inspectInterview.applicationId].studentName}
                      </span>
                    </div>
                    <div className="job-spec-row">
                      <span className="job-spec-row-label">
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                          <Briefcase size={15} /> Placement Opportunity:
                        </span>
                      </span>
                      <span className="job-spec-row-value">
                        {appMetadataMap[inspectInterview.applicationId].jobTitle}
                      </span>
                    </div>
                    <div className="job-spec-row">
                      <span className="job-spec-row-label">
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                          <Building2 size={15} /> Recruiting Company:
                        </span>
                      </span>
                      <span className="job-spec-row-value">
                        {appMetadataMap[inspectInterview.applicationId].companyName}
                      </span>
                    </div>
                  </>
                )}
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Hash size={15} /> Round Sequence:
                    </span>
                  </span>
                  <span className="job-spec-row-value">Round #{inspectInterview.roundNumber}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Tag size={15} /> Evaluation Category:
                    </span>
                  </span>
                  <span className="job-spec-row-value">{inspectInterview.roundType || 'Technical / HR'}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Calendar size={15} /> Scheduled Date:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {formatScheduledDate(inspectInterview.scheduledOn)}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Clock size={15} /> Scheduled Time:
                    </span>
                  </span>
                  <span className="job-spec-row-value">
                    {formatScheduledTime(inspectInterview.scheduledOn)}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                      <User size={15} /> Assigned Interviewer:
                    </span>
                  </span>
                  <span className="job-spec-row-value">{inspectInterview.interviewer || 'TBD'}</span>
                </div>
              </div>

              {/* Feedback Block */}
              <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', marginBottom: '0.4rem' }}>
                  Evaluator Feedback & Assessment Notes
                </div>
                <div
                  style={{
                    backgroundColor: '#f8fafc',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.875rem 1rem',
                    fontSize: '0.875rem',
                    color: inspectInterview.feedback ? 'var(--text-main)' : 'var(--text-muted)',
                    fontStyle: inspectInterview.feedback ? 'normal' : 'italic',
                    lineHeight: 1.6,
                  }}
                >
                  {inspectInterview.feedback ||
                    'No formal feedback has been submitted for this round yet.'}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setInspectInterview(null)}
                label="Close"
              />
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  const target = inspectInterview;
                  setInspectInterview(null);
                  handleOpenEditModal(target);
                }}
                label="Edit Schedule &rarr;"
              />
              <Button
                type="button"
                variant="primary"
                onClick={() => {
                  const target = inspectInterview;
                  setInspectInterview(null);
                  handleOpenEvalModal(target);
                }}
                label="Evaluate Round &rarr;"
              />
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          EVALUATE INTERVIEW MODAL DIALOG
          ========================================================= */}
      {isEvalModalOpen && selectedInterviewForEval && (
        <div className="modal-backdrop" onClick={handleCloseEvalModal}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '640px' }}
          >
            <div className="modal-header">
              <div>
                <h3 className="modal-title">
                  Evaluate Interview Round #{selectedInterviewForEval.interviewId}
                </h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Round {selectedInterviewForEval.roundNumber} &bull; {selectedInterviewForEval.roundType || 'General Round'} &bull; Candidate Evaluation
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseEvalModal}
                disabled={evalSubmitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleEvalFormSubmit} noValidate>
              {evalErrorAlert && <FormAlert type="error" message={evalErrorAlert} />}
              {evalSuccessAlert && <FormAlert type="success" message={evalSuccessAlert} />}

              {/* Context Summary Box */}
              <div
                style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.875rem 1rem',
                  marginBottom: '1.25rem',
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.75rem',
                  fontSize: '0.8125rem',
                }}
              >
                <div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>Candidate</div>
                  <strong style={{ color: 'var(--secondary-color)', fontSize: '0.875rem' }}>
                    {appMetadataMap[selectedInterviewForEval.applicationId]?.studentName || `Application #${selectedInterviewForEval.applicationId}`}
                  </strong>
                </div>
                <div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>Drive Opportunity</div>
                  <strong style={{ color: 'var(--secondary-color)', fontSize: '0.875rem' }}>
                    {appMetadataMap[selectedInterviewForEval.applicationId]?.jobTitle || 'Placement Job'}
                  </strong>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {appMetadataMap[selectedInterviewForEval.applicationId]?.companyName || ''}
                  </div>
                </div>
                <div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>Scheduled Time</div>
                  <div>{formatScheduledDate(selectedInterviewForEval.scheduledOn)} at {formatScheduledTime(selectedInterviewForEval.scheduledOn)}</div>
                </div>
                <div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>Assigned Interviewer</div>
                  <div>{selectedInterviewForEval.interviewer || 'TBD'}</div>
                </div>
              </div>

              {/* Confirmation Step if PASSED or FAILED */}
              {showConfirmDialog ? (
                <div
                  style={{
                    backgroundColor: evalForm.result === 'PASSED' ? '#ecfdf5' : '#fff1f2',
                    border: `1px solid ${evalForm.result === 'PASSED' ? '#a7f3d0' : '#fecdd3'}`,
                    borderRadius: 'var(--radius-sm)',
                    padding: '1.25rem',
                    marginBottom: '1.25rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
                    <span style={{ display: 'inline-flex', marginTop: '0.125rem' }}>
                      {evalForm.result === 'PASSED' ? (
                        <Award size={28} color="var(--success-color)" />
                      ) : (
                        <AlertTriangle size={28} color="var(--danger-color)" />
                      )}
                    </span>
                    <div style={{ flex: 1 }}>
                      <h4
                        style={{
                          margin: '0 0 0.4rem',
                          color: evalForm.result === 'PASSED' ? '#065f46' : '#9f1239',
                          fontSize: '1rem',
                          fontWeight: 700,
                        }}
                      >
                        Confirm {evalForm.result} Outcome Decision
                      </h4>
                      <p
                        style={{
                          margin: '0 0 0.85rem',
                          fontSize: '0.875rem',
                          color: evalForm.result === 'PASSED' ? '#047857' : '#be123c',
                          lineHeight: 1.5,
                        }}
                      >
                        You are about to record the outcome of <strong>Round #{selectedInterviewForEval.roundNumber}</strong> as{' '}
                        <strong>{evalForm.result}</strong>.
                        {evalForm.result === 'PASSED'
                          ? ' Marking this round as PASSED confirms the candidate has satisfied the round criteria. You will be able to schedule subsequent rounds or proceed with recruitment progression.'
                          : ' Marking this round as FAILED records that the candidate did not clear this evaluation round.'}
                        {' '}This decision and your evaluation notes will be permanently logged in audit logs and will be visible on the student portal.
                      </p>

                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <Button
                          type="button"
                          variant="primary"
                          loading={evalSubmitting}
                          disabled={evalSubmitting}
                          onClick={executeEvalSubmission}
                          label={evalSubmitting ? 'Recording Decision...' : `Yes, Record Decision as ${evalForm.result}`}
                          style={{
                            backgroundColor:
                              evalForm.result === 'PASSED'
                                ? 'var(--success-color)'
                                : 'var(--danger-color)',
                            borderColor:
                              evalForm.result === 'PASSED'
                                ? 'var(--success-color)'
                                : 'var(--danger-color)',
                          }}
                        />
                        <Button
                          type="button"
                          variant="secondary"
                          disabled={evalSubmitting}
                          onClick={() => setShowConfirmDialog(false)}
                          label="Back to Form"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <>
                  {/* Evaluation Result Selector */}
                  <div className="form-group">
                    <label className="form-label">
                      Evaluation Outcome Decision <span className="form-required">*</span>
                    </label>

                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
                        gap: '0.75rem',
                      }}
                    >
                      {/* PENDING Option */}
                      <label
                        style={{
                          display: 'block',
                          border: `2px solid ${evalForm.result === 'PENDING' ? 'var(--primary-color)' : 'var(--border-color)'}`,
                          borderRadius: 'var(--radius-sm)',
                          padding: '0.875rem',
                          backgroundColor: evalForm.result === 'PENDING' ? '#f0f9ff' : '#ffffff',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        <input
                          type="radio"
                          name="evalResult"
                          value="PENDING"
                          checked={evalForm.result === 'PENDING'}
                          onChange={(e) => setEvalForm((prev) => ({ ...prev, result: e.target.value }))}
                          style={{ display: 'none' }}
                        />
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.875rem', color: '#64748b', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                            <Clock size={14} /> PENDING
                          </span>
                          {evalForm.result === 'PENDING' && (
                            <span style={{ color: 'var(--primary-color)', fontWeight: 700 }}>●</span>
                          )}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                          Awaiting assessment or panel review.
                        </div>
                      </label>

                      {/* PASSED Option */}
                      <label
                        style={{
                          display: 'block',
                          border: `2px solid ${evalForm.result === 'PASSED' ? 'var(--success-color)' : 'var(--border-color)'}`,
                          borderRadius: 'var(--radius-sm)',
                          padding: '0.875rem',
                          backgroundColor: evalForm.result === 'PASSED' ? '#ecfdf5' : '#ffffff',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        <input
                          type="radio"
                          name="evalResult"
                          value="PASSED"
                          checked={evalForm.result === 'PASSED'}
                          onChange={(e) => setEvalForm((prev) => ({ ...prev, result: e.target.value }))}
                          style={{ display: 'none' }}
                        />
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--success-color)', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                            <CheckCircle2 size={14} /> PASSED
                          </span>
                          {evalForm.result === 'PASSED' && (
                            <span style={{ color: 'var(--success-color)', fontWeight: 700 }}>●</span>
                          )}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                          Candidate cleared evaluation criteria.
                        </div>
                      </label>

                      {/* FAILED Option */}
                      <label
                        style={{
                          display: 'block',
                          border: `2px solid ${evalForm.result === 'FAILED' ? 'var(--danger-color)' : 'var(--border-color)'}`,
                          borderRadius: 'var(--radius-sm)',
                          padding: '0.875rem',
                          backgroundColor: evalForm.result === 'FAILED' ? '#fff1f2' : '#ffffff',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        <input
                          type="radio"
                          name="evalResult"
                          value="FAILED"
                          checked={evalForm.result === 'FAILED'}
                          onChange={(e) => setEvalForm((prev) => ({ ...prev, result: e.target.value }))}
                          style={{ display: 'none' }}
                        />
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--danger-color)', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                            <X size={14} /> FAILED
                          </span>
                          {evalForm.result === 'FAILED' && (
                            <span style={{ color: 'var(--danger-color)', fontWeight: 700 }}>●</span>
                          )}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                          Candidate did not clear round criteria.
                        </div>
                      </label>
                    </div>

                    {evalErrors.result && (
                      <div className="form-error" style={{ marginTop: '0.35rem' }}>
                        {evalErrors.result}
                      </div>
                    )}
                  </div>

                  {/* Feedback Textarea Field */}
                  <div className="form-group">
                    <label className="form-label" htmlFor="evalFeedback">
                      Interviewer Feedback & Assessment Notes
                    </label>
                    <textarea
                      id="evalFeedback"
                      rows={5}
                      className={`form-input ${evalErrors.feedback ? 'input-error' : ''}`}
                      placeholder="Enter detailed evaluation feedback, candidate strengths, technical assessment notes, topics covered, or reasons for decision..."
                      value={evalForm.feedback}
                      onChange={(e) => {
                        setEvalForm((prev) => ({ ...prev, feedback: e.target.value }));
                        if (evalErrors.feedback) {
                          setEvalErrors((prev) => ({ ...prev, feedback: null }));
                        }
                      }}
                      maxLength={2000}
                      style={{ resize: 'vertical', lineHeight: 1.5, fontFamily: 'inherit' }}
                    />
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        marginTop: '0.35rem',
                        fontSize: '0.75rem',
                        color:
                          evalForm.feedback?.length > 1900
                            ? 'var(--danger-color)'
                            : 'var(--text-muted)',
                      }}
                    >
                      <span>
                        Feedback notes will be displayed to the student candidate on their placement portal.
                      </span>
                      <span>{evalForm.feedback?.length || 0} / 2000</span>
                    </div>
                    {evalErrors.feedback && (
                      <div className="form-error" style={{ marginTop: '0.25rem' }}>
                        {evalErrors.feedback}
                      </div>
                    )}
                  </div>

                  <div className="modal-footer">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={handleCloseEvalModal}
                      disabled={evalSubmitting}
                      label="Cancel"
                    />
                    <Button
                      type="submit"
                      variant="primary"
                      loading={evalSubmitting}
                      disabled={evalSubmitting || Boolean(evalSuccessAlert)}
                      label={
                        evalSubmitting
                          ? 'Submitting...'
                          : evalForm.result === 'PASSED' || evalForm.result === 'FAILED'
                          ? `Continue to Confirm ${evalForm.result} &rarr;`
                          : 'Save Evaluation'
                      }
                    />
                  </div>
                </>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default InterviewManagementPage;
