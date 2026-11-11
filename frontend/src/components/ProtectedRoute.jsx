import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import useAuth from '../hooks/useAuth';

/**
 * ProtectedRoute Component
 * Guards routes against unauthenticated access and enforces optional role-based authorization.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - Child element/component to render if authorized
 * @param {string[]} [props.allowedRoles] - Optional list of permitted roles (e.g. ['ROLE_STUDENT'])
 */
function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem' }}>
        <p style={{ color: 'var(--text-muted)' }}>Verifying authentication session...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect unauthenticated user to /login while preserving requested path
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check role-based access if allowedRoles is specified
  if (allowedRoles && allowedRoles.length > 0) {
    const hasRole = user && allowedRoles.includes(user.role);
    if (!hasRole) {
      return (
        <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', maxWidth: '600px', margin: '2rem auto' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
            <ShieldAlert size={48} strokeWidth={1.5} color="var(--danger-color)" />
          </div>
          <h2 style={{ color: 'var(--danger-color)', marginBottom: '0.75rem' }}>403 — Access Denied</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
            Your account role (<strong>{user?.role}</strong>) does not have permission to view this section.
          </p>
          <a href="/" className="btn btn-secondary">
            Return to Home
          </a>
        </div>
      );
    }
  }

  return children;
}

export default ProtectedRoute;
