import { Navigate } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

/**
 * DashboardPage (Role Dispatcher)
 * Evaluates the authenticated user's role and redirects them
 * to their designated role-specific dashboard.
 */
function DashboardPage() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem' }}>
        <p style={{ color: 'var(--text-muted)' }}>Redirecting to your dashboard...</p>
      </div>
    );
  }

  if (user?.role === 'ROLE_ADMIN') {
    return <Navigate to="/admin/dashboard" replace />;
  }

  if (user?.role === 'ROLE_COMPANY') {
    return <Navigate to="/company/dashboard" replace />;
  }

  // Default to student dashboard for ROLE_STUDENT
  return <Navigate to="/student/dashboard" replace />;
}

export default DashboardPage;
