import { useState, useEffect } from 'react';
import api from '../services/api';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * HomePage Component
 * Displays frontend setup verification, live backend connectivity status,
 * and an overview of audited backend REST API modules.
 */
function HomePage() {
  const [healthStatus, setHealthStatus] = useState('checking');
  const [healthData, setHealthData] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  const checkBackendHealth = async () => {
    setHealthStatus('checking');
    setErrorMessage(null);
    try {
      const response = await api.get(API_ENDPOINTS.ACTUATOR.HEALTH);
      setHealthData(response.data);
      setHealthStatus('connected');
    } catch (err) {
      setHealthStatus('disconnected');
      setErrorMessage(
        err.response?.data?.message || err.message || 'Unable to connect to backend server'
      );
    }
  };

  useEffect(() => {
    checkBackendHealth();
  }, []);

  return (
    <div>
      <div className="card">
        <h1 className="card-title" style={{ fontSize: '1.75rem', marginBottom: '0.5rem' }}>
          Placement Management System — Frontend
        </h1>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>
          React + Vite + React Router + Axios frontend foundation connected to Spring Boot 4 REST API.
        </p>

        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div>
            <strong>Backend API URL:</strong> <code>{apiBaseUrl}</code>
          </div>
          <div>
            <strong>Status:</strong>{' '}
            {healthStatus === 'checking' && <span className="badge badge-warning">Checking Connectivity...</span>}
            {healthStatus === 'connected' && (
              <span className="badge badge-success">Backend Connected ({healthData?.status || 'UP'})</span>
            )}
            {healthStatus === 'disconnected' && (
              <span className="badge badge-warning">Backend Offline / Awaiting Start</span>
            )}
          </div>
          <button className="btn btn-primary" onClick={checkBackendHealth} style={{ marginLeft: 'auto' }}>
            Recheck Connection
          </button>
        </div>

        {errorMessage && (
          <div style={{ marginTop: '0.75rem', fontSize: '0.875rem', color: 'var(--danger-color)' }}>
            <em>Note: {errorMessage} (Ensure the Spring Boot backend is running on {apiBaseUrl})</em>
          </div>
        )}
      </div>

      <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem', color: 'var(--secondary-color)' }}>
        Audited Backend Modules
      </h2>

      <div className="grid-2">
        <div className="card">
          <h3 className="card-title">1. Authentication Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Stateless JWT authentication with role-based authorization.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>POST /api/auth/login</code> — Public user login</li>
            <li><code>POST /api/auth/register</code> — Public student registration</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">2. Students Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Student profile management, academic records, and skill assignments.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>GET /api/students</code> — Paginated student listings (ADMIN, COMPANY)</li>
            <li><code>GET /api/students/{'{id}'}</code> — Student profile details</li>
            <li><code>GET /api/students/{'{id}'}/skills</code> — Student skill associations</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">3. Companies Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Recruiter profiles, company registrations, and verified contact points.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>GET /api/companies</code> — Public paginated company listings</li>
            <li><code>GET /api/companies/{'{id}'}</code> — Public company details</li>
            <li><code>POST /api/companies</code> — Register company (ADMIN)</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">4. Jobs Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Placement job openings, eligibility criteria, and application deadlines.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>GET /api/jobs</code> — Public paginated job openings</li>
            <li><code>GET /api/jobs/{'{id}'}</code> — Public job details</li>
            <li><code>POST /api/jobs</code> — Post new job opening (ADMIN, COMPANY)</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">5. Applications Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Application tracking workflow: APPLIED &rarr; SHORTLISTED &rarr; INTERVIEWING &rarr; SELECTED / REJECTED.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>POST /api/applications</code> — Submit application (STUDENT)</li>
            <li><code>GET /api/applications</code> — Paginated applications</li>
            <li><code>PUT /api/applications/{'{id}'}/status</code> — Update status</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">6. Interviews Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Multi-round interview scheduling, interviewer tracking, and round results.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>POST /api/interviews</code> — Schedule round (ADMIN, COMPANY)</li>
            <li><code>GET /api/interviews/application/{'{id}'}</code> — Interview history</li>
            <li><code>PUT /api/interviews/{'{id}'}</code> — Record feedback and results</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">7. Placements Module</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Confirmed placement offers, salary packages, and joining confirmations.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>GET /api/placements</code> — All placements (ADMIN, COMPANY)</li>
            <li><code>GET /api/placements/student/{'{id}'}</code> — Student placement record</li>
            <li><code>POST /api/placements</code> — Create placement (ADMIN)</li>
          </ul>
        </div>

        <div className="card">
          <h3 className="card-title">8. Dashboard & Analytics</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
            Aggregated institutional statistics and administrative metrics.
          </p>
          <ul style={{ paddingLeft: '1.25rem', fontSize: '0.875rem' }}>
            <li><code>GET /api/dashboard/summary</code> — Aggregated statistics (ADMIN)</li>
            <li><code>GET /actuator/health</code> — Public system health & liveness probes</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default HomePage;
