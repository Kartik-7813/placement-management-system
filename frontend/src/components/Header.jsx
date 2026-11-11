import { useNavigate } from 'react-router-dom';
import { Menu, LogOut } from 'lucide-react';
import useAuth from '../hooks/useAuth';
import Button from './Button';

/**
 * Header Component
 * Top navigation bar providing mobile sidebar toggle, active user info, and logout action.
 *
 * @param {Object} props
 * @param {function} props.onToggleSidebar - Mobile sidebar toggle handler
 */
function Header({ onToggleSidebar }) {
  const { user, logout } = useAuth();
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
    <header className="dashboard-header">
      <div className="header-left">
        <button
          onClick={onToggleSidebar}
          className="mobile-menu-toggle"
          aria-label="Toggle navigation menu"
        >
          <Menu size={18} />
        </button>
        <span style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-main)' }}>
          Placement Management System
        </span>
      </div>

      <div className="header-right">
        <div className="header-user-info">
          <span className="header-user-email" title={user?.email}>
            {user?.email}
          </span>
          <span className={`badge ${roleBadgeClass}`} style={{ fontSize: '0.6875rem' }}>
            {user?.role}
          </span>
        </div>
        <Button
          type="button"
          variant="secondary"
          onClick={handleLogout}
          style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center' }}
        >
          <LogOut size={14} style={{ marginRight: '0.35rem' }} />
          <span>Logout</span>
        </Button>
      </div>
    </header>
  );
}

export default Header;
