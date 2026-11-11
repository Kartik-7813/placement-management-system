import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Briefcase,
  Plus,
  RefreshCw,
  X,
  Search,
  Eye,
  Pencil,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react';
import {
  fetchCompanies,
  createCompany,
  updateCompany,
  deactivateCompany,
} from '../services/adminService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * AdminCompaniesPage Component
 * Provides institutional administrators with complete recruiting corporate partner oversight:
 * - Search by corporate name
 * - Partner status filtering (ACTIVE / INACTIVE)
 * - Registration of new recruiting organizations (POST /api/companies)
 * - Editing existing company profiles (PUT /api/companies/{id})
 * - Company deactivation (DELETE /api/companies/{id}) with automatic job closure warning
 * - Re-activation of inactive organizations
 * - Shortcuts to associated company job listings
 */
function AdminCompaniesPage() {
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

  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchName, setSearchName] = useState('');
  const [searchInput, setSearchInput] = useState('');

  // Modals
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);

  // Add / Edit Form Modal
  const [formModal, setFormModal] = useState({
    isOpen: false,
    mode: 'CREATE', // 'CREATE' | 'EDIT'
    companyId: null,
    formData: {
      name: '',
      website: '',
      contactPerson: '',
      email: '',
      phone: '',
      address: '',
      status: 'ACTIVE',
    },
    submitting: false,
    formError: null,
  });

  // Confirm Modal
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    company: null,
    actionType: null, // 'DEACTIVATE' | 'ACTIVATE'
    loading: false,
  });

  const loadCompanies = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await fetchCompanies({
        name: searchName || undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        page,
        size: pageSize,
        sort: 'companyId,asc',
      });

      setCompanies(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load companies:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve company directory.'
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadCompanies();
  }, [page, statusFilter, searchName]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setSearchName(searchInput.trim());
  };

  const handleClearFilters = () => {
    setStatusFilter('ALL');
    setSearchName('');
    setSearchInput('');
    setPage(0);
  };

  // Open Create Modal
  const handleOpenCreate = () => {
    setActionError(null);
    setActionSuccess(null);
    setFormModal({
      isOpen: true,
      mode: 'CREATE',
      companyId: null,
      formData: {
        name: '',
        website: '',
        contactPerson: '',
        email: '',
        phone: '',
        address: '',
        status: 'ACTIVE',
      },
      submitting: false,
      formError: null,
    });
  };

  // Open Edit Modal
  const handleOpenEdit = (comp) => {
    setActionError(null);
    setActionSuccess(null);
    setFormModal({
      isOpen: true,
      mode: 'EDIT',
      companyId: comp.companyId,
      formData: {
        name: comp.name || '',
        website: comp.website || '',
        contactPerson: comp.contactPerson || '',
        email: comp.email || '',
        phone: comp.phone || '',
        address: comp.address || '',
        status: comp.status || 'ACTIVE',
      },
      submitting: false,
      formError: null,
    });
  };

  // Handle Form Submit (Create or Update)
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    const { mode, companyId, formData } = formModal;

    if (!formData.name.trim() || !formData.email.trim()) {
      setFormModal((prev) => ({
        ...prev,
        formError: 'Company Name and Email are required fields.',
      }));
      return;
    }

    setFormModal((prev) => ({ ...prev, submitting: true, formError: null }));

    try {
      if (mode === 'CREATE') {
        const created = await createCompany(formData);
        setActionSuccess(`Corporate partner "${created.name}" registered successfully.`);
      } else {
        const updated = await updateCompany(companyId, formData);
        setActionSuccess(`Corporate partner "${updated.name}" updated successfully.`);
      }

      setFormModal({ isOpen: false, mode: 'CREATE', companyId: null, formData: {}, submitting: false, formError: null });
      await loadCompanies(true);
    } catch (err) {
      console.error('Failed to save company profile:', err);
      setFormModal((prev) => ({
        ...prev,
        submitting: false,
        formError: err.response?.data?.message || err.message || 'Failed to save company record.',
      }));
    }
  };

  // Open confirm modal
  const handleOpenConfirm = (company, actionType) => {
    setActionError(null);
    setActionSuccess(null);
    setConfirmModal({
      isOpen: true,
      company,
      actionType,
      loading: false,
    });
  };

  // Execute confirm modal
  const handleExecuteAction = async () => {
    const { company, actionType } = confirmModal;
    if (!company || !actionType) return;

    setConfirmModal((prev) => ({ ...prev, loading: true }));
    setActionError(null);
    setActionSuccess(null);

    try {
      if (actionType === 'DEACTIVATE') {
        await deactivateCompany(company.companyId);
        setActionSuccess(
          `Company "${company.name}" was deactivated. Any currently active job openings have been set to CLOSED.`
        );
      } else if (actionType === 'ACTIVATE') {
        await updateCompany(company.companyId, {
          ...company,
          status: 'ACTIVE',
        });
        setActionSuccess(`Company "${company.name}" was re-activated successfully.`);
      }

      setConfirmModal({ isOpen: false, company: null, actionType: null, loading: false });
      await loadCompanies(true);
    } catch (err) {
      console.error('Failed to change company status:', err);
      setActionError(
        err.response?.data?.message || err.message || 'Failed to change company status.'
      );
      setConfirmModal((prev) => ({ ...prev, loading: false }));
    }
  };

  if (loading && !refreshing) {
    return <LoadingState message="Loading recruiting company registry..." />;
  }

  if (error && companies.length === 0) {
    return <ErrorState message={error} onRetry={() => loadCompanies()} />;
  }

  return (
    <div>
      <PageHeader
        title="Corporate Partners & Recruiting Companies"
        subtitle="Manage campus placement partners, institutional affiliations, recruiter accounts, and drive authorizations"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Button
              type="button"
              variant="primary"
              onClick={handleOpenCreate}
              label={
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Plus size={15} /> Register Company
                </span>
              }
            />
            <Button
              type="button"
              variant="secondary"
              onClick={() => loadCompanies(true)}
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
        <form onSubmit={handleSearchSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label className="form-label" htmlFor="companySearchInput">
              Search by Company Name
            </label>
            <input
              id="companySearchInput"
              type="text"
              className="form-input"
              placeholder="e.g. Google, Microsoft, Tech..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="statusFilter">
              Partnership Status
            </label>
            <select
              id="statusFilter"
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

      {/* Companies Table */}
      {companies.length === 0 ? (
        <EmptyState
          title="No Companies Found"
          message="No corporate partners matched your specified search or status filter."
          icon={<Building2 size={40} />}
          action={
            (statusFilter !== 'ALL' || searchName) ? (
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
              Showing {companies.length} of {totalElements} registered corporate partners
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
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Company Name</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Contact Person</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Email</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Phone</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Website</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {companies.map((c) => (
                  <tr key={c.companyId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                      #{c.companyId}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)' }}>
                      {c.name}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      {c.contactPerson || '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {c.email}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {c.phone || '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.8125rem' }}>
                      {c.website ? (
                        <a
                          href={c.website.startsWith('http') ? c.website : `https://${c.website}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{ color: 'var(--primary-color)', textDecoration: 'none' }}
                        >
                          Visit &nearr;
                        </a>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <span className={`badge ${c.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                        {c.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => {
                            setSelectedCompany(c);
                            setDetailsModalOpen(true);
                          }}
                          title="View company details & openings"
                        >
                          <Eye size={13} /> View
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                          onClick={() => handleOpenEdit(c)}
                          title="Edit company profile"
                        >
                          <Pencil size={13} /> Edit
                        </button>
                        {c.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            className="btn btn-danger"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                            onClick={() => handleOpenConfirm(c, 'DEACTIVATE')}
                            title="Deactivate company and close open jobs"
                          >
                            Deactivate
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-primary"
                            style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem', backgroundColor: '#16a34a', borderColor: '#16a34a' }}
                            onClick={() => handleOpenConfirm(c, 'ACTIVATE')}
                            title="Re-activate company"
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
      {detailsModalOpen && selectedCompany && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '600px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <h3 className="modal-title" style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Building2 size={18} /> {selectedCompany.name}
                </h3>
                <span className={`badge ${selectedCompany.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.6875rem' }}>
                  {selectedCompany.status}
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
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Company ID</span>
                  <strong>#{selectedCompany.companyId}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Contact Person</span>
                  <strong>{selectedCompany.contactPerson || 'Not specified'}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Official Email</span>
                  <span>{selectedCompany.email}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Phone</span>
                  <span>{selectedCompany.phone || 'Not provided'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Website</span>
                  <span>
                    {selectedCompany.website ? (
                      <a
                        href={selectedCompany.website.startsWith('http') ? selectedCompany.website : `https://${selectedCompany.website}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ color: 'var(--primary-color)' }}
                      >
                        {selectedCompany.website} &nearr;
                      </a>
                    ) : (
                      '—'
                    )}
                  </span>
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Corporate Address</span>
                  <span>{selectedCompany.address || 'No physical address recorded'}</span>
                </div>
              </div>
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <Link
                to={`/admin/jobs?companyId=${selectedCompany.companyId}`}
                className="btn btn-primary"
                style={{ fontSize: '0.8125rem', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                onClick={() => setDetailsModalOpen(false)}
              >
                <Briefcase size={14} /> View Job Openings by this Company &rarr;
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

      {/* Create / Edit Company Modal */}
      {formModal.isOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '540px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {formModal.mode === 'CREATE' ? (
                  <>
                    <Building2 size={18} /> Register New Corporate Partner
                  </>
                ) : (
                  <>
                    <Pencil size={18} /> Edit &quot;{formModal.formData.name}&quot;
                  </>
                )}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !formModal.submitting && setFormModal((prev) => ({ ...prev, isOpen: false }))}
                disabled={formModal.submitting}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleFormSubmit}>
              <div style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {formModal.formError && (
                  <div className="alert alert-error" style={{ margin: 0 }}>
                    {formModal.formError}
                  </div>
                )}

                <div>
                  <label className="form-label" htmlFor="companyNameField">
                    Company Name *
                  </label>
                  <input
                    id="companyNameField"
                    type="text"
                    className="form-input"
                    required
                    placeholder="e.g. Acme Corporation"
                    value={formModal.formData.name}
                    onChange={(e) => setFormModal((prev) => ({
                      ...prev,
                      formData: { ...prev.formData, name: e.target.value }
                    }))}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label className="form-label" htmlFor="companyEmailField">
                      Official Email *
                    </label>
                    <input
                      id="companyEmailField"
                      type="email"
                      className="form-input"
                      required
                      placeholder="hr@acme.com"
                      value={formModal.formData.email}
                      onChange={(e) => setFormModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, email: e.target.value }
                      }))}
                    />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="companyPhoneField">
                      Phone Number
                    </label>
                    <input
                      id="companyPhoneField"
                      type="text"
                      className="form-input"
                      placeholder="+91 9876543210"
                      value={formModal.formData.phone}
                      onChange={(e) => setFormModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, phone: e.target.value }
                      }))}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label className="form-label" htmlFor="contactPersonField">
                      Contact Person
                    </label>
                    <input
                      id="contactPersonField"
                      type="text"
                      className="form-input"
                      placeholder="e.g. Jane Doe"
                      value={formModal.formData.contactPerson}
                      onChange={(e) => setFormModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, contactPerson: e.target.value }
                      }))}
                    />
                  </div>

                  <div>
                    <label className="form-label" htmlFor="websiteField">
                      Website URL
                    </label>
                    <input
                      id="websiteField"
                      type="text"
                      className="form-input"
                      placeholder="https://acme.com"
                      value={formModal.formData.website}
                      onChange={(e) => setFormModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, website: e.target.value }
                      }))}
                    />
                  </div>
                </div>

                <div>
                  <label className="form-label" htmlFor="addressField">
                    Corporate Headquarters Address
                  </label>
                  <textarea
                    id="addressField"
                    className="form-input"
                    rows={2}
                    placeholder="Street, City, State, Country"
                    value={formModal.formData.address}
                    onChange={(e) => setFormModal((prev) => ({
                      ...prev,
                      formData: { ...prev.formData, address: e.target.value }
                    }))}
                  />
                </div>

                {formModal.mode === 'EDIT' && (
                  <div>
                    <label className="form-label" htmlFor="statusEditField">
                      Company Status
                    </label>
                    <select
                      id="statusEditField"
                      className="form-input"
                      value={formModal.formData.status}
                      onChange={(e) => setFormModal((prev) => ({
                        ...prev,
                        formData: { ...prev.formData, status: e.target.value }
                      }))}
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                    </select>
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  label="Cancel"
                  onClick={() => setFormModal((prev) => ({ ...prev, isOpen: false }))}
                  disabled={formModal.submitting}
                />
                <Button
                  type="submit"
                  variant="primary"
                  label={formModal.submitting ? 'Saving...' : formModal.mode === 'CREATE' ? 'Register Company' : 'Save Changes'}
                  disabled={formModal.submitting}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmModal.isOpen && confirmModal.company && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                {confirmModal.actionType === 'DEACTIVATE' ? (
                  <>
                    <AlertTriangle size={18} color="var(--warning-color)" /> Confirm Company Deactivation
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} color="var(--success-color)" /> Confirm Company Activation
                  </>
                )}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => !confirmModal.loading && setConfirmModal({ isOpen: false, company: null, actionType: null, loading: false })}
                disabled={confirmModal.loading}
                aria-label="Close modal"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '1.25rem' }}>
              <p style={{ margin: '0 0 1rem', fontSize: '0.9375rem', lineHeight: 1.5 }}>
                Are you sure you want to {confirmModal.actionType === 'DEACTIVATE' ? 'deactivate' : 're-activate'}{' '}
                <strong>{confirmModal.company.name}</strong> (Company #{confirmModal.company.companyId})?
              </p>

              {confirmModal.actionType === 'DEACTIVATE' ? (
                <div className="alert alert-warning" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  <strong>Important:</strong> Deactivating a corporate partner automatically transitions all of its currently active recruitment job postings to <code>CLOSED</code>.
                </div>
              ) : (
                <div className="alert alert-info" style={{ fontSize: '0.8125rem', marginBottom: 0 }}>
                  Re-activating restores the organization's active partnership status in the portal.
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                label="Cancel"
                onClick={() => setConfirmModal({ isOpen: false, company: null, actionType: null, loading: false })}
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

export default AdminCompaniesPage;
