import axios from 'axios';
import { STORAGE_KEYS } from '../utils/constants';

/**
 * Base Axios Client Instance
 * Configured with backend base URL, timeouts, and request/response interceptors.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 90000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

/**
 * Request Interceptor:
 * Automatically injects the JWT Bearer token into the Authorization header
 * for all authenticated backend requests using sessionStorage.
 */
api.interceptors.request.use(
  (config) => {
    try {
      const token = sessionStorage.getItem(STORAGE_KEYS.TOKEN);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Gracefully continue if sessionStorage is inaccessible
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response Interceptor:
 * Standardizes API responses and handles 401 Unauthorized by clearing
 * expired tokens from sessionStorage and notifying auth listeners.
 */
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // If backend returns a 401 Unauthorized, token is expired or invalid
    if (error.response && error.response.status === 401) {
      try {
        sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
        sessionStorage.removeItem(STORAGE_KEYS.USER);
        sessionStorage.removeItem(STORAGE_KEYS.STUDENT_ID);
        sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
        window.dispatchEvent(new Event('auth:unauthorized'));
      } catch {
        // Ignore storage exceptions
      }
    }
    return Promise.reject(error);
  }
);

export default api;
