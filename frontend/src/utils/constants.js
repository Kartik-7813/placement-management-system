/**
 * Application Constants
 * Placement Management System
 */

// User Roles matching backend Spring Security authorities
export const ROLES = {
  ADMIN: 'ROLE_ADMIN',
  STUDENT: 'ROLE_STUDENT',
  COMPANY: 'ROLE_COMPANY',
};

// LocalStorage Keys for authentication session persistence
export const STORAGE_KEYS = {
  TOKEN: 'pms_token',
  USER: 'pms_user',
  STUDENT_ID: 'pms_student_id',
  COMPANY_ID: 'pms_company_id',
};

// Backend REST API Endpoints
export const API_ENDPOINTS = {
  // Authentication
  AUTH: {
    LOGIN: '/api/auth/login',
    REGISTER: '/api/auth/register',
  },
  // Students
  STUDENTS: {
    BASE: '/api/students',
    BY_ID: (id) => `/api/students/${id}`,
    SKILLS: (id) => `/api/students/${id}/skills`,
    ASSIGN_SKILL: (studentId, skillId) => `/api/students/${studentId}/skills/${skillId}`,
    REMOVE_SKILL: (studentId, skillId) => `/api/students/${studentId}/skills/${skillId}`,
  },
  // Companies
  COMPANIES: {
    BASE: '/api/companies',
    BY_ID: (id) => `/api/companies/${id}`,
  },
  // Jobs
  JOBS: {
    BASE: '/api/jobs',
    BY_ID: (id) => `/api/jobs/${id}`,
  },
  // Applications
  APPLICATIONS: {
    BASE: '/api/applications',
    BY_ID: (id) => `/api/applications/${id}`,
    STATUS: (id) => `/api/applications/${id}/status`,
  },
  // Interviews
  INTERVIEWS: {
    BASE: '/api/interviews',
    BY_ID: (id) => `/api/interviews/${id}`,
    BY_APPLICATION: (applicationId) => `/api/interviews/application/${applicationId}`,
  },
  // Placements
  PLACEMENTS: {
    BASE: '/api/placements',
    BY_ID: (id) => `/api/placements/${id}`,
    BY_STUDENT: (studentId) => `/api/placements/student/${studentId}`,
    STUDENT_HISTORY: (studentId) => `/api/placements/student/${studentId}/history`,
    STATUS: (id) => `/api/placements/${id}/status`,
  },
  // Dashboard
  DASHBOARD: {
    SUMMARY: '/api/dashboard/summary',
  },
  // Actuator
  ACTUATOR: {
    HEALTH: '/actuator/health',
  },
};
