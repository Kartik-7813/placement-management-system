import { createContext, useState, useEffect } from 'react';
import api from '../services/api';
import { API_ENDPOINTS, STORAGE_KEYS } from '../utils/constants';

export const AuthContext = createContext(null);

/**
 * AuthProvider Component
 * Manages global authentication state, session restoration from sessionStorage,
 * and exposes login, register, and logout actions to descendant components.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore authenticated session from sessionStorage on initial page load / refresh
  useEffect(() => {
    try {
      const storedToken = sessionStorage.getItem(STORAGE_KEYS.TOKEN);
      const storedUser = sessionStorage.getItem(STORAGE_KEYS.USER);

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      }
    } catch {
      // Storage parsing failure; clear corrupted data
      sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
      sessionStorage.removeItem(STORAGE_KEYS.USER);
      sessionStorage.removeItem(STORAGE_KEYS.STUDENT_ID);
      sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
    } finally {
      setLoading(false);
    }

    // Sync auth state when an API call encounters a 401 Unauthorized
    const handleUnauthorized = () => {
      try {
        sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
        sessionStorage.removeItem(STORAGE_KEYS.USER);
        sessionStorage.removeItem(STORAGE_KEYS.STUDENT_ID);
        sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
      } catch {
        // Ignore storage exceptions
      }
      setToken(null);
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  /**
   * Authenticates user against POST /api/auth/login, stores JWT in sessionStorage,
   * and updates active React state.
   */
  const login = async (email, password) => {
    // Clear any residual entity IDs from a previous user session
    try {
      sessionStorage.removeItem(STORAGE_KEYS.STUDENT_ID);
      sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
    } catch {
      // Ignore storage errors
    }

    const response = await api.post(API_ENDPOINTS.AUTH.LOGIN, {
      email,
      password,
    });

    const { token: jwtToken, email: userEmail, role, type, expiresInMs } = response.data;
    const userData = { email: userEmail, role };

    sessionStorage.setItem(STORAGE_KEYS.TOKEN, jwtToken);
    sessionStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(userData));

    setToken(jwtToken);
    setUser(userData);

    return { token: jwtToken, email: userEmail, role, type, expiresInMs };
  };

  /**
   * Registers a student profile via POST /api/auth/register.
   */
  const register = async (studentData) => {
    const response = await api.post(API_ENDPOINTS.AUTH.REGISTER, studentData);
    return response.data;
  };

  /**
   * Clears sessionStorage and resets in-memory authentication state.
   */
  const logout = () => {
    try {
      sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
      sessionStorage.removeItem(STORAGE_KEYS.USER);
      sessionStorage.removeItem(STORAGE_KEYS.STUDENT_ID);
      sessionStorage.removeItem(STORAGE_KEYS.COMPANY_ID);
    } catch {
      // Ignore storage errors on logout
    }
    setToken(null);
    setUser(null);
  };

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    loading,
    login,
    logout,
    register,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
