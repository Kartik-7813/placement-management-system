import api from './api';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * Application Service
 * Provides backend API integration for placement applications,
 * status transitions, candidate pipeline aggregation, and role-based filtering.
 */

// Permitted recruitment lifecycle transitions based on backend state machine
export const APPLICATION_STATUSES = {
  APPLIED: 'APPLIED',
  SHORTLISTED: 'SHORTLISTED',
  INTERVIEWING: 'INTERVIEWING',
  SELECTED: 'SELECTED',
  REJECTED: 'REJECTED',
  WITHDRAWN: 'WITHDRAWN',
};

export const ALLOWED_STATUS_TRANSITIONS = {
  APPLIED: ['SHORTLISTED', 'REJECTED'],
  SHORTLISTED: ['INTERVIEWING', 'REJECTED'],
  INTERVIEWING: ['SELECTED', 'REJECTED'],
  SELECTED: [],
  REJECTED: [],
  WITHDRAWN: [],
};

/**
 * Checks whether transitioning from currentStatus to targetStatus is valid.
 *
 * @param {string} currentStatus
 * @param {string} targetStatus
 * @returns {boolean}
 */
export function isValidTransition(currentStatus, targetStatus) {
  if (!currentStatus || !targetStatus) return false;
  if (currentStatus === targetStatus) return true;
  const allowed = ALLOWED_STATUS_TRANSITIONS[currentStatus] || [];
  return allowed.includes(targetStatus);
}

/**
 * Retrieves paginated applications with optional server-side filtering.
 *
 * @param {Object} [params]
 * @param {number} [params.jobId]
 * @param {number} [params.studentId]
 * @param {string} [params.status]
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='applicationId,desc']
 * @returns {Promise<Object>} Spring Data Page<ApplicationResponse>
 */
export async function fetchApplications({
  jobId,
  studentId,
  status,
  page = 0,
  size = 10,
  sort = 'applicationId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.APPLICATIONS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (jobId != null && jobId !== '') {
    url += `&jobId=${jobId}`;
  }
  if (studentId != null && studentId !== '') {
    url += `&studentId=${studentId}`;
  }
  if (status && status !== 'ALL') {
    url += `&status=${status}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single application record by its unique identifier.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} ApplicationResponse DTO
 */
export async function fetchApplicationById(id) {
  const response = await api.get(API_ENDPOINTS.APPLICATIONS.BY_ID(id));
  return response.data;
}

/**
 * Updates the recruitment lifecycle status of an application.
 * Backend enforces company ownership (recruiter's company must own the job) or ADMIN authority.
 *
 * @param {number|string} applicationId
 * @param {string} newStatus - APPLIED, SHORTLISTED, INTERVIEWING, SELECTED, REJECTED, WITHDRAWN
 * @returns {Promise<Object>} Updated ApplicationResponse DTO
 */
export async function updateApplicationStatus(applicationId, newStatus) {
  const url = `${API_ENDPOINTS.APPLICATIONS.STATUS(applicationId)}?status=${encodeURIComponent(newStatus)}`;
  const response = await api.put(url);
  return response.data;
}

/**
 * Retrieves all applications belonging to the company's published jobs.
 *
 * Note on company data scoping:
 * Backend GET /api/applications accepts ?jobId= but does not accept ?companyId=.
 * To guarantee strict tenant isolation and retrieve all candidates for a company's jobs:
 * 1. If a specific jobId is selected, fetches directly with ?jobId=...
 * 2. If 'All Company Jobs' is selected, queries each owned job in parallel and aggregates the results.
 *
 * @param {Array<Object>} companyJobs - List of JobResponse objects owned by company
 * @param {Object} [options]
 * @param {number|string} [options.jobId] - Filter by specific job ID
 * @param {string} [options.status] - Filter by status
 * @param {string} [options.search] - Search text (candidate name / job title)
 * @returns {Promise<Array<Object>>} Aggregated applications list
 */
export async function fetchCompanyPipelineApplications(companyJobs, { jobId, status, search } = {}) {
  if (!companyJobs || companyJobs.length === 0) {
    return [];
  }

  let rawList = [];

  if (jobId && jobId !== 'ALL') {
    // Specific job selected - single query
    const res = await fetchApplications({
      jobId: Number(jobId),
      status: status !== 'ALL' ? status : undefined,
      size: 100,
    });
    rawList = res.content || [];
  } else {
    // Query all owned jobs concurrently (up to 50 jobs per company)
    const promises = companyJobs.map((job) =>
      fetchApplications({
        jobId: job.jobId,
        status: status !== 'ALL' ? status : undefined,
        size: 100,
      }).then((res) => res.content || []).catch(() => [])
    );

    const results = await Promise.all(promises);
    rawList = results.flat();
  }

  // De-duplicate by applicationId (in case of overlap) and sort newest first
  const uniqueMap = new Map();
  for (const app of rawList) {
    if (app && app.applicationId && !uniqueMap.has(app.applicationId)) {
      uniqueMap.set(app.applicationId, app);
    }
  }

  let list = Array.from(uniqueMap.values());
  list.sort((a, b) => b.applicationId - a.applicationId);

  // Client-side text filter if search provided
  if (search && search.trim()) {
    const q = search.trim().toLowerCase();
    list = list.filter((app) => {
      const studentMatch = app.studentName?.toLowerCase().includes(q);
      const jobMatch = app.jobTitle?.toLowerCase().includes(q);
      const remarksMatch = app.remarks?.toLowerCase().includes(q);
      return studentMatch || jobMatch || remarksMatch;
    });
  }

  return list;
}

/**
 * Calculates aggregated recruitment pipeline metrics across all company applications.
 *
 * @param {Array<Object>} applications - All applications belonging to company's jobs
 * @returns {Object} Metric counts
 */
export function calculatePipelineMetrics(applications = []) {
  const metrics = {
    total: applications.length,
    applied: 0,
    shortlisted: 0,
    interviewing: 0,
    selected: 0,
    rejected: 0,
    withdrawn: 0,
  };

  for (const app of applications) {
    switch (app.status) {
      case 'APPLIED':
        metrics.applied++;
        break;
      case 'SHORTLISTED':
        metrics.shortlisted++;
        break;
      case 'INTERVIEWING':
        metrics.interviewing++;
        break;
      case 'SELECTED':
        metrics.selected++;
        break;
      case 'REJECTED':
        metrics.rejected++;
        break;
      case 'WITHDRAWN':
        metrics.withdrawn++;
        break;
      default:
        break;
    }
  }

  return metrics;
}

export default {
  APPLICATION_STATUSES,
  ALLOWED_STATUS_TRANSITIONS,
  isValidTransition,
  fetchApplications,
  fetchApplicationById,
  updateApplicationStatus,
  fetchCompanyPipelineApplications,
  calculatePipelineMetrics,
};
