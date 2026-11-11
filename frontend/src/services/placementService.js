import api from './api';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * Placement Service
 * Provides complete REST API integration for campus placement records:
 * - Admin placement directory & status filtering
 * - Confirmed placement recording (POST /api/placements)
 * - Placement details modification (PUT /api/placements/{id})
 * - Placement status transitions (CONFIRMED -> JOINED | CANCELLED)
 * - Student-specific placement visibility (GET /api/placements/student/{studentId})
 */

export const PLACEMENT_STATUSES = {
  CONFIRMED: 'CONFIRMED',
  JOINED: 'JOINED',
  CANCELLED: 'CANCELLED',
};

// Permitted lifecycle transitions based on backend PlacementService.validateStatusTransition
export const ALLOWED_PLACEMENT_TRANSITIONS = {
  CONFIRMED: ['JOINED', 'CANCELLED'],
  JOINED: [],
  CANCELLED: [],
};

/**
 * Validates whether transitioning from currentStatus to targetStatus is valid.
 *
 * @param {string} currentStatus
 * @param {string} targetStatus
 * @returns {boolean}
 */
export function isValidPlacementTransition(currentStatus, targetStatus) {
  if (!currentStatus || !targetStatus) return false;
  if (currentStatus === targetStatus) return true;
  const allowed = ALLOWED_PLACEMENT_TRANSITIONS[currentStatus] || [];
  return allowed.includes(targetStatus);
}

/**
 * Retrieves paginated placement records with optional status filter.
 * Requires ROLE_ADMIN or ROLE_COMPANY.
 *
 * @param {Object} [params]
 * @param {string} [params.status] - CONFIRMED | JOINED | CANCELLED
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @param {string} [params.sort='placementId,desc']
 * @returns {Promise<Object>} Spring Data Page<PlacementResponse>
 */
export async function fetchPlacements({
  status,
  page = 0,
  size = 10,
  sort = 'placementId,desc',
} = {}) {
  let url = `${API_ENDPOINTS.PLACEMENTS.BASE}?page=${page}&size=${size}&sort=${sort}`;
  if (status && status !== 'ALL') {
    url += `&status=${encodeURIComponent(status.trim())}`;
  }

  const response = await api.get(url);
  return response.data;
}

/**
 * Retrieves a single placement record by primary key ID.
 * Requires ROLE_ADMIN, ROLE_COMPANY, or placed STUDENT.
 *
 * @param {number|string} id
 * @returns {Promise<Object>} PlacementResponse
 */
export async function getPlacementById(id) {
  const response = await api.get(API_ENDPOINTS.PLACEMENTS.BY_ID(id));
  return response.data;
}

/**
 * Retrieves the active (CONFIRMED/JOINED) or most recent placement for a student.
 * Requires ROLE_ADMIN, ROLE_COMPANY, or target STUDENT.
 *
 * @param {number|string} studentId
 * @returns {Promise<Object>} PlacementResponse
 */
export async function getPlacementByStudent(studentId) {
  const response = await api.get(API_ENDPOINTS.PLACEMENTS.BY_STUDENT(studentId));
  return response.data;
}

/**
 * Retrieves full placement history for a student ordered by placedOn desc.
 * Requires ROLE_ADMIN, ROLE_COMPANY, or target STUDENT.
 *
 * @param {number|string} studentId
 * @returns {Promise<Array<Object>>} List of PlacementResponse
 */
export async function getPlacementHistoryByStudent(studentId) {
  const response = await api.get(API_ENDPOINTS.PLACEMENTS.STUDENT_HISTORY(studentId));
  return response.data || [];
}

/**
 * Records a new confirmed placement for a SELECTED application.
 * Requires ROLE_ADMIN.
 *
 * @param {Object} payload - PlacementRequest DTO
 * @param {number} payload.studentId - Required
 * @param {number} payload.applicationId - Required
 * @param {string} payload.jobRole - Required (max 150)
 * @param {number|string} [payload.packageAmount] - Optional non-negative decimal
 * @param {string} [payload.joiningDate] - Optional YYYY-MM-DD
 * @returns {Promise<Object>} PlacementResponse (initial status CONFIRMED)
 */
export async function createPlacement(payload) {
  const body = {
    studentId: Number(payload.studentId),
    applicationId: Number(payload.applicationId),
    jobRole: payload.jobRole?.trim(),
    packageAmount: payload.packageAmount !== '' && payload.packageAmount != null ? Number(payload.packageAmount) : null,
    joiningDate: payload.joiningDate || null,
  };

  const response = await api.post(API_ENDPOINTS.PLACEMENTS.BASE, body);
  return response.data;
}

/**
 * Updates details of an existing placement record (role, package, joining date).
 * Student and application relationships are immutable.
 * Requires ROLE_ADMIN.
 *
 * @param {number|string} id
 * @param {Object} payload - PlacementRequest DTO
 * @returns {Promise<Object>} Updated PlacementResponse
 */
export async function updatePlacement(id, payload) {
  const body = {
    studentId: Number(payload.studentId),
    applicationId: Number(payload.applicationId),
    jobRole: payload.jobRole?.trim(),
    packageAmount: payload.packageAmount !== '' && payload.packageAmount != null ? Number(payload.packageAmount) : null,
    joiningDate: payload.joiningDate || null,
  };

  const response = await api.put(API_ENDPOINTS.PLACEMENTS.BY_ID(id), body);
  return response.data;
}

/**
 * Transitions placement status (CONFIRMED -> JOINED | CANCELLED).
 * Requires ROLE_ADMIN.
 *
 * @param {number|string} id
 * @param {string} newStatus - JOINED | CANCELLED
 * @returns {Promise<Object>} Updated PlacementResponse
 */
export async function updatePlacementStatus(id, newStatus) {
  const url = `${API_ENDPOINTS.PLACEMENTS.STATUS(id)}?status=${encodeURIComponent(newStatus)}`;
  const response = await api.put(url);
  return response.data;
}

export default {
  PLACEMENT_STATUSES,
  ALLOWED_PLACEMENT_TRANSITIONS,
  isValidPlacementTransition,
  fetchPlacements,
  getPlacementById,
  getPlacementByStudent,
  getPlacementHistoryByStudent,
  createPlacement,
  updatePlacement,
  updatePlacementStatus,
};
