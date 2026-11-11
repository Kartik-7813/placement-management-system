import api from './api';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * Interview Service
 * Provides helper methods for:
 * 1. Student interview tracking (Milestone 9.7.1)
 * 2. Recruiter & Admin interview scheduling and round management (Milestone 9.7.2)
 * using verified backend REST APIs.
 */

/**
 * Fetches all interview rounds scheduled for the authenticated student.
 *
 * Since the backend restricts `GET /api/interviews` to ADMIN and COMPANY roles,
 * students retrieve their interview schedules by:
 * 1. Fetching all applications belonging to the student (`GET /api/applications`).
 * 2. Concurrently querying `GET /api/interviews/application/{applicationId}` for each application.
 * 3. Enriching each interview record with related job and company metadata.
 *
 * @returns {Promise<{ interviews: Array<Object>, applications: Array<Object> }>}
 */
export async function fetchStudentInterviews() {
  try {
    const appsRes = await api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?size=100`);
    const applications = appsRes.data?.content || [];

    if (applications.length === 0) {
      return { interviews: [], applications: [] };
    }

    const interviewPromises = applications.map(async (app) => {
      try {
        const ivRes = await api.get(
          API_ENDPOINTS.INTERVIEWS.BY_APPLICATION(app.applicationId)
        );
        const rounds = ivRes.data || [];

        return rounds.map((iv) => ({
          ...iv,
          jobId: app.jobId,
          jobTitle: app.jobTitle,
          companyName: app.companyName,
          studentId: app.studentId,
          studentName: app.studentName,
          applicationStatus: app.status,
          appliedOn: app.appliedOn,
        }));
      } catch (err) {
        console.warn(`Could not load interviews for application #${app.applicationId}:`, err);
        return [];
      }
    });

    const settled = await Promise.all(interviewPromises);
    const flattened = settled.flat();

    flattened.sort((a, b) => {
      if (!a.scheduledOn) return 1;
      if (!b.scheduledOn) return -1;
      return new Date(b.scheduledOn) - new Date(a.scheduledOn);
    });

    return {
      interviews: flattened,
      applications,
    };
  } catch (err) {
    console.error('Failed to fetch student interviews:', err);
    throw err;
  }
}

/**
 * Retrieves a paginated list of all interviews with optional filters.
 * Requires ADMIN or COMPANY role.
 *
 * @param {Object} [params]
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {number} [params.applicationId]
 * @param {string} [params.result]
 * @param {string} [params.sort='interviewId,desc']
 * @returns {Promise<Object>} Spring Data Page<InterviewResponse>
 */
export async function fetchAllInterviews({
  page = 0,
  size = 10,
  applicationId,
  result,
  sort = 'interviewId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.INTERVIEWS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (applicationId) {
    url += `&applicationId=${applicationId}`;
  }
  if (result) {
    url += `&result=${result}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single interview record by its primary key identifier.
 * Requires ADMIN, owning COMPANY, or applicant STUDENT role.
 *
 * @param {number|string} interviewId
 * @returns {Promise<Object>} InterviewResponse DTO
 */
export async function fetchInterviewById(interviewId) {
  const response = await api.get(API_ENDPOINTS.INTERVIEWS.BY_ID(interviewId));
  return response.data;
}

/**
 * Retrieves all interview rounds for a specific job application.
 * Requires ADMIN, owning COMPANY, or applicant STUDENT role.
 *
 * @param {number|string} applicationId
 * @returns {Promise<Array<Object>>} List of InterviewResponse DTOs
 */
export async function fetchInterviewsByApplication(applicationId) {
  const response = await api.get(
    API_ENDPOINTS.INTERVIEWS.BY_APPLICATION(applicationId)
  );
  return response.data;
}

/**
 * Schedules a new interview round for an application.
 * Requires ADMIN or owning COMPANY role.
 * Application must be in SHORTLISTED or INTERVIEWING status.
 *
 * @param {Object} payload
 * @param {number} payload.applicationId
 * @param {number} payload.roundNumber
 * @param {string} payload.roundType (e.g. 'TECHNICAL', 'HR', 'CODING')
 * @param {string} payload.scheduledOn (ISO-8601 YYYY-MM-DDTHH:mm:ss)
 * @param {string} [payload.interviewer]
 * @param {string} [payload.feedback]
 * @param {string} [payload.result='PENDING']
 * @returns {Promise<Object>} InterviewResponse DTO
 */
export async function scheduleInterview(payload) {
  const body = {
    applicationId: Number(payload.applicationId),
    roundNumber: Number(payload.roundNumber),
    roundType: payload.roundType?.trim(),
    scheduledOn: payload.scheduledOn,
    interviewer: payload.interviewer?.trim() || null,
    feedback: payload.feedback?.trim() || null,
    result: payload.result || 'PENDING',
  };

  const response = await api.post(API_ENDPOINTS.INTERVIEWS.BASE, body);
  return response.data;
}

/**
 * Updates an existing interview round's scheduling specifications.
 * Requires ADMIN or owning COMPANY role.
 *
 * @param {number|string} id
 * @param {Object} payload
 * @param {number} payload.applicationId
 * @param {number} payload.roundNumber
 * @param {string} payload.roundType
 * @param {string} payload.scheduledOn
 * @param {string} [payload.interviewer]
 * @param {string} [payload.feedback]
 * @param {string} [payload.result]
 * @returns {Promise<Object>} InterviewResponse DTO
 */
export async function updateInterview(id, payload) {
  const body = {
    applicationId: Number(payload.applicationId),
    roundNumber: Number(payload.roundNumber),
    roundType: payload.roundType?.trim(),
    scheduledOn: payload.scheduledOn,
    interviewer: payload.interviewer?.trim() || null,
    feedback: payload.feedback !== undefined ? payload.feedback : null,
    result: payload.result || 'PENDING',
  };

  const response = await api.put(API_ENDPOINTS.INTERVIEWS.BY_ID(id), body);
  return response.data;
}

/**
 * Submits an evaluation outcome (PENDING, PASSED, FAILED) and assessment feedback
 * for a scheduled interview round via PUT /api/interviews/{id}.
 *
 * Preserves all mandatory interview specifications (applicationId, roundNumber,
 * roundType, scheduledOn, interviewer) so they are not overwritten.
 *
 * Requires ADMIN or owning COMPANY role.
 *
 * @param {number|string} id - Interview primary key identifier
 * @param {Object} existingInterview - Complete existing interview record
 * @param {Object} evaluation - Evaluator outcome and notes
 * @param {string} evaluation.result - 'PENDING' | 'PASSED' | 'FAILED'
 * @param {string} [evaluation.feedback] - Evaluator notes (max 2000 chars)
 * @returns {Promise<Object>} InterviewResponse DTO
 */
export async function submitEvaluation(id, existingInterview, { result, feedback }) {
  const body = {
    applicationId: Number(existingInterview.applicationId),
    roundNumber: Number(existingInterview.roundNumber),
    roundType: existingInterview.roundType,
    scheduledOn: existingInterview.scheduledOn,
    interviewer: existingInterview.interviewer || null,
    feedback: feedback !== undefined ? (feedback?.trim() || null) : null,
    result: result || 'PENDING',
  };

  const response = await api.put(API_ENDPOINTS.INTERVIEWS.BY_ID(id), body);
  return response.data;
}

/**
 * Deletes an interview record.
 * Requires ADMIN role.
 *
 * @param {number|string} id
 * @returns {Promise<void>}
 */
export async function deleteInterview(id) {
  const response = await api.delete(API_ENDPOINTS.INTERVIEWS.BY_ID(id));
  return response.data;
}

/**
 * Retrieves eligible applications that can be scheduled for an interview.
 * The backend enforces that only applications with status SHORTLISTED or INTERVIEWING
 * can have interview rounds created.
 *
 * For COMPANY users, this matches applications against the company's profile.
 * For ADMIN users, applications across all companies are returned.
 *
 * @param {string} [userRole]
 * @param {string} [userEmail]
 * @returns {Promise<Array<Object>>} List of eligible ApplicationResponse objects
 */
export async function fetchEligibleApplications(userRole, userEmail) {
  try {
    const shortlistedPromise = api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?status=SHORTLISTED&size=100`);
    const interviewingPromise = api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?status=INTERVIEWING&size=100`);

    const [shortlistedRes, interviewingRes] = await Promise.all([shortlistedPromise, interviewingPromise]);

    const combined = [
      ...(shortlistedRes.data?.content || []),
      ...(interviewingRes.data?.content || []),
    ];

    // For company recruiters, filter applications to their own company
    if (userRole === 'ROLE_COMPANY' && userEmail) {
      try {
        const companiesRes = await api.get(`${API_ENDPOINTS.COMPANIES.BASE}?size=100`);
        const companies = companiesRes.data?.content || [];
        const myCompany = companies.find(
          (c) => c.email?.toLowerCase() === userEmail.toLowerCase()
        );
        if (myCompany) {
          return combined.filter(
            (app) => app.companyName?.toLowerCase() === myCompany.name?.toLowerCase()
          );
        }
      } catch (cErr) {
        console.warn('Could not filter applications by recruiter company email:', cErr);
      }
    }

    return combined;
  } catch (err) {
    console.error('Failed to fetch eligible applications for scheduling:', err);
    return [];
  }
}

export default {
  fetchStudentInterviews,
  fetchAllInterviews,
  fetchInterviewById,
  fetchInterviewsByApplication,
  scheduleInterview,
  updateInterview,
  submitEvaluation,
  deleteInterview,
  fetchEligibleApplications,
};
