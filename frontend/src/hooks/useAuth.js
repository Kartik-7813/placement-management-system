import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

/**
 * useAuth Hook
 * Custom hook to consume AuthContext cleanly across components.
 * Throws a descriptive error if accessed outside an AuthProvider.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default useAuth;
