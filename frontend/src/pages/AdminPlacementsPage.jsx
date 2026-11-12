import { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Award,
  CheckCircle2,
  Clock,
  X,
  RefreshCw,
  Plus,
  Search,
  Eye,
  Pencil,
  User,
  FileText,
  Briefcase,
  FileCheck,
  AlertTriangle,
  Check,
} from 'lucide-react';
import {
  fetchPlacements,
  createPlacement,
  updatePlacement,
  updatePlacementStatus,
} from '../services/placementService';
import { fetchDashboardSummary, fetchAllApplications } from '../services/adminService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * AdminPlacementsPage Component
 * Provides institutional administrators with comprehensive campus placement oversight:
 * - Real aggregated placement KPIs from GET /api/dashboard/summary
 * - Paginated placement records with status filtering (CONFIRMED / JOINED / CANCELLED)
 * - Safe recording of new placements for SELECTED student applications (POST /api/placements)
 * - Placement details modification (PUT /api/placements/{id})
 * - Lifecycle status transitions (CONFIRMED -> JOINED | CANCELLED) with confirmation dialogs
 * - Navigation links to associated applications and student directories
 */
function AdminPlacementsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialAppId = searchParams.get('applicationId');
  const initialStudentId = searchParams.get('studentId');
  const autoCreate = searchParams.get('create') === 'true';

  const [placements, setPlacements] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [selectedPlacement, setSelectedPlacement] = useState(null);

  // Create Placement Modal
  const [createModal, setCreateModal] = useState({
    isOpen: false,
    selectedApplications: [],
    loadingApps: false,
    formData: {
      applicationId: initialAppId ? Number(initialAppId) : '',
      studentId: initialStudentId ? Number(initialStudentId) : '',
      studentName: '',
      companyName: '',
      jobTitle: '',
      jobRole: '',
      packageAmount: '',
      joiningDate: '',
    },
    submitting: false,
    formError: null,
  });

  // Edit Placement Modal
  const [editModal, setEditModal] = useState({
    isOpen: false,
    placementId: null,
    formData: {
      studentId: '',
      studentName: '',
      applicationId: '',
      jobRole: '',
      packageAmount: '',
      joiningDate: '',
    },
    submitting: false,
    formError: null,
  });

  // Confirm Status Modal
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    placement: null,
    targetStatus: null, // 'JOINED' | 'CANCELLED'
    loading: false,
  });

  // Load placement data and dashboard summary
  const loadPlacementsData = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const placementsPromise = fetchPlacements({
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        page,
        size: pageSize,
        sort: 'placementId,desc',
      });

      const summaryPromise = fetchDashboardSummary().catch(() => null);

      const [pData, sData] = await Promise.all([placementsPromise, summaryPromise]);

      setPlacements(pData?.content || []);
      setTotalPages(pData?.totalPages || 0);
      setTotalElements(pData?.totalElements || 0);
      if (sData) setSummary(sData);
    } catch (err) {
      console.error('Failed to load placements data:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve placement records.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadPlacementsData();
  }, [page, statusFilter]);

  // Open Create Modal & load SELECTED applications
  const handleOpenCreateModal = async () => {
    setActionError(null);
    setActionSuccess(null);
    setCreateModal((prev) => ({
      ...prev,
      isOpen: true,
      loadingApps: true,
      formError: null,
    }));

    try {
      const res = await fetchAllApplications({ status: 'SELECTED', size: 100 });
      const apps = res?.content || [];

      let preselected = null;
      if (initialAppId) {
        preselected = apps.find((a) => String(a.applicationId) === String(initialAppId));
      }

      setCreateModal({
        isOpen: true,
        selectedApplications: apps,
        loadingApps: false,
        formData: {
          applicationId: preselected ? preselected.applicationId : (apps[0]?.applicationId || ''),
          studentId: preselected ? preselected.studentId : (apps[0]?.studentId || ''),
          studentName: preselected ? preselected.studentName : (apps[0]?.studentName || ''),
          companyName: preselected ? preselected.companyName : (apps[0]?.companyName || ''),
          jobTitle: preselected ? preselected.jobTitle : (apps[0]?.jobTitle || ''),
          jobRole: preselected ? preselected.jobTitle : (apps[0]?.jobTitle || ''),
          packageAmount: '',
          joiningDate: '',
        },
        submitting: false,
        formError: null,
      });
    } catch (err) {
      console.warn('Failed to load selected applications:', err);
      setCreateModal((prev) => ({
        ...prev,
        loadingApps: false,
        formError: 'Failed to load eligible SELECTED applications. You may enter IDs manually.',
      }));
    }
  };

  // If URL has ?create=true, trigger create modal automatically
  useEffect(() => {
    if (autoCreate && initialAppId) {
      handleOpenCreateModal();
    }
  }, [autoCreate, initialAppId]);

  // Handle application dropdown selection in Create Modal
  const handleApplicationSelect = (appId) => {
    const app = createModal.selectedApplications.find((a) => String(a.applicationId) === String(appId));
    if (app) {
      setCreateModal((prev) => ({
        ...prev,
        formData: {
          ...prev.formData,
          applicationId: app.applicationId,
          studentId: app.studentId,
          studentName: app.studentName,
          companyName: app.companyName,
          jobTitle: app.jobTitle,
          jobRole: app.jobTitle,
        },
      }));
    } else {
      setCreateModal((prev) => ({
        ...prev,
        formData: {
          ...prev.formData,
          applicationId: appId,
        },
      }));
    }
  };

  // Submit Create Placement
  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    const { formData } = createModal;

    if (!formData.applicationId || !formData.studentId || !formData.jobRole.trim()) {
      setCreateModal((prev) => ({
        ...prev,
        formError: 'Application, Student ID, and Job Role are required.',
      }));
      return;
    }

    setCreateModal((prev) => ({ ...prev, submitting: true, formError: null }));

    try {
      const created = await createPlacement({
        studentId: Number(formData.studentId),
        applicationId: Number(formData.applicationId),
        jobRole: formData.jobRole.trim(),
        packageAmount: formData.packageAmount !== '' ? Number(formData.packageAmount) : null,
        joiningDate: formData.joiningDate || null,
      });

      setActionSuccess(
        `Placement record #${created.placementId} for ${created.studentName} was created successfully with status CONFIRMED.`
      );
      setCreateModal((prev) => ({ ...prev, isOpen: false }));
      setSearchParams({});
      await loadPlacementsData(true);
    } catch (err) {
      console.error('Failed to create placement record:', err);
      setCreateModal((prev) => ({
        ...prev,
        submitting: false,
        formError:
          err.response?.data?.message || err.message || 'Failed to create placement record.',
      }));
    }
  };

  // Open Edit Modal
  const handleOpenEdit = (p) => {
    setActionError(null);
    setActionSuccess(null);
    setEditModal({
      isOpen: true,
      placementId: p.placementId,
      formData: {
        studentId: p.studentId,
        studentName: p.studentName,
        applicationId: p.applicationId,
        jobRole: p.jobRole || '',
        packageAmount: p.packageAmount != null ? p.packageAmount : '',
        joiningDate: p.joiningDate || '',
      },
      submitting: false,
      formError: null,
    });
  };

  // Submit Edit Placement
  const handleEditSubmit = async (e) => {
    e.preventDefault();
    const { placementId, formData } = editModal;

    if (!formData.jobRole.trim()) {
      setEditModal((prev) => ({
        ...prev,
        formError: 'Job Role is required.',
      }));
      return;
    }

    setEditModal((prev) => ({ ...prev, submitting: true, formError: null }));

    try {
      const updated = await updatePlacement(placementId, {
        studentId: formData.studentId,
        applicationId: formData.applicationId,
        jobRole: formData.jobRole.trim(),
        packageAmount: formData.packageAmount !== '' ? Number(formData.packageAmount) : null,
        joiningDate: formData.joiningDate || null,
      });

      setActionSuccess(`Placement record #${updated.placementId} updated successfully.`);
      setEditModal((prev) => ({ ...prev, isOpen: false }));
      await loadPlacementsData(true);
    } catch (err) {
      console.error('Failed to update placement record:', err);
      setEditModal((prev) => ({
        ...prev,
        submitting: false,
        formError:
          err.response?.data?.message || err.message || 'Failed to update placement record.',
      }));
    }
  };

  // Open Status Confirmation Modal
  const handleOpenConfirmStatus = (placement, targetStatus) => {
    setActionError(null);
    setActionSuccess(null);
    setConfirmModal({
      isOpen: true,
      placement,
      targetStatus,
      loading: false,
    });
  };

  // Execute Status Transition
  const handleExecuteStatusChange = async () => {
    const { placement, targetStatus } = confirmModal;
    if (!placement || !targetStatus) return;

    setConfirmModal((prev) => ({ ...prev, loading: true }));
    setActionError(null);
    setActionSuccess(null);

    try {
      const updated = await updatePlacementStatus(placement.placementId, targetStatus);
      setActionSuccess(
        `Placement #${placement.placementId} for ${placement.studentName} updated to ${targetStatus} successfully.`
      );
      setConfirmModal({ isOpen: false, placement: null, targetStatus: null, loading: false });
      await loadPlacementsData(true);
    } catch (err) {
      console.error('Failed to transition placement status:', err);
      setActionError(
        err.response?.data?.message || err.message || 'Failed to update placement status.'
      );
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  // Filtered placements by client text search
  const filteredPlacements = useMemo(() => {
    if (!searchQuery.trim()) return placements;
    const q = searchQuery.toLowerCase().trim();
    return placements.filter((p) => {
      const studentMatch = p.studentName?.toLowerCase().includes(q);
      const roleMatch = p.jobRole?.toLowerCase().includes(q);
      const idMatch = String(p.placementId).includes(q) || String(p.studentId).includes(q);
      return studentMatch || roleMatch || idMatch;
    });
  }, [placements, searchQuery]);

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return <span className="badge-status badge-selected">CONFIRMED</span>;
      case 'JOINED':
        return (
          <span
            className="badge-status"
            style={{ backgroundColor: '#dcfce7', color: '#15803d', border: '1px solid #bbf7d0', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}
          >
            <Check size={12} /> JOINED
          </span>
        );
      case 'CANCELLED':
        return <span className="badge-status badge-rejected">CANCELLED</span>;
      default:
        return <span className="badge badge-info">{status}</span>;
    }
  };

  if (loading && !refreshing) {
    return <LoadingState message="Loading institutional placement records..." />;
  }

  if (error && placements.length === 0) {
    return <ErrorState message={error} onRetry={() => loadPlacementsData()} />;
  }

  return (
    <div>
      <PageHeader
        title="Campus Placements & Offer Administration"
        subtitle="Institutional placement records, confirmed employment offers, joining confirmations, and compensation tracking"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <Button
              type="button"
              variant="primary"
              onClick={handleOpenCreateModal}
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Plus size={15} /> Record Placement
                </span>
              }
            />
            <Link to="/admin/applications?status=SELECTED" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
              Selected Candidates &rarr;
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadPlacementsData(true)}
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

      {/* Real Verified Placement KPI Cards from GET /api/dashboard/summary */}
      {summary && (
        <div className="stat-grid" style={{ marginBottom: '1.75rem' }}>
          <div onClick={() => setStatusFilter('ALL')} style={{ cursor: 'pointer' }}>
            <StatCard
              label="Total Placement Records"
              value={summary.totalPlacements}
              context="All historical campus placements"
              icon={<Award size={22} />}
            />
          </div>
          <div onClick={() => setStatusFilter('CONFIRMED')} style={{ cursor: 'pointer' }}>
            <StatCard
              label="Confirmed Offers"
              value={summary.confirmedPlacements}
              context="Offers issued awaiting joining"
              icon={<FileCheck size={22} />}
            />
          </div>
          <div onClick={() => setStatusFilter('JOINED')} style={{ cursor: 'pointer' }}>
            <StatCard
              label="Candidates Joined"
              value={summary.joinedPlacements}
              context="Candidates onboarded into organizations"
              icon={<Briefcase size={22} />}
            />
          </div>
          <div onClick={() => setStatusFilter('CANCELLED')} style={{ cursor: 'pointer' }}>
            <StatCard
              label="Cancelled / Revoked"
              value={summary.cancelledPlacements}
              context="Offers declined or revoked"
              icon={<X size={22} />}
            />
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label className="form-label" htmlFor="placementSearchInput">
              Search by Student or Role
            </label>
            <input
              id="placementSearchInput"
              type="text"
              className="form-input"
              placeholder="Search candidate name or role..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="placementStatusFilter">
              Placement Status
            </label>
            <select
              id="placementStatusFilter"
              className="form-input"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="JOINED">JOINED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          <div>
            <Button
              type="button"
              variant="secondary"
              label="Reset Filters"
              onClick={() => {
                setStatusFilter('ALL');
                setSearchQuery('');
                setPage(0);
              }}
              style={{ width: '100%' }}
            />
          </div>
        </div>
      </div>

      {/* Placements Table */}
      {filteredPlacements.length === 0 ? (
        <EmptyState
          title="No Placement Records Found"
          message={
            placements.length === 0
              ? 'No placement records exist in the system yet. Once candidate applications are marked as SELECTED, use "+ Record Placement" to record confirmed placement offers.'
              : 'No placement records matched your active search or status filter.'
          }
          icon={<Award size={40} />}
          action={
            statusFilter !== 'ALL' || searchQuery ? (
              <Button
                variant="secondary"
                label="Clear All Filters"
                onClick={() => {
                  setStatusFilter('ALL');
                  setSearchQuery('');
                }}
                style={{ marginTop: '0.75rem' }}
              />
            ) : (
              <Button
                variant="primary"
                label={
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Plus size={15} /> Record New Placement
                  </span>
                }
                onClick={handleOpenCreateModal}
                style={{ marginTop: '0.75rem' }}
              />
            )
          }
        />
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
              Showing {filteredPlacements.length} of {totalElements} placement records
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
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Placed Candidate</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Job Role</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Package Amount</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Joining Date</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Placed On</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredPlacements.map((p) => (
                  <tr key={p.placementId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{p.placementId}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      <Link to={`/admin/students`} style={{ color: 'inherit', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                        <User size={13} /> {p.studentName}
                      </Link>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                        Student #{p.studentId} &bull; App #{p.applicationId}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 500 }}>
                      {p.jobRole || '—'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: '#166534' }}>
                      {p.packageAmount != null ? `₹${p.packageAmount} LPA` : '—'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {p.joiningDate ? new Date(p.joiningDate).toLocaleDateString() : '—'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {p.placedOn ? new Date(p.placedOn).toLocaleDateString() : '—'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem' }}>
                      {renderStatusBadge(p.status)}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => {
                            setSelectedPlacement(p);
                            setDetailsModalOpen(true);
                          }}
                          title="View placement record details"
                        >
                          <Eye size={12} /> Details
                        </button>

                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => handleOpenEdit(p)}
                          title="Edit placement package, role, or joining date"
                        >
                          <Pencil size={12} /> Edit
                        </button>

                        {/* Lifecycle Status Transitions */}
                        {p.status === 'CONFIRMED' && (
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
                              onClick={() => handleOpenConfirmStatus(p, 'JOINED')}
                              title="Mark candidate as JOINED organization"
                            >
                              <Check size={12} /> Joined
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger"
                              style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                              onClick={() => handleOpenConfirmStatus(p, 'CANCELLED')}
                              title="Cancel or revoke this placement offer"
                            >
                              <X size={12} /> Cancel
                            </button>
                          </>
                        )}

                        {(p.status === 'JOINED' || p.status === 'CANCELLED') && (
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

      {/* Details Modal */}
      {detailsModalOpen && selectedPlacement && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '580px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <h3 className="modal-title" style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Award size={18} /> Placement Record #{selectedPlacement.placementId}
                </h3>
                {renderStatusBadge(selectedPlacement.status)}
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
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Candidate</span>
                  <strong style={{ fontSize: '0.9375rem', color: 'var(--secondary-color)', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <User size={15} /> {selectedPlacement.studentName}
                  </strong>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}> (Student #{selectedPlacement.studentId})</span>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Application Reference</span>
                  <strong>Application #{selectedPlacement.applicationId}</strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Placed Job Role</span>
                  <strong>{selectedPlacement.jobRole}</strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Annual Package</span>
                  <strong style={{ color: '#166534', fontSize: '1rem' }}>
                    {selectedPlacement.packageAmount != null ? `₹${selectedPlacement.packageAmount} LPA` : 'Not specified'}
                  </strong>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Tentative Joining Date</span>
                  <span>{selectedPlacement.joiningDate ? new Date(selectedPlacement.joiningDate).toLocaleDateString() : 'Not scheduled'}</span>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Offer Confirmation Timestamp</span>
                  <span>{selectedPlacement.placedOn ? new Date(selectedPlacement.placedOn).toLocaleString() : '—'}</span>
                </div>
              </div>
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <Link
                to={`/admin/applications?studentId=${selectedPlacement.studentId}`}
                className="btn btn-primary"
                style={{ fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                onClick={() => setDetailsModalOpen(false)}
              >
                <FileText size={14} /> View Candidate Application &rarr;
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

      {/* Record New Placement Modal */}
      {createModal.isOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '580px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                <Award size={18} /> Record Confirmed Placement Offer
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !createModal.submitting && setCreateModal((prev) => ({ ...prev, isOpen: false }))}
                disabled={createModal.submitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit}>
              <div style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {createModal.formError && (
                  <div className="alert alert-error" style={{ margin: 0 }}>
                    {createModal.formError}
                  </div>
                )}

                <div className="alert alert-info" style={{ margin: 0, fontSize: '0.8125rem' }}>
                  <strong>Backend Requirement:</strong> A placement record can only be created for an application that has been marked as <code>SELECTED</code>. Initial status will automatically be set to <code>CONFIRMED</code>.
                </div>

                <div>
                  <label className="form-label" htmlFor="appSelect">
                    Select Candidate Application (Status: SELECTED) *
                  </label>
                  {createModal.loadingApps ? (
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Loading selected applications...</div>
                  ) : (
                    <select
                      id="appSelect"
                      className="form-input"
                      value={createModal.formData.applicationId}
                      onChange={(e) => handleApplicationSelect(e.target.value)}
                      required
                    >
                      <option value="">-- Choose Selected Candidate Application --</option>
                      {createModal.selectedApplications.map((a) => (
                        <option key={a.applicationId} value={a.applicationId}>
                          App #{a.applicationId} &bull; {a.studentName} &bull; {a.jobTitle} ({a.companyName})
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label className="form-label" htmlFor="studentIdInput">
                      Student ID *
                    </label>
                    <input
                      id="studentIdInput"
                      type="number"
                      className="form-input"
                      value={createModal.formData.studentId}
                      readOnly
                      required
                    />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="candidateNameInput">
                      Candidate Name
                    </label>
                    <input
                      id="candidateNameInput"
                      type="text"
                      className="form-input"
                      value={createModal.formData.studentName || 'Select an application above'}
                      readOnly
                    />
                  </div>
                </div>

                <div>
                  <label className="form-label" htmlFor="jobRoleInput">
                    Placed Job Role *
                  </label>
                  <input
                    id="jobRoleInput"
                    type="text"
                    className="form-input"
                    placeholder="e.g. Graduate Software Engineer"
                    value={createModal.formData.jobRole}
                    onChange={(e) =>
                      setCreateModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, jobRole: e.target.value },
                      }))
                    }
                    maxLength={150}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label className="form-label" htmlFor="packageAmountInput">
                      Annual Package (LPA)
                    </label>
                    <input
                      id="packageAmountInput"
                      type="number"
                      step="0.01"
                      min="0"
                      className="form-input"
                      placeholder="e.g. 12.50"
                      value={createModal.formData.packageAmount}
                      onChange={(e) =>
                        setCreateModal((prev) => ({
                          ...prev,
                          formData: { ...prev.formData, packageAmount: e.target.value },
                        }))
                      }
                    />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="joiningDateInput">
                      Tentative Joining Date
                    </label>
                    <input
                      id="joiningDateInput"
                      type="date"
                      className="form-input"
                      value={createModal.formData.joiningDate}
                      onChange={(e) =>
                        setCreateModal((prev) => ({
                          ...prev,
                          formData: { ...prev.formData, joiningDate: e.target.value },
                        }))
                      }
                    />
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  label="Cancel"
                  onClick={() => setCreateModal((prev) => ({ ...prev, isOpen: false }))}
                  disabled={createModal.submitting}
                />
                <Button
                  type="submit"
                  variant="primary"
                  label={createModal.submitting ? 'Recording...' : 'Confirm & Save Placement'}
                  disabled={createModal.submitting || !createModal.formData.applicationId}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Placement Modal */}
      {editModal.isOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '520px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                <Pencil size={18} /> Edit Placement Details #{editModal.placementId}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !editModal.submitting && setEditModal((prev) => ({ ...prev, isOpen: false }))}
                disabled={editModal.submitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleEditSubmit}>
              <div style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {editModal.formError && (
                  <div className="alert alert-error" style={{ margin: 0 }}>
                    {editModal.formError}
                  </div>
                )}

                <div>
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Candidate:</span>
                  <div style={{ fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <User size={13} /> {editModal.formData.studentName} (App #{editModal.formData.applicationId})
                  </div>
                </div>

                <div>
                  <label className="form-label" htmlFor="editJobRole">
                    Placed Job Role *
                  </label>
                  <input
                    id="editJobRole"
                    type="text"
                    className="form-input"
                    value={editModal.formData.jobRole}
                    onChange={(e) =>
                      setEditModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, jobRole: e.target.value },
                      }))
                    }
                    maxLength={150}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label className="form-label" htmlFor="editPackageAmount">
                      Annual Package (LPA)
                    </label>
                    <input
                      id="editPackageAmount"
                      type="number"
                      step="0.01"
                      min="0"
                      className="form-input"
                      value={editModal.formData.packageAmount}
                      onChange={(e) =>
                        setEditModal((prev) => ({
                          ...prev,
                          formData: { ...prev.formData, packageAmount: e.target.value },
                        }))
                      }
                    />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="editJoiningDate">
                      Tentative Joining Date
                    </label>
                    <input
                      id="editJoiningDate"
                      type="date"
                      className="form-input"
                      value={editModal.formData.joiningDate}
                      onChange={(e) =>
                        setEditModal((prev) => ({
                          ...prev,
                          formData: { ...prev.formData, joiningDate: e.target.value },
                        }))
                      }
                    />
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  label="Cancel"
                  onClick={() => setEditModal((prev) => ({ ...prev, isOpen: false }))}
                  disabled={editModal.submitting}
                />
                <Button
                  type="submit"
                  variant="primary"
                  label={editModal.submitting ? 'Saving...' : 'Save Changes'}
                  disabled={editModal.submitting}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal for Status Transitions */}
      {confirmModal.isOpen && confirmModal.placement && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {confirmModal.targetStatus === 'JOINED' ? (
                  <>
                    <CheckCircle2 size={18} color="var(--success-color)" /> Confirm Candidate Joined
                  </>
                ) : (
                  <>
                    <AlertTriangle size={18} color="var(--danger-color)" /> Confirm Placement Cancellation
                  </>
                )}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() =>
                  !confirmModal.loading &&
                  setConfirmModal({ isOpen: false, placement: null, targetStatus: null, loading: false })
                }
                disabled={confirmModal.loading}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <p style={{ margin: '0 0 1rem', fontSize: '0.9375rem', lineHeight: 1.5 }}>
                Are you sure you want to transition placement #{confirmModal.placement.placementId} for{' '}
                <strong>{confirmModal.placement.studentName}</strong> to{' '}
                <code>{confirmModal.targetStatus}</code>?
              </p>

              {confirmModal.targetStatus === 'JOINED' ? (
                <div className="alert alert-success" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Important:</strong> <code>JOINED</code> indicates the student has officially onboarded with the hiring organization. This is a terminal state.
                </div>
              ) : (
                <div className="alert alert-error" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Warning:</strong> <code>CANCELLED</code> indicates the placement offer was withdrawn, declined, or revoked. This is a terminal state.
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                label="Cancel"
                onClick={() => setConfirmModal({ isOpen: false, placement: null, targetStatus: null, loading: false })}
                disabled={confirmModal.loading}
              />
              <Button
                type="button"
                variant={confirmModal.targetStatus === 'CANCELLED' ? 'danger' : 'primary'}
                label={
                  confirmModal.loading
                    ? 'Updating...'
                    : confirmModal.targetStatus === 'JOINED'
                      ? 'Confirm Joined'
                      : 'Confirm Cancellation'
                }
                onClick={handleExecuteStatusChange}
                disabled={confirmModal.loading}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminPlacementsPage;