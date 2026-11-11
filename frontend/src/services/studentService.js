import api from './api';
import { API_ENDPOINTS, STORAGE_KEYS } from '../utils/constants';

/**
 * Student Service
 * Provides helper functions for resolving current student profile,
 * caching student ID in sessionStorage, and submitting placement applications.
 */

/**
 * Resolves the authenticated student's database identifier (studentId).
 *
 * Strategy:
 * 1. Check cached student ID in sessionStorage.
 * 2. Query GET /api/applications?size=1 (backend scopes to current student, returning studentId if any exist).
 * 3. Fallback: Concurrently probe GET /api/students/:id in batches of 10.
 *    The backend enforces ownership: matching profile returns 200 OK, mismatches return 403 Forbidden.
 *
 * @returns {Promise<number|null>} The numeric studentId or null if unable to resolve.
 */
export async function resolveCurrentStudentId() {
  try {
    const cached = sessionStorage.getItem(STORAGE_KEYS.STUDENT_ID);
    if (cached) {
      const parsed = Number(cached);
      if (!isNaN(parsed) && parsed > 0) {
        return parsed;
      }
    }

    // 1. Check existing applications
    try {
      const appsRes = await api.get(`${API_ENDPOINTS.APPLICATIONS.BASE}?size=1`);
      const apps = appsRes.data?.content || [];
      if (apps.length > 0 && apps[0].studentId) {
        const studentId = apps[0].studentId;
        sessionStorage.setItem(STORAGE_KEYS.STUDENT_ID, String(studentId));
        return studentId;
      }
    } catch {
      // Ignore and proceed to discovery
    }

    // 2. Discover by probing /api/students/:id in concurrent batches of 10
    // Matching student ID returns HTTP 200, non-matching IDs return 403 or 404
    for (let start = 1; start <= 100; start += 10) {
      const promises = [];
      for (let i = start; i < start + 10; i++) {
        promises.push(
          api.get(API_ENDPOINTS.STUDENTS.BY_ID(i)).then((res) => {
            if (res.data && res.data.studentId) {
              return res.data.studentId;
            }
            throw new Error('No id');
          })
        );
      }

      try {
        const foundId = await Promise.any(promises);
        if (foundId) {
          sessionStorage.setItem(STORAGE_KEYS.STUDENT_ID, String(foundId));
          return foundId;
        }
      } catch {
        // Current batch had no match, continue to next batch
      }
    }
  } catch (err) {
    console.error('Failed to resolve student ID:', err);
  }

  return null;
}

/**
 * Retrieves the full profile of the authenticated student.
 *
 * @returns {Promise<Object|null>} Student profile object or null.
 */
export async function fetchCurrentStudentProfile() {
  const studentId = await resolveCurrentStudentId();
  if (!studentId) {
    return null;
  }
  const response = await api.get(API_ENDPOINTS.STUDENTS.BY_ID(studentId));
  return response.data;
}

/**
 * Submits a new job application on behalf of the authenticated student.
 *
 * @param {Object} params
 * @param {number} params.studentId
 * @param {number} params.jobId
 * @param {string} params.resumeUrl
 * @param {string} [params.remarks]
 * @returns {Promise<Object>} ApplicationResponse
 */
export async function submitApplication({ studentId, jobId, resumeUrl, remarks }) {
  const payload = {
    studentId: Number(studentId),
    jobId: Number(jobId),
    resumeUrl: resumeUrl?.trim(),
    remarks: remarks?.trim() || null,
  };

  const response = await api.post(API_ENDPOINTS.APPLICATIONS.BASE, payload);
  if (response.data && response.data.studentId) {
    sessionStorage.setItem(STORAGE_KEYS.STUDENT_ID, String(response.data.studentId));
  }
  return response.data;
}

export default {
  resolveCurrentStudentId,
  fetchCurrentStudentProfile,
  submitApplication,
};
