import api from './api';
import { API_ENDPOINTS, STORAGE_KEYS } from '../utils/constants';

/**
 * Company Service
 * Handles company profile management, organization job postings,
 * drive updates, and company recruitment metrics.
 */

/**
 * Resolves the authenticated recruiter's associated Company profile.
 *
 * Since the backend links the recruiter to a company via `users.company_id`
 * but does not expose a `/api/companies/me` endpoint:
 * 1. Checks sessionStorage cache for a previously verified companyId.
 * 2. Fetches public company listings from `GET /api/companies`.
 * 3. Matches by recruiter email or institutional domain.
 * 4. Caches verified companyId in sessionStorage for subsequent fast lookups.
 *
 * @param {string} userEmail - Authenticated user email
 * @returns {Promise<Object|null>} CompanyResponse DTO or null
 */
export async function fetchMyCompanyProfile(userEmail) {
  try {
    // 1. Check cached companyId in sessionStorage
    const cachedId = sessionStorage.getItem(STORAGE_KEYS.COMPANY_ID);
    if (cachedId) {
      try {
        const cachedRes = await api.get(API_ENDPOINTS.COMPANIES.BY_ID(cachedId));
        if (cachedRes.data) {
          return cachedRes.data;
        }
      } catch (cErr) {
        // Cache invalid or expired; clear and re-resolve
        sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
      }
    }

    // 2. Fetch companies list
    const res = await api.get(`${API_ENDPOINTS.COMPANIES.BASE}?size=100&sort=companyId,asc`);
    const companies = res.data?.content || [];

    if (companies.length === 0) {
      return null;
    }

    let matched = null;

    // 3. Match by exact email
    if (userEmail) {
      const lowerEmail = userEmail.toLowerCase();
      matched = companies.find(
        (c) => c.email && c.email.toLowerCase() === lowerEmail
      );

      // 4. Match by domain if no exact match (e.g. recruiter@techsolutions.com -> hr@techsolutions.com)
      if (!matched && lowerEmail.includes('@')) {
        const userDomain = lowerEmail.split('@')[1];
        matched = companies.find((c) => {
          if (!c.email || !c.email.includes('@')) return false;
          return c.email.toLowerCase().split('@')[1] === userDomain;
        });
      }
    }

    // 5. If only 1 company exists on the platform, resolve to that single organization
    if (!matched && companies.length === 1) {
      matched = companies[0];
    }

    // 6. Persist companyId for session
    if (matched?.companyId) {
      sessionStorage.setItem(STORAGE_KEYS.COMPANY_ID, String(matched.companyId));
    }

    return matched;
  } catch (err) {
    console.error('Failed to resolve recruiter company profile:', err);
    throw err;
  }
}

/**
 * Retrieves a company record by ID.
 *
 * @param {number|string} companyId
 * @returns {Promise<Object>} CompanyResponse DTO
 */
export async function getCompanyById(companyId) {
  const response = await api.get(API_ENDPOINTS.COMPANIES.BY_ID(companyId));
  return response.data;
}

/**
 * Updates an existing company's profile information.
 * Enforces ownership on backend: company recruiters can only edit their own company.
 *
 * @param {number|string} companyId
 * @param {Object} payload - CompanyRequest fields
 * @returns {Promise<Object>} Updated CompanyResponse DTO
 */
export async function updateCompanyProfile(companyId, payload) {
  const body = {
    name: payload.name?.trim(),
    website: payload.website?.trim() || null,
    contactPerson: payload.contactPerson?.trim() || null,
    email: payload.email?.trim() || null,
    phone: payload.phone?.trim() || null,
    address: payload.address?.trim() || null,
    status: payload.status || 'ACTIVE',
  };

  const response = await api.put(API_ENDPOINTS.COMPANIES.BY_ID(companyId), body);
  return response.data;
}

/**
 * Retrieves paginated job postings filtered to a specific company.
 *
 * @param {number|string} companyId
 * @param {Object} [params]
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.status]
 * @param {string} [params.title]
 * @param {string} [params.sort='jobId,desc']
 * @returns {Promise<Object>} Spring Data Page<JobResponse>
 */
export async function fetchCompanyJobs(companyId, {
  page = 0,
  size = 10,
  status,
  title,
  sort = 'jobId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.JOBS.BASE}?companyId=${companyId}&page=${page}&size=${size}&sort=${sort}`;
  if (status) {
    url += `&status=${status}`;
  }
  if (title && title.trim()) {
    url += `&title=${encodeURIComponent(title.trim())}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Posts a new job opening for the recruiting company.
 *
 * @param {Object} payload - JobRequest fields
 * @returns {Promise<Object>} Created JobResponse DTO
 */
export async function createCompanyJob(payload) {
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

  const response = await api.post(API_ENDPOINTS.JOBS.BASE, body);
  return response.data;
}

/**
 * Updates an existing job posting.
 * Enforces ownership on backend: company recruiters can only edit their own jobs.
 *
 * @param {number|string} jobId
 * @param {Object} payload - JobRequest fields
 * @returns {Promise<Object>} Updated JobResponse DTO
 */
export async function updateCompanyJob(jobId, payload) {
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

  const response = await api.put(API_ENDPOINTS.JOBS.BY_ID(jobId), body);
  return response.data;
}

/**
 * Closes an active job posting so it no longer accepts applications.
 * Uses the backend's DELETE /api/jobs/{id} endpoint which sets status to CLOSED.
 *
 * @param {number|string} jobId
 * @returns {Promise<void>}
 */
export async function closeCompanyJob(jobId) {
  await api.delete(API_ENDPOINTS.JOBS.BY_ID(jobId));
}

/**
 * Calculates verified company-specific recruitment drive metrics
 * by querying only this company's jobs from GET /api/jobs?companyId={id}.
 *
 * @param {number|string} companyId
 * @returns {Promise<Object>} Computed company metrics
 */
export async function fetchCompanyMetrics(companyId) {
  try {
    const res = await api.get(`${API_ENDPOINTS.JOBS.BASE}?companyId=${companyId}&size=100`);
    const jobs = res.data?.content || [];

    const now = new Date();
    const sevenDaysFromNow = new Date();
    sevenDaysFromNow.setDate(now.getDate() + 7);

    const totalJobs = jobs.length;
    const activeJobs = jobs.filter((j) => j.status === 'ACTIVE').length;
    const closedJobs = jobs.filter((j) => j.status === 'CLOSED').length;
    const nearingDeadline = jobs.filter((j) => {
      if (j.status !== 'ACTIVE' || !j.deadline) return false;
      const d = new Date(j.deadline);
      return d >= now && d <= sevenDaysFromNow;
    }).length;

    return {
      totalJobs,
      activeJobs,
      closedJobs,
      nearingDeadline,
      recentJobs: jobs.slice(0, 5),
    };
  } catch (err) {
    console.error('Failed to compute company metrics:', err);
    throw err;
  }
}

export default {
  fetchMyCompanyProfile,
  getCompanyById,
  updateCompanyProfile,
  fetchCompanyJobs,
  createCompanyJob,
  updateCompanyJob,
  closeCompanyJob,
  fetchCompanyMetrics,
};
