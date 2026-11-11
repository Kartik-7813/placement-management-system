import api from './api';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * Admin Service
 * Provides complete REST API integration for institutional administrator oversight:
 * - Student profiles, academic credentials, and deactivation
 * - Corporate partner registry, registration, editing, and deactivation
 * - Platform-wide job openings management and closure
 * - Institution-wide application oversight and state-machine transitions
 * - High-level placement analytics summary
 */

// ============================================================================
// STUDENT MANAGEMENT
// ============================================================================

/**
 * Retrieves paginated students with server-side filters.
 * Requires ROLE_ADMIN or ROLE_COMPANY.
 *
 * @param {Object} [params]
 * @param {string} [params.department]
 * @param {string} [params.status] - ACTIVE | INACTIVE
 * @param {string} [params.name] - Search by candidate name
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='studentId,asc']
 * @returns {Promise<Object>} Spring Data Page<StudentResponse>
 */
export async function fetchStudents({
  department,
  status,
  name,
  page = 0,
  size = 10,
  sort = 'studentId,asc',
} = {}) {
  let url = `${API_ENDPOINTS.STUDENTS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (department && department !== 'ALL') {
    url += `&department=${encodeURIComponent(department.trim())}`;
  }
  if (status && status !== 'ALL') {
    url += `&status=${encodeURIComponent(status.trim())}`;
  }
  if (name && name.trim()) {
    url += `&name=${encodeURIComponent(name.trim())}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single student record by primary key ID.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} StudentResponse
 */
export async function getStudentById(id) {
  const response = await api.get(API_ENDPOINTS.STUDENTS.BY_ID(id));
  return response.data;
}

/**
 * Registers a new student profile on the platform.
 * Requires ROLE_ADMIN.
 *
 * @param {Object} payload - StudentRequest DTO
 * @returns {Promise<Object>} StudentResponse
 */
export async function createStudent(payload) {
  const response = await api.post(API_ENDPOINTS.STUDENTS.BASE, payload);
  return response.data;
}

/**
 * Updates an existing student's profile.
 * Requires ROLE_ADMIN or profile-owner STUDENT.
 * Note: Email is immutable on backend; login email must remain unchanged.
 *
 * @param {number|string} id
 * @param {Object} payload - StudentRequest DTO
 * @returns {Promise<Object>} Updated StudentResponse
 */
export async function updateStudent(id, payload) {
  const body = {
    rollNumber: payload.rollNumber?.trim(),
    name: payload.name?.trim(),
    email: payload.email?.trim(),
    phone: payload.phone?.trim() || null,
    dob: payload.dob || null,
    gender: payload.gender || null,
    department: payload.department?.trim() || null,
    cgpa: payload.cgpa !== '' && payload.cgpa != null ? Number(payload.cgpa) : null,
    passingYear: payload.passingYear ? Number(payload.passingYear) : null,
    address: payload.address?.trim() || null,
    status: payload.status || 'ACTIVE',
  };

  const response = await api.put(API_ENDPOINTS.STUDENTS.BY_ID(id), body);
  return response.data;
}

/**
 * Deactivates a student profile (sets status = INACTIVE).
 * Requires ROLE_ADMIN.
 *
 * @param {number|string} id
 * @returns {Promise<void>}
 */
export async function deactivateStudent(id) {
  await api.delete(API_ENDPOINTS.STUDENTS.BY_ID(id));
}

/**
 * Retrieves skills assigned to a student.
 *
 * @param {number|string} studentId
 * @returns {Promise<Array<Object>>} List of SkillResponse DTOs
 */
export async function getStudentSkills(studentId) {
  const response = await api.get(API_ENDPOINTS.STUDENTS.SKILLS(studentId));
  return response.data || [];
}

// ============================================================================
// COMPANY MANAGEMENT
// ============================================================================

/**
 * Retrieves paginated corporate recruiters with name and status filters.
 *
 * @param {Object} [params]
 * @param {string} [params.name]
 * @param {string} [params.status] - ACTIVE | INACTIVE
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='companyId,asc']
 * @returns {Promise<Object>} Spring Data Page<CompanyResponse>
 */
export async function fetchCompanies({
  name,
  status,
  page = 0,
  size = 10,
  sort = 'companyId,asc',
} = {}) {
  let url = `${API_ENDPOINTS.COMPANIES.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (name && name.trim()) {
    url += `&name=${encodeURIComponent(name.trim())}`;
  }
  if (status && status !== 'ALL') {
    url += `&status=${encodeURIComponent(status.trim())}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a company record by primary key ID.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} CompanyResponse
 */
export async function getCompanyById(id) {
  const response = await api.get(API_ENDPOINTS.COMPANIES.BY_ID(id));
  return response.data;
}

/**
 * Registers a new recruiting company.
 * Requires ROLE_ADMIN.
 *
 * @param {Object} payload - CompanyRequest DTO
 * @returns {Promise<Object>} CompanyResponse
 */
export async function createCompany(payload) {
  const body = {
    name: payload.name?.trim(),
    website: payload.website?.trim() || null,
    contactPerson: payload.contactPerson?.trim() || null,
    email: payload.email?.trim(),
    phone: payload.phone?.trim() || null,
    address: payload.address?.trim() || null,
    status: payload.status || 'ACTIVE',
  };

  const response = await api.post(API_ENDPOINTS.COMPANIES.BASE, body);
  return response.data;
}

/**
 * Updates an existing company's information.
 * Requires ROLE_ADMIN or owning COMPANY.
 *
 * @param {number|string} id
 * @param {Object} payload - CompanyRequest DTO
 * @returns {Promise<Object>} Updated CompanyResponse
 */
export async function updateCompany(id, payload) {
  const body = {
    name: payload.name?.trim(),
    website: payload.website?.trim() || null,
    contactPerson: payload.contactPerson?.trim() || null,
    email: payload.email?.trim(),
    phone: payload.phone?.trim() || null,
    address: payload.address?.trim() || null,
    status: payload.status || 'ACTIVE',
  };

  const response = await api.put(API_ENDPOINTS.COMPANIES.BY_ID(id), body);
  return response.data;
}

/**
 * Deactivates a company (sets status = INACTIVE and automatically closes all its active jobs).
 * Requires ROLE_ADMIN.
 *
 * @param {number|string} id
 * @returns {Promise<void>}
 */
export async function deactivateCompany(id) {
  await api.delete(API_ENDPOINTS.COMPANIES.BY_ID(id));
}

// ============================================================================
// JOB MANAGEMENT
// ============================================================================

/**
 * Retrieves all jobs posted across the platform with optional filters.
 *
 * @param {Object} [params]
 * @param {string} [params.title]
 * @param {number|string} [params.companyId]
 * @param {string} [params.status] - ACTIVE | CLOSED
 * @param {number} [params.minCgpa]
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='jobId,desc']
 * @returns {Promise<Object>} Spring Data Page<JobResponse>
 */
export async function fetchAllJobs({
  title,
  companyId,
  status,
  minCgpa,
  page = 0,
  size = 10,
  sort = 'jobId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.JOBS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (title && title.trim()) {
    url += `&title=${encodeURIComponent(title.trim())}`;
  }
  if (companyId && companyId !== 'ALL') {
    url += `&companyId=${companyId}`;
  }
  if (status && status !== 'ALL') {
    url += `&status=${encodeURIComponent(status.trim())}`;
  }
  if (minCgpa != null && minCgpa !== '') {
    url += `&minCgpa=${minCgpa}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single job opening by its ID.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} JobResponse
 */
export async function getJobById(id) {
  const response = await api.get(API_ENDPOINTS.JOBS.BY_ID(id));
  return response.data;
}

/**
 * Updates a job opening. Requires ROLE_ADMIN or owning COMPANY.
 *
 * @param {number|string} id
 * @param {Object} payload - JobRequest DTO
 * @returns {Promise<Object>} Updated JobResponse
 */
export async function updateJob(id, payload) {
  const body = {
    companyId: Number(payload.companyId),
    title: payload.title?.trim(),
    description: payload.description?.trim() || null,
    eligibilityCriteria: payload.eligibilityCriteria?.trim() || null,
    requiredSkills: payload.requiredSkills?.trim() || null,
    minCgpa: payload.minCgpa !== '' && payload.minCgpa != null ? Number(payload.minCgpa) : null,
    packageAmount: payload.packageAmount !== '' && payload.packageAmount != null ? Number(payload.packageAmount) : null,
    jobType: payload.jobType?.trim() || 'FULL_TIME',
    deadline: payload.deadline || null,
    status: payload.status || 'ACTIVE',
  };

  const response = await api.put(API_ENDPOINTS.JOBS.BY_ID(id), body);
  return response.data;
}

/**
 * Closes an active job posting (sets status = CLOSED).
 * Requires ROLE_ADMIN or owning COMPANY.
 *
 * @param {number|string} id
 * @returns {Promise<void>}
 */
export async function closeJob(id) {
  await api.delete(API_ENDPOINTS.JOBS.BY_ID(id));
}

// ============================================================================
// APPLICATION OVERSIGHT
// ============================================================================

/**
 * Retrieves paginated applications platform-wide with query filters.
 * Requires ROLE_ADMIN, ROLE_COMPANY, or ROLE_STUDENT.
 *
 * @param {Object} [params]
 * @param {number|string} [params.studentId]
 * @param {number|string} [params.jobId]
 * @param {string} [params.status] - APPLIED | SHORTLISTED | INTERVIEWING | SELECTED | REJECTED | WITHDRAWN
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='applicationId,desc']
 * @returns {Promise<Object>} Spring Data Page<ApplicationResponse>
 */
export async function fetchAllApplications({
  studentId,
  jobId,
  status,
  page = 0,
  size = 10,
  sort = 'applicationId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.APPLICATIONS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (studentId != null && studentId !== 'ALL' && studentId !== '') {
    url += `&studentId=${studentId}`;
  }
  if (jobId != null && jobId !== 'ALL' && jobId !== '') {
    url += `&jobId=${jobId}`;
  }
  if (status && status !== 'ALL') {
    url += `&status=${encodeURIComponent(status.trim())}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single application record by its ID.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} ApplicationResponse
 */
export async function getApplicationById(id) {
  const response = await api.get(API_ENDPOINTS.APPLICATIONS.BY_ID(id));
  return response.data;
}

/**
 * Updates an application's lifecycle status.
 * Requires ROLE_ADMIN or owning COMPANY.
 *
 * @param {number|string} id
 * @param {string} newStatus - APPLIED, SHORTLISTED, INTERVIEWING, SELECTED, REJECTED, WITHDRAWN
 * @returns {Promise<Object>} Updated ApplicationResponse
 */
export async function updateApplicationStatus(id, newStatus) {
  const url = `${API_ENDPOINTS.APPLICATIONS.STATUS(id)}?status=${encodeURIComponent(newStatus)}`;
  const response = await api.put(url);
  return response.data;
}

// ============================================================================
// PLATFORM ANALYTICS
// ============================================================================

/**
 * Retrieves aggregated institutional placement summary metrics.
 * Requires ROLE_ADMIN.
 *
 * @returns {Promise<Object>} DashboardResponse DTO
 */
export async function fetchDashboardSummary() {
  const response = await api.get(API_ENDPOINTS.DASHBOARD.SUMMARY);
  return response.data;
}

export default {
  fetchStudents,
  getStudentById,
  createStudent,
  updateStudent,
  deactivateStudent,
  getStudentSkills,
  fetchCompanies,
  getCompanyById,
  createCompany,
  updateCompany,
  deactivateCompany,
  fetchAllJobs,
  getJobById,
  updateJob,
  closeJob,
  fetchAllApplications,
  getApplicationById,
  updateApplicationStatus,
  fetchDashboardSummary,
};
