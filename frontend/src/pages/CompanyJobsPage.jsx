import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Briefcase,
  IndianRupee,
  GraduationCap,
  Calendar,
  Wrench,
  AlertTriangle,
  X,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';
import {
  fetchMyCompanyProfile,
  fetchCompanyJobs,
  createCompanyJob,
  updateCompanyJob,
  closeCompanyJob,
} from '../services/companyService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';
import FormInput from '../components/FormInput';
import FormAlert from '../components/FormAlert';

/**
 * CompanyJobsPage Component
 * Provides recruitment coordinators with a full management console for
 * publishing openings, editing job parameters, closing recruitment drives,
 * and filtering active listings.
 */
function CompanyJobsPage() {
  const { user } = useAuth();

  const [company, setCompany] = useState(null);
  const [loadingCompany, setLoadingCompany] = useState(true);

  // Job List Data States
  const [jobs, setJobs] = useState([]);
  const [loadingJobs, setLoadingJobs] = useState(false);
  const [jobsError, setJobsError] = useState(null);

  // Pagination & Filtering
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchTitle, setSearchTitle] = useState('');
  const [statusFilter, setStatusFilter] = useState(''); // '' | 'ACTIVE' | 'CLOSED'

  // Modal States
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedJobForEdit, setSelectedJobForEdit] = useState(null);
  const [inspectJob, setInspectJob] = useState(null);
  const [closingJobId, setClosingJobId] = useState(null);
  const [jobToClose, setJobToClose] = useState(null);

  // Form State (Shared for Create & Edit)
  const [jobForm, setJobForm] = useState({
    title: '',
    jobType: 'FULL_TIME',
    packageAmount: '',
    minCgpa: '',
    deadline: '',
    requiredSkills: '',
    eligibilityCriteria: '',
    description: '',
    status: 'ACTIVE',
  });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [modalErrorAlert, setModalErrorAlert] = useState(null);
  const [modalSuccessAlert, setModalSuccessAlert] = useState(null);
  const [pageSuccessAlert, setPageSuccessAlert] = useState(null);

  // 1. Initial Load: Resolve Company
  useEffect(() => {
    async function initCompany() {
      setLoadingCompany(true);
      try {
        const resolved = await fetchMyCompanyProfile(user?.email);
        setCompany(resolved);
      } catch (err) {
        setJobsError(
          err.response?.data?.message || err.message || 'Failed to resolve corporate recruiter account.'
        );
      } finally {
        setLoadingCompany(false);
      }
    }
    initCompany();
  }, [user?.email]);

  // 2. Load Jobs once company is resolved
  const loadJobs = async () => {
    if (!company?.companyId) return;
    setLoadingJobs(true);
    setJobsError(null);
    try {
      const response = await fetchCompanyJobs(company.companyId, {
        page,
        size: pageSize,
        status: statusFilter || undefined,
        title: searchTitle || undefined,
        sort: 'jobId,desc',
      });
      setJobs(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements ?? 0);
    } catch (err) {
      setJobsError(
        err.response?.data?.message || err.message || 'Failed to retrieve organization job postings.'
      );
    } finally {
      setLoadingJobs(false);
    }
  };

  useEffect(() => {
    if (company?.companyId) {
      loadJobs();
    }
  }, [company?.companyId, page, pageSize, statusFilter]);

  // Handle Search Trigger
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    loadJobs();
  };

  // Form Validation
  const validateJobForm = (isEdit = false) => {
    const errors = {};
    if (!jobForm.title?.trim()) {
      errors.title = 'Job title is required.';
    } else if (jobForm.title.trim().length > 150) {
      errors.title = 'Job title cannot exceed 150 characters.';
    }

    if (jobForm.jobType && jobForm.jobType.length > 50) {
      errors.jobType = 'Job type cannot exceed 50 characters.';
    }

    if (jobForm.packageAmount !== '' && jobForm.packageAmount != null) {
      const pkg = Number(jobForm.packageAmount);
      if (isNaN(pkg) || pkg < 0) {
        errors.packageAmount = 'Package amount must be a positive number.';
      }
    }

    if (jobForm.minCgpa !== '' && jobForm.minCgpa != null) {
      const cgpa = Number(jobForm.minCgpa);
      if (isNaN(cgpa) || cgpa < 0 || cgpa > 10) {
        errors.minCgpa = 'Minimum CGPA must be between 0.00 and 10.00.';
      }
    }

    if (jobForm.deadline) {
      const selected = new Date(jobForm.deadline);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // In create mode: deadline must be today or future
      // In edit mode: cannot change to past date unless keeping the existing deadline
      if (!isEdit && selected < today) {
        errors.deadline = 'Application deadline must be today or in the future.';
      } else if (isEdit && selectedJobForEdit?.deadline !== jobForm.deadline && selected < today) {
        errors.deadline = 'Application deadline cannot be changed to a past date.';
      }
    }

    if (jobForm.requiredSkills && jobForm.requiredSkills.length > 1000) {
      errors.requiredSkills = 'Required skills cannot exceed 1000 characters.';
    }

    if (jobForm.eligibilityCriteria && jobForm.eligibilityCriteria.length > 5000) {
      errors.eligibilityCriteria = 'Eligibility criteria cannot exceed 5000 characters.';
    }

    if (jobForm.description && jobForm.description.length > 5000) {
      errors.description = 'Description cannot exceed 5000 characters.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Open Create Modal
  const handleOpenCreateModal = () => {
    setJobForm({
      title: '',
      jobType: 'FULL_TIME',
      packageAmount: '',
      minCgpa: '',
      deadline: '',
      requiredSkills: '',
      eligibilityCriteria: '',
      description: '',
      status: 'ACTIVE',
    });
    setFormErrors({});
    setModalErrorAlert(null);
    setModalSuccessAlert(null);
    setIsCreateModalOpen(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = (job) => {
    setSelectedJobForEdit(job);
    setJobForm({
      title: job.title || '',
      jobType: job.jobType || 'FULL_TIME',
      packageAmount: job.packageAmount != null ? String(job.packageAmount) : '',
      minCgpa: job.minCgpa != null ? String(job.minCgpa) : '',
      deadline: job.deadline || '',
      requiredSkills: job.requiredSkills || '',
      eligibilityCriteria: job.eligibilityCriteria || '',
      description: job.description || '',
      status: job.status || 'ACTIVE',
    });
    setFormErrors({});
    setModalErrorAlert(null);
    setModalSuccessAlert(null);
    setIsEditModalOpen(true);
  };

  // Submit Create Job
  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setModalErrorAlert(null);
    setModalSuccessAlert(null);

    if (!validateJobForm(false) || !company) return;

    setSubmitting(true);
    try {
      const created = await createCompanyJob({
        ...jobForm,
        companyId: company.companyId,
      });

      setModalSuccessAlert(`Placement drive "${created.title}" successfully published!`);
      setPageSuccessAlert(`Job drive #${created.jobId} created.`);
      await loadJobs();
      setTimeout(() => {
        setIsCreateModalOpen(false);
      }, 1000);
    } catch (err) {
      setModalErrorAlert(
        err.response?.data?.message || err.message || 'Failed to create job posting. Please check inputs.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Submit Edit Job
  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setModalErrorAlert(null);
    setModalSuccessAlert(null);

    if (!validateJobForm(true) || !selectedJobForEdit || !company) return;

    setSubmitting(true);
    try {
      const updated = await updateCompanyJob(selectedJobForEdit.jobId, {
        ...jobForm,
        companyId: company.companyId,
      });

      setModalSuccessAlert(`Job #${updated.jobId} details successfully updated!`);
      setPageSuccessAlert(`Job #${updated.jobId} updated.`);
      await loadJobs();
      setTimeout(() => {
        setIsEditModalOpen(false);
        setSelectedJobForEdit(null);
      }, 1000);
    } catch (err) {
      setModalErrorAlert(
        err.response?.data?.message || err.message || 'Failed to update job details.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Execute Close Job
  const handleConfirmClose = async () => {
    if (!jobToClose) return;
    setClosingJobId(jobToClose.jobId);
    try {
      await closeCompanyJob(jobToClose.jobId);
      setPageSuccessAlert(`Recruitment drive "${jobToClose.title}" is now CLOSED.`);
      setJobToClose(null);
      await loadJobs();
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Failed to close job posting.');
    } finally {
      setClosingJobId(null);
    }
  };

  if (loadingCompany) {
    return <LoadingState message="Loading recruiter company details..." />;
  }

  if (!company) {
    return (
      <EmptyState
        title="Company Profile Required"
        message="Your recruiter user account is not currently linked to an active company profile. Contact the administrator to associate your profile."
        icon={<Building2 size={40} strokeWidth={1.5} />}
        action={
          <Link to="/company/profile" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
            Check Company Profile &rarr;
          </Link>
        }
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Manage Campus Recruitment Drives"
        subtitle={`Recruitment job postings for ${company.name} (Org ID #${company.companyId})`}
        actions={
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Button
              type="button"
              variant="secondary"
              onClick={loadJobs}
              label="Refresh"
            />
            <Button
              type="button"
              variant="primary"
              onClick={handleOpenCreateModal}
              label="+ Post New Job"
            />
          </div>
        }
      />

      {pageSuccessAlert && (
        <FormAlert
          type="success"
          message={pageSuccessAlert}
          style={{ marginBottom: '1.25rem' }}
        />
      )}

      {/* Filter and Search Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem 1.25rem' }}>
        <form
          onSubmit={handleSearchSubmit}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', flex: 1 }}>
            <input
              type="text"
              className="form-input"
              placeholder="Search by job title or keyword..."
              value={searchTitle}
              onChange={(e) => setSearchTitle(e.target.value)}
              style={{ maxWidth: '300px', fontSize: '0.875rem' }}
            />
            <Button
              type="submit"
              variant="secondary"
              label="Search"
              style={{ padding: '0.45rem 0.85rem' }}
            />
            {searchTitle && (
              <Button
                type="button"
                variant="secondary"
                label="Clear"
                onClick={() => {
                  setSearchTitle('');
                  setPage(0);
                }}
                style={{ padding: '0.45rem 0.65rem' }}
              />
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <label style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Status:</label>
            <select
              className="form-select"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              style={{ width: 'auto', fontSize: '0.875rem', padding: '0.4rem 0.75rem' }}
            >
              <option value="">All Drive Statuses</option>
              <option value="ACTIVE">ACTIVE Only</option>
              <option value="CLOSED">CLOSED Only</option>
            </select>
          </div>
        </form>
      </div>

      {/* Main Table Listing */}
      {loadingJobs ? (
        <LoadingState message="Loading company job postings..." />
      ) : jobsError ? (
        <ErrorState message={jobsError} onRetry={loadJobs} />
      ) : jobs.length > 0 ? (
        <div className="data-table-card">
          <div className="data-table-header">
            <div>
              <h2 className="data-table-title">Published Recruitment Openings</h2>
              <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                Showing {jobs.length} of {totalElements} openings for {company.name}
              </span>
            </div>
          </div>

          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Job ID</th>
                  <th>Job Title & Position</th>
                  <th>Job Type</th>
                  <th>Package (CTC)</th>
                  <th>Min CGPA</th>
                  <th>Deadline</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.jobId}>
                    <td>
                      <code>#{job.jobId}</code>
                    </td>
                    <td>
                      <strong>{job.title}</strong>
                      {job.requiredSkills && (
                        <div
                          style={{
                            fontSize: '0.75rem',
                            color: 'var(--text-muted)',
                            maxWidth: '240px',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}
                          title={job.requiredSkills}
                        >
                          Skills: {job.requiredSkills}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="badge badge-info" style={{ fontSize: '0.6875rem' }}>
                        {job.jobType || 'FULL_TIME'}
                      </span>
                    </td>
                    <td>
                      {job.packageAmount != null
                        ? `₹ ${Number(job.packageAmount).toLocaleString()}`
                        : 'Negotiable'}
                    </td>
                    <td>{job.minCgpa != null ? job.minCgpa : 'Open'}</td>
                    <td>
                      {job.deadline ? (
                        <span>{job.deadline}</span>
                      ) : (
                        <span style={{ color: 'var(--text-muted)' }}>No Deadline</span>
                      )}
                    </td>
                    <td>
                      <span
                        className={`badge ${job.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}
                      >
                        {job.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => setInspectJob(job)}
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                        >
                          View
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => handleOpenEditModal(job)}
                          style={{ padding: '0.25rem 0.55rem', fontSize: '0.75rem' }}
                        >
                          Edit
                        </button>
                        {job.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => setJobToClose(job)}
                            disabled={closingJobId === job.jobId}
                            style={{
                              padding: '0.25rem 0.55rem',
                              fontSize: '0.75rem',
                              color: 'var(--danger-color)',
                              borderColor: '#fecaca',
                            }}
                          >
                            Close
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
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
                Page {page + 1} of {totalPages} &bull; Total {totalElements} openings
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
          title={statusFilter || searchTitle ? 'No Matching Job Drives' : 'No Job Drives Posted Yet'}
          message={
            statusFilter || searchTitle
              ? 'No openings match your search or filter criteria.'
              : `${company.name} has not published any recruitment openings yet. Click "+ Post New Job" to create your first campus drive.`
          }
          icon={<Briefcase size={40} strokeWidth={1.5} />}
          action={
            statusFilter || searchTitle ? (
              <Button
                type="button"
                variant="secondary"
                label="Clear Filters"
                onClick={() => {
                  setStatusFilter('');
                  setSearchTitle('');
                  setPage(0);
                }}
                style={{ marginTop: '0.75rem' }}
              />
            ) : (
              <Button
                type="button"
                variant="primary"
                onClick={handleOpenCreateModal}
                label="+ Post First Job"
                style={{ marginTop: '0.75rem' }}
              />
            )
          }
        />
      )}

      {/* =========================================================
          CREATE JOB MODAL
          ========================================================= */}
      {isCreateModalOpen && (
        <div className="modal-backdrop" onClick={() => !submitting && setIsCreateModalOpen(false)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '680px' }}
          >
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Post New Campus Recruitment Drive</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Publishing for <strong>{company.name}</strong> (Company ID #{company.companyId})
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setIsCreateModalOpen(false)}
                disabled={submitting}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} noValidate>
              {modalErrorAlert && <FormAlert type="error" message={modalErrorAlert} />}
              {modalSuccessAlert && <FormAlert type="success" message={modalSuccessAlert} />}

              <FormInput
                label="Job Title"
                name="title"
                type="text"
                placeholder="e.g. Graduate Software Engineer (Java)"
                value={jobForm.title}
                onChange={(e) => {
                  setJobForm((p) => ({ ...p, title: e.target.value }));
                  if (formErrors.title) setFormErrors((p) => ({ ...p, title: null }));
                }}
                required
                error={formErrors.title}
                maxLength={150}
              />

              <div className="form-grid-2">
                <div className="form-group">
                  <label className="form-label" htmlFor="jobType">
                    Engagement Type
                  </label>
                  <select
                    id="jobType"
                    className="form-select"
                    value={jobForm.jobType}
                    onChange={(e) => setJobForm((p) => ({ ...p, jobType: e.target.value }))}
                  >
                    <option value="FULL_TIME">Full Time</option>
                    <option value="INTERNSHIP">Internship</option>
                    <option value="PART_TIME">Part Time</option>
                    <option value="CONTRACT">Contract</option>
                  </select>
                </div>

                <FormInput
                  label="Annual CTC Package (₹)"
                  name="packageAmount"
                  type="number"
                  placeholder="e.g. 1200000"
                  value={jobForm.packageAmount}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, packageAmount: e.target.value }));
                    if (formErrors.packageAmount) setFormErrors((p) => ({ ...p, packageAmount: null }));
                  }}
                  error={formErrors.packageAmount}
                />
              </div>

              <div className="form-grid-2">
                <FormInput
                  label="Minimum CGPA Criterion"
                  name="minCgpa"
                  type="number"
                  step="0.01"
                  placeholder="e.g. 7.50 (leave empty for Open to All)"
                  value={jobForm.minCgpa}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, minCgpa: e.target.value }));
                    if (formErrors.minCgpa) setFormErrors((p) => ({ ...p, minCgpa: null }));
                  }}
                  error={formErrors.minCgpa}
                />

                <FormInput
                  label="Application Deadline"
                  name="deadline"
                  type="date"
                  value={jobForm.deadline}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, deadline: e.target.value }));
                    if (formErrors.deadline) setFormErrors((p) => ({ ...p, deadline: null }));
                  }}
                  error={formErrors.deadline}
                />
              </div>

              <FormInput
                label="Required Skills / Tech Stack"
                name="requiredSkills"
                type="text"
                placeholder="e.g. Java, Spring Boot, MySQL, REST APIs, Git"
                value={jobForm.requiredSkills}
                onChange={(e) => {
                  setJobForm((p) => ({ ...p, requiredSkills: e.target.value }));
                  if (formErrors.requiredSkills) setFormErrors((p) => ({ ...p, requiredSkills: null }));
                }}
                maxLength={1000}
                error={formErrors.requiredSkills}
              />

              <div className="form-group">
                <label className="form-label" htmlFor="eligibilityCriteria">
                  Eligibility Criteria & Academic Qualifications
                </label>
                <textarea
                  id="eligibilityCriteria"
                  rows={3}
                  className="form-input"
                  placeholder="e.g. B.Tech / M.Tech in CS/IT/ECE graduating in 2026. No active backlogs."
                  value={jobForm.eligibilityCriteria}
                  onChange={(e) => setJobForm((p) => ({ ...p, eligibilityCriteria: e.target.value }))}
                  maxLength={5000}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="description">
                  Role Description & Job Responsibilities
                </label>
                <textarea
                  id="description"
                  rows={4}
                  className="form-input"
                  placeholder="Describe day-to-day responsibilities, learning opportunities, and work culture..."
                  value={jobForm.description}
                  onChange={(e) => setJobForm((p) => ({ ...p, description: e.target.value }))}
                  maxLength={5000}
                />
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIsCreateModalOpen(false)}
                  disabled={submitting}
                  label="Cancel"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={submitting}
                  disabled={submitting || Boolean(modalSuccessAlert)}
                  label={submitting ? 'Publishing...' : 'Publish Job Opening'}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================
          EDIT JOB MODAL
          ========================================================= */}
      {isEditModalOpen && selectedJobForEdit && (
        <div className="modal-backdrop" onClick={() => !submitting && setIsEditModalOpen(false)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '680px' }}
          >
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Edit Drive: #{selectedJobForEdit.jobId}</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Updating job specifications for {company.name}
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setIsEditModalOpen(false)}
                disabled={submitting}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleEditSubmit} noValidate>
              {modalErrorAlert && <FormAlert type="error" message={modalErrorAlert} />}
              {modalSuccessAlert && <FormAlert type="success" message={modalSuccessAlert} />}

              <FormInput
                label="Job Title"
                name="editTitle"
                type="text"
                value={jobForm.title}
                onChange={(e) => {
                  setJobForm((p) => ({ ...p, title: e.target.value }));
                  if (formErrors.title) setFormErrors((p) => ({ ...p, title: null }));
                }}
                required
                error={formErrors.title}
                maxLength={150}
              />

              <div className="form-grid-2">
                <div className="form-group">
                  <label className="form-label" htmlFor="editJobType">
                    Engagement Type
                  </label>
                  <select
                    id="editJobType"
                    className="form-select"
                    value={jobForm.jobType}
                    onChange={(e) => setJobForm((p) => ({ ...p, jobType: e.target.value }))}
                  >
                    <option value="FULL_TIME">Full Time</option>
                    <option value="INTERNSHIP">Internship</option>
                    <option value="PART_TIME">Part Time</option>
                    <option value="CONTRACT">Contract</option>
                  </select>
                </div>

                <FormInput
                  label="Annual CTC Package (₹)"
                  name="editPackageAmount"
                  type="number"
                  value={jobForm.packageAmount}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, packageAmount: e.target.value }));
                    if (formErrors.packageAmount) setFormErrors((p) => ({ ...p, packageAmount: null }));
                  }}
                  error={formErrors.packageAmount}
                />
              </div>

              <div className="form-grid-2">
                <FormInput
                  label="Minimum CGPA Criterion"
                  name="editMinCgpa"
                  type="number"
                  step="0.01"
                  value={jobForm.minCgpa}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, minCgpa: e.target.value }));
                    if (formErrors.minCgpa) setFormErrors((p) => ({ ...p, minCgpa: null }));
                  }}
                  error={formErrors.minCgpa}
                />

                <FormInput
                  label="Application Deadline"
                  name="editDeadline"
                  type="date"
                  value={jobForm.deadline}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, deadline: e.target.value }));
                    if (formErrors.deadline) setFormErrors((p) => ({ ...p, deadline: null }));
                  }}
                  error={formErrors.deadline}
                />
              </div>

              <div className="form-grid-2">
                <FormInput
                  label="Required Skills / Tech Stack"
                  name="editRequiredSkills"
                  type="text"
                  value={jobForm.requiredSkills}
                  onChange={(e) => {
                    setJobForm((p) => ({ ...p, requiredSkills: e.target.value }));
                    if (formErrors.requiredSkills) setFormErrors((p) => ({ ...p, requiredSkills: null }));
                  }}
                  maxLength={1000}
                  error={formErrors.requiredSkills}
                />

                <div className="form-group">
                  <label className="form-label" htmlFor="editStatus">
                    Drive Status
                  </label>
                  <select
                    id="editStatus"
                    className="form-select"
                    value={jobForm.status}
                    onChange={(e) => setJobForm((p) => ({ ...p, status: e.target.value }))}
                  >
                    <option value="ACTIVE">ACTIVE (Accepting Applications)</option>
                    <option value="CLOSED">CLOSED (Applications Halted)</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="editEligibility">
                  Eligibility Criteria & Academic Qualifications
                </label>
                <textarea
                  id="editEligibility"
                  rows={3}
                  className="form-input"
                  value={jobForm.eligibilityCriteria}
                  onChange={(e) => setJobForm((p) => ({ ...p, eligibilityCriteria: e.target.value }))}
                  maxLength={5000}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="editDescription">
                  Role Description & Job Responsibilities
                </label>
                <textarea
                  id="editDescription"
                  rows={4}
                  className="form-input"
                  value={jobForm.description}
                  onChange={(e) => setJobForm((p) => ({ ...p, description: e.target.value }))}
                  maxLength={5000}
                />
              </div>

              <div className="modal-footer">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={submitting}
                  label="Cancel"
                />
                <Button
                  type="submit"
                  variant="primary"
                  loading={submitting}
                  disabled={submitting || Boolean(modalSuccessAlert)}
                  label={submitting ? 'Saving...' : 'Save Job Changes'}
                />
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================
          INSPECT / VIEW JOB DETAILS MODAL
          ========================================================= */}
      {inspectJob && (
        <div className="modal-backdrop" onClick={() => setInspectJob(null)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '640px' }}
          >
            <div className="modal-header">
              <div>
                <h3 className="modal-title">{inspectJob.title}</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  Job ID #{inspectJob.jobId} &bull; {inspectJob.companyName}
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setInspectJob(null)}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <div>
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
                    Recruitment Status
                  </div>
                  <strong style={{ fontSize: '1rem', color: 'var(--secondary-color)' }}>
                    {inspectJob.status === 'ACTIVE' ? 'Accepting Applications' : 'Closed for Applications'}
                  </strong>
                </div>
                <span className={`badge ${inspectJob.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                  {inspectJob.status}
                </span>
              </div>

              <div className="job-spec-list">
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Briefcase size={15} className="inline-icon" />
                    Position Type:
                  </span>
                  <span className="job-spec-row-value">{inspectJob.jobType || 'FULL_TIME'}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <IndianRupee size={15} className="inline-icon" />
                    CTC Package:
                  </span>
                  <span className="job-spec-row-value">
                    {inspectJob.packageAmount != null
                      ? `₹ ${Number(inspectJob.packageAmount).toLocaleString()}`
                      : 'Negotiable'}
                  </span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <GraduationCap size={15} className="inline-icon" />
                    Min CGPA Required:
                  </span>
                  <span className="job-spec-row-value">{inspectJob.minCgpa != null ? inspectJob.minCgpa : 'Open to All'}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Calendar size={15} className="inline-icon" />
                    Application Deadline:
                  </span>
                  <span className="job-spec-row-value">{inspectJob.deadline || 'No Deadline'}</span>
                </div>
                <div className="job-spec-row">
                  <span className="job-spec-row-label">
                    <Wrench size={15} className="inline-icon" />
                    Required Skills:
                  </span>
                  <span className="job-spec-row-value">{inspectJob.requiredSkills || 'None listed'}</span>
                </div>
              </div>

              {inspectJob.eligibilityCriteria && (
                <div style={{ marginTop: '1.25rem' }}>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', marginBottom: '0.35rem' }}>
                    Eligibility Criteria
                  </div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-main)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                    {inspectJob.eligibilityCriteria}
                  </div>
                </div>
              )}

              {inspectJob.description && (
                <div style={{ marginTop: '1rem' }}>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--secondary-color)', marginBottom: '0.35rem' }}>
                    Job Description
                  </div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-main)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                    {inspectJob.description}
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setInspectJob(null)}
                label="Close"
              />
              <Button
                type="button"
                variant="primary"
                onClick={() => {
                  const target = inspectJob;
                  setInspectJob(null);
                  handleOpenEditModal(target);
                }}
                label="Edit Job &rarr;"
              />
            </div>
          </div>
        </div>
      )}

      {/* =========================================================
          CLOSE JOB CONFIRMATION MODAL
          ========================================================= */}
      {jobToClose && (
        <div className="modal-backdrop" onClick={() => setJobToClose(null)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '500px' }}
          >
            <div className="modal-header">
              <h3 className="modal-title">Close Recruitment Drive</h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setJobToClose(null)}
                aria-label="Close modal"
                style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: '0.5rem 0' }}>
              <p style={{ fontSize: '0.9375rem', color: 'var(--text-main)', lineHeight: 1.5, margin: '0 0 1rem' }}>
                Are you sure you want to close the job posting <strong>"{jobToClose.title}"</strong> (Job #{jobToClose.jobId})?
              </p>
              <div
                style={{
                  backgroundColor: '#fffbeb',
                  border: '1px solid #fde68a',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.75rem 1rem',
                  fontSize: '0.8125rem',
                  color: '#92400e',
                  lineHeight: 1.5,
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.5rem',
                }}
              >
                <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '0.125rem' }} />
                <div>
                  <strong>Note:</strong> Closing a job disables new candidate submissions and transitions the drive status to <code>CLOSED</code>. Existing applicant records and interview schedules are preserved.
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setJobToClose(null)}
                disabled={Boolean(closingJobId)}
                label="Cancel"
              />
              <Button
                type="button"
                variant="danger"
                loading={Boolean(closingJobId)}
                disabled={Boolean(closingJobId)}
                onClick={handleConfirmClose}
                label={closingJobId ? 'Closing...' : 'Yes, Close Drive'}
                style={{ backgroundColor: 'var(--danger-color)', borderColor: 'var(--danger-color)', color: '#fff' }}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default CompanyJobsPage;
