import { Outlet, Link, useNavigate } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

/**
 * MainLayout
 * Primary application layout displaying dynamic navigation based on authentication state,
 * active user info, role badge, and logout action.
 */
function MainLayout() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const roleBadgeClass =
    user?.role === 'ROLE_ADMIN'
      ? 'badge-info'
      : user?.role === 'ROLE_COMPANY'
      ? 'badge-warning'
      : 'badge-success';

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="brand-title">
          <Link to="/">Placement Management System</Link>
        </div>
        <nav>
          <ul className="nav-links" style={{ alignItems: 'center' }}>
            <li>
              <Link to="/">Home</Link>
            </li>

            {isAuthenticated ? (
              <>
                <li>
                  <Link to="/dashboard">Dashboard</Link>
                </li>
                <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginLeft: '0.5rem' }}>
                  <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)' }}>
                    {user?.email}
                  </span>
                  <span className={`badge ${roleBadgeClass}`} style={{ fontSize: '0.7rem' }}>
                    {user?.role}
                  </span>
                </li>
                <li>
                  <button
                    onClick={handleLogout}
                    className="btn btn-secondary"
                    style={{ padding: '0.25rem 0.75rem', fontSize: '0.8125rem' }}
                    title="Sign out of current session"
                  >
                    Logout
                  </button>
                </li>
              </>
            ) : (
              <>
                <li>
                  <Link to="/register">Student Registration</Link>
                </li>
                <li>
                  <Link to="/login">Sign In</Link>
                </li>
              </>
            )}
          </ul>
        </nav>
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <footer className="app-footer">
        <p>Placement Management System &copy; {new Date().getFullYear()} — Java 21 / Spring Boot 4 + React / Vite</p>
      </footer>
    </div>
  );
}

export default MainLayout;
