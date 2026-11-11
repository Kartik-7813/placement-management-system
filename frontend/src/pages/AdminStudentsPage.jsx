import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  GraduationCap,
  FileText,
  Award,
  RefreshCw,
  X,
  Search,
  Eye,
  AlertTriangle,
  CheckCircle2,
  Zap,
} from 'lucide-react';
import {
  fetchStudents,
  getStudentById,
  updateStudent,
  deactivateStudent,
  getStudentSkills,
} from '../services/adminService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * AdminStudentsPage Component
 * Provides institutional administrators with complete student directory oversight:
 * - Search by candidate name
 * - Department and account status filters
 * - Academic CGPA and passing year verification
 * - Detailed profile modal with assigned technical skills
 * - Direct shortcut to candidate application history
 * - Student profile activation and soft deactivation with confirmation dialogs
 */
function AdminStudentsPage() {
  const [students, setStudents] = useState([]);
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

  const [departmentFilter, setDepartmentFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchName, setSearchName] = useState('');
  const [searchInput, setSearchInput] = useState('');

  // Modals
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [studentSkills, setStudentSkills] = useState([]);
  const [loadingSkills, setLoadingSkills] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);

  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    student: null,
    actionType: null, // 'DEACTIVATE' | 'ACTIVATE'
    loading: false,
  });

  const loadStudents = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await fetchStudents({
        department: departmentFilter !== 'ALL' ? departmentFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        name: searchName || undefined,
        page,
        size: pageSize,
        sort: 'studentId,asc',
      });

      setStudents(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load students:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve student directory.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadStudents();
  }, [page, departmentFilter, statusFilter, searchName]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setSearchName(searchInput.trim());
  };

  const handleClearFilters = () => {
    setDepartmentFilter('ALL');
    setStatusFilter('ALL');
    setSearchName('');
    setSearchInput('');
    setPage(0);
  };

  // Inspect student details
  const handleOpenDetails = async (student) => {
    setSelectedStudent(student);
    setDetailsModalOpen(true);
    setLoadingSkills(true);
    setStudentSkills([]);

    try {
      const skills = await getStudentSkills(student.studentId);
      setStudentSkills(skills);
    } catch (err) {
      console.warn('Failed to load skills for student:', err);
    } finally {
      setLoadingSkills(false);
    }
  };

  // Open confirmation modal
  const handleOpenConfirm = (student, actionType) => {
    setActionError(null);
    setActionSuccess(null);
    setConfirmModal({
      isOpen: true,
      student,
      actionType,
      loading: false,
    });
  };

  // Execute confirmation action
  const handleExecuteAction = async () => {
    const { student, actionType } = confirmModal;
    if (!student || !actionType) return;

    setConfirmModal((prev) => ({ ...prev, loading: true }));
    setActionError(null);
    setActionSuccess(null);

    try {
      if (actionType === 'DEACTIVATE') {
        await deactivateStudent(student.studentId);
        setActionSuccess(
          `Student profile #${student.studentId} (${student.name}) was deactivated successfully.`
        );
      } else if (actionType === 'ACTIVATE') {
        await updateStudent(student.studentId, {
          ...student,
          status: 'ACTIVE',
        });
        setActionSuccess(
          `Student profile #${student.studentId} (${student.name}) was re-activated successfully.`
        );
      }

      setConfirmModal({ isOpen: false, student: null, actionType: null, loading: false });
      await loadStudents(true);
    } catch (err) {
      console.error('Failed to update student account status:', err);
      setActionError(
        err.response?.data?.message || err.message || 'Failed to update student account status.'
      );
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  if (loading && !refreshing) {
    return <LoadingState message="Loading institutional student directory..." />;
  }

  if (error && students.length === 0) {
    return <ErrorState message={error} onRetry={() => loadStudents()} />;
  }

  return (
    <div>
      <PageHeader
        title="Student Directory & Academic Records"
        subtitle="Institution-wide student profiles, academic verification, skill inventory, and account status management"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Link to="/admin/applications" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
              <FileText size={15} /> Applications Oversight
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadStudents(true)}
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

      {/* Filters Card */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label className="form-label" htmlFor="studentSearchInput">
              Search by Student Name
            </label>
            <input
              id="studentSearchInput"
              type="text"
              className="form-input"
              placeholder="e.g. John Doe"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="departmentSelect">
              Department
            </label>
            <select
              id="departmentSelect"
              className="form-input"
              value={departmentFilter}
              onChange={(e) => {
                setDepartmentFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Departments</option>
              <option value="Computer Science">Computer Science</option>
              <option value="Information Technology">Information Technology</option>
              <option value="Electronics">Electronics &amp; Comm</option>
              <option value="Electrical">Electrical Engineering</option>
              <option value="Mechanical">Mechanical Engineering</option>
              <option value="Civil">Civil Engineering</option>
            </select>
          </div>

          <div>
            <label className="form-label" htmlFor="statusSelect">
              Account Status
            </label>
            <select
              id="statusSelect"
              className="form-input"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
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

      {/* Table Section */}
      {students.length === 0 ? (
        <EmptyState
          title="No Students Found"
          message="No student profiles matched the specified search criteria or department filter."
          icon={<GraduationCap size={40} />}
          action={
            (departmentFilter !== 'ALL' || statusFilter !== 'ALL' || searchName) ? (
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
              Showing {students.length} of {totalElements} registered students
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
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Roll No</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Name</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Email</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Department</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>CGPA</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Pass Year</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.studentId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{s.studentId}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontFamily: 'monospace' }}>
                      {s.rollNumber}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      {s.name}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {s.email}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      {s.department || '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600 }}>
                      {s.cgpa != null ? s.cgpa.toFixed(2) : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {s.passingYear || '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <span className={`badge ${s.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                        {s.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => handleOpenDetails(s)}
                          title="View student profile and assigned skills"
                        >
                          <Eye size={13} /> Details
                        </button>

                        {s.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            className="btn btn-danger"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                            onClick={() => handleOpenConfirm(s, 'DEACTIVATE')}
                            title="Deactivate student account"
                          >
                            Deactivate
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-primary"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                            onClick={() => handleOpenConfirm(s, 'ACTIVATE')}
                            title="Re-activate student account"
                          >
                            Re-activate
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

      {/* Details Modal */}
      {detailsModalOpen && selectedStudent && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '640px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <h3 className="modal-title" style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <GraduationCap size={18} /> {selectedStudent.name}
                </h3>
                <span className={`badge ${selectedStudent.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                  {selectedStudent.status}
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
                  marginBottom: '1.25rem',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Student ID</span>
                  <strong>#{selectedStudent.studentId}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Roll Number</span>
                  <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{selectedStudent.rollNumber}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Email Address</span>
                  <span>{selectedStudent.email}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Phone</span>
                  <span>{selectedStudent.phone || 'Not provided'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Department</span>
                  <strong>{selectedStudent.department || 'Not assigned'}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Academic CGPA</span>
                  <strong style={{ color: 'var(--primary-color)' }}>
                    {selectedStudent.cgpa != null ? selectedStudent.cgpa.toFixed(2) : '—'} / 10.0
                  </strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Graduation Year</span>
                  <span>{selectedStudent.passingYear || '—'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Gender</span>
                  <span>{selectedStudent.gender || '—'}</span>
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Residential Address</span>
                  <span>{selectedStudent.address || 'No address provided'}</span>
                </div>
              </div>

              {/* Skills section */}
              <div style={{ marginBottom: '1.25rem' }}>
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 0.5rem' }}>
                  Assigned Technical Skills ({studentSkills.length})
                </h4>
                {loadingSkills ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Loading skills inventory...</div>
                ) : studentSkills.length === 0 ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No technical skills assigned to this student profile.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                    {studentSkills.map((sk) => (
                      <span
                        key={sk.skillId}
                        style={{
                          backgroundColor: '#e0e7ff',
                          color: '#3730a3',
                          padding: '0.2rem 0.6rem',
                          borderRadius: '999px',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                        }}
                      >
                        <Zap size={11} /> {sk.name}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <Link
                  to={`/admin/applications?studentId=${selectedStudent.studentId}`}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                  onClick={() => setDetailsModalOpen(false)}
                >
                  <FileText size={14} /> Applications &rarr;
                </Link>
                <Link
                  to={`/admin/placements`}
                  className="btn btn-primary"
                  style={{ fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                  onClick={() => setDetailsModalOpen(false)}
                >
                  <Award size={14} /> View Placements &rarr;
                </Link>
              </div>
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
      {confirmModal.isOpen && confirmModal.student && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {confirmModal.actionType === 'DEACTIVATE' ? (
                  <>
                    <AlertTriangle size={18} color="var(--warning-color)" /> Confirm Student Deactivation
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} color="var(--success-color)" /> Confirm Student Activation
                  </>
                )}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !confirmModal.loading && setConfirmModal({ isOpen: false, student: null, actionType: null, loading: false })}
                disabled={confirmModal.loading}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <p style={{ margin: '0 0 1rem', fontSize: '0.9375rem', lineHeight: 1.5 }}>
                Are you sure you want to {confirmModal.actionType === 'DEACTIVATE' ? 'deactivate' : 're-activate'} the profile of{' '}
                <strong>{confirmModal.student.name}</strong> (Roll No: <code>{confirmModal.student.rollNumber}</code>)?
              </p>

              {confirmModal.actionType === 'DEACTIVATE' ? (
                <div className="alert alert-warning" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Note:</strong> Deactivating marks this student as <code>INACTIVE</code> in the institutional repository. The student will not be permitted to apply for active job openings until re-activated.
                </div>
              ) : (
                <div className="alert alert-info" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  This will restore the student's status to <code>ACTIVE</code>, allowing participation in eligible campus recruitment drives.
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                label="Cancel"
                onClick={() => setConfirmModal({ isOpen: false, student: null, actionType: null, loading: false })}
                disabled={confirmModal.loading}
              />
              <Button
                type="button"
                variant={confirmModal.actionType === 'DEACTIVATE' ? 'danger' : 'primary'}
                label={confirmModal.loading ? 'Updating...' : confirmModal.actionType === 'DEACTIVATE' ? 'Confirm Deactivation' : 'Confirm Activation'}
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

export default AdminStudentsPage;
