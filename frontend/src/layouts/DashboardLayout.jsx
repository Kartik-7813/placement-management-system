import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';

/**
 * DashboardLayout Component
 * Master SaaS layout wrapping authenticated application views with a sticky sidebar,
 * responsive mobile navigation drawer, and top header.
 */
function DashboardLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const toggleSidebar = () => {
    setIsSidebarOpen((prev) => !prev);
  };

  const closeSidebar = () => {
    setIsSidebarOpen(false);
  };

  // Close mobile drawer on Escape key press
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && isSidebarOpen) {
        closeSidebar();
      }
    };

    if (isSidebarOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isSidebarOpen]);

  // Automatically close mobile drawer if window is resized to desktop width (>= 1024px)
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024 && isSidebarOpen) {
        setIsSidebarOpen(false);
      }
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [isSidebarOpen]);

  return (
    <div className="dashboard-layout">
      {/* Mobile Drawer Overlay */}
      <div
        className={`sidebar-overlay ${isSidebarOpen ? 'active' : ''}`}
        onClick={closeSidebar}
        aria-hidden="true"
      />

      {/* Sidebar Navigation */}
      <Sidebar isOpen={isSidebarOpen} onClose={closeSidebar} />

      {/* Main Content Viewport */}
      <div className="dashboard-main-wrapper">
        <Header onToggleSidebar={toggleSidebar} />
        <main className="dashboard-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default DashboardLayout;
