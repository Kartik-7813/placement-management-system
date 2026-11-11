import { NavLink, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  Briefcase,
  FileText,
  CalendarDays,
  Award,
  Users,
  Building2,
  GraduationCap,
  Home,
  X,
} from 'lucide-react';
import useAuth from '../hooks/useAuth';

/**
 * Sidebar Component
 * Renders role-specific navigation menu, user badge, and brand logo.
 *
 * @param {Object} props
 * @param {boolean} props.isOpen - Mobile drawer visibility
 * @param {function} props.onClose - Mobile drawer close handler
 */
function Sidebar({ isOpen, onClose }) {
  const { user } = useAuth();

  const role = user?.role;

  const roleBadgeClass =
    role === 'ROLE_ADMIN'
      ? 'badge-info'
      : role === 'ROLE_COMPANY'
        ? 'badge-warning'
        : 'badge-success';

  const roleDisplay =
    role === 'ROLE_ADMIN'
      ? 'Administrator'
      : role === 'ROLE_COMPANY'
        ? 'Company Recruiter'
        : 'Student';

  return (
    <aside className={`dashboard-sidebar ${isOpen ? 'sidebar-open' : ''}`}>
      <div className="sidebar-header">
        <Link to="/" className="sidebar-brand" onClick={onClose}>
          <span className="sidebar-brand-icon">P</span>
          <span>PMS Portal</span>
        </Link>
        <button
          onClick={onClose}
          className="mobile-menu-toggle"
          aria-label="Close navigation sidebar"
        >
          <X size={18} />
        </button>
      </div>

      <div className="sidebar-user-preview">
        <div className="sidebar-user-email" title={user?.email}>
          {user?.email}
        </div>
        <span className={`badge ${roleBadgeClass}`} style={{ fontSize: '0.6875rem' }}>
          {roleDisplay}
        </span>
      </div>

      <nav className="sidebar-nav">
        {/* Role Specific Navigation */}
        <div className="sidebar-section-title">Navigation</div>
        <ul className="sidebar-menu">
          {role === 'ROLE_STUDENT' && (
            <>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/student/dashboard"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <LayoutDashboard size={18} />
                  </span>
                  <span>Dashboard</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/student/jobs"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Briefcase size={18} />
                  </span>
                  <span>Browse Jobs</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/student/applications"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <FileText size={18} />
                  </span>
                  <span>My Applications</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/student/interviews"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <CalendarDays size={18} />
                  </span>
                  <span>Interviews</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/student/placement"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Award size={18} />
                  </span>
                  <span>My Placement</span>
                </NavLink>
              </li>
            </>
          )}

          {role === 'ROLE_COMPANY' && (
            <>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/company/dashboard"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <LayoutDashboard size={18} />
                  </span>
                  <span>Dashboard</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/company/jobs"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Briefcase size={18} />
                  </span>
                  <span>Manage Jobs</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/company/applicants"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Users size={18} />
                  </span>
                  <span>Applicants</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/company/profile"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Building2 size={18} />
                  </span>
                  <span>Company Profile</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/company/interviews"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <CalendarDays size={18} />
                  </span>
                  <span>Interviews</span>
                </NavLink>
              </li>
            </>
          )}

          {role === 'ROLE_ADMIN' && (
            <>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/dashboard"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <LayoutDashboard size={18} />
                  </span>
                  <span>Admin Dashboard</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/students"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <GraduationCap size={18} />
                  </span>
                  <span>Students</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/companies"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Building2 size={18} />
                  </span>
                  <span>Companies</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/jobs"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Briefcase size={18} />
                  </span>
                  <span>Jobs</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/applications"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <FileText size={18} />
                  </span>
                  <span>Applications</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/placements"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <Award size={18} />
                  </span>
                  <span>Placements</span>
                </NavLink>
              </li>
              <li className="sidebar-menu-item">
                <NavLink
                  to="/admin/interviews"
                  className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <span className="sidebar-link-icon">
                    <CalendarDays size={18} />
                  </span>
                  <span>Interviews</span>
                </NavLink>
              </li>
            </>
          )}
        </ul>

        {/* General Links */}
        <div className="sidebar-section-title" style={{ marginTop: '1.5rem' }}>
          Portal
        </div>
        <ul className="sidebar-menu">
          <li className="sidebar-menu-item">
            <NavLink
              to="/"
              end
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
              onClick={onClose}
            >
              <span className="sidebar-link-icon">
                <Home size={18} />
              </span>
              <span>Public Overview</span>
            </NavLink>
          </li>
        </ul>
      </nav>

      <div className="sidebar-footer">
        Placement Management System &copy; {new Date().getFullYear()}
      </div>
    </aside>
  );
}

export default Sidebar;
