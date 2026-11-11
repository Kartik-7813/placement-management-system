import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import DashboardLayout from './layouts/DashboardLayout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import StudentDashboardPage from './pages/StudentDashboardPage';
import JobsPage from './pages/JobsPage';
import JobDetailsPage from './pages/JobDetailsPage';
import MyApplicationsPage from './pages/MyApplicationsPage';
import ApplicationDetailsPage from './pages/ApplicationDetailsPage';
import StudentInterviewsPage from './pages/StudentInterviewsPage';
import InterviewManagementPage from './pages/InterviewManagementPage';
import CompanyDashboardPage from './pages/CompanyDashboardPage';
import CompanyJobsPage from './pages/CompanyJobsPage';
import CompanyProfilePage from './pages/CompanyProfilePage';
import CompanyApplicantsPage from './pages/CompanyApplicantsPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminStudentsPage from './pages/AdminStudentsPage';
import AdminCompaniesPage from './pages/AdminCompaniesPage';
import AdminJobsPage from './pages/AdminJobsPage';
import AdminApplicationsPage from './pages/AdminApplicationsPage';
import AdminPlacementsPage from './pages/AdminPlacementsPage';
import StudentPlacementPage from './pages/StudentPlacementPage';
import NotFoundPage from './pages/NotFoundPage';
import ProtectedRoute from './components/ProtectedRoute';

/**
 * App Component
 * Configures application-wide AuthProvider, public routes, and role-protected dashboard & job routes.
 */
function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Portal Routes */}
          <Route path="/" element={<MainLayout />}>
            <Route index element={<HomePage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            {/* General /dashboard redirector */}
            <Route
              path="dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          {/* Authenticated SaaS Dashboard Routes (Left Sidebar + Header Layout) */}
          <Route
            element={
              <ProtectedRoute>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            {/* Student Routes */}
            <Route
              path="student/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <StudentDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/jobs"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <JobsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/jobs/:jobId"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <JobDetailsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/applications"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <MyApplicationsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/applications/:applicationId"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <ApplicationDetailsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/interviews"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <StudentInterviewsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="student/placement"
              element={
                <ProtectedRoute allowedRoles={['ROLE_STUDENT']}>
                  <StudentPlacementPage />
                </ProtectedRoute>
              }
            />

            {/* Company Routes */}
            <Route
              path="company/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <CompanyDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="company/jobs"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <CompanyJobsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="company/applicants"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <CompanyApplicantsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="company/applications/:applicationId"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <ApplicationDetailsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="company/profile"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <CompanyProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="company/interviews"
              element={
                <ProtectedRoute allowedRoles={['ROLE_COMPANY']}>
                  <InterviewManagementPage />
                </ProtectedRoute>
              }
            />

            {/* Administrator Routes */}
            <Route
              path="admin/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/students"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminStudentsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/companies"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminCompaniesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/jobs"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminJobsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/applications"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminApplicationsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/placements"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <AdminPlacementsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/interviews"
              element={
                <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                  <InterviewManagementPage />
                </ProtectedRoute>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
