import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Award, GraduationCap, Briefcase, FileText, CheckCircle2, RotateCw } from 'lucide-react';
import useAuth from '../hooks/useAuth';
import {
  getPlacementByStudent,
  getPlacementHistoryByStudent,
} from '../services/placementService';
import { resolveCurrentStudentId } from '../services/studentService';
import PageHeader from '../components/PageHeader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Button from '../components/Button';

/**
 * StudentPlacementPage Component
 * Provides authenticated students with read-only visibility into their official
 * placement record, confirmed offer package, tentative joining date, and placement history.
 * Strictly verifies ownership on backend (GET /api/placements/student/{id}).
 */
function StudentPlacementPage() {
  const { user } = useAuth();
  const [placement, setPlacement] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [hasNoPlacement, setHasNoPlacement] = useState(false);

  const loadPlacementData = async () => {
    setLoading(true);
    setError(null);
    setHasNoPlacement(false);

    try {
      const studentId = await resolveCurrentStudentId();
      if (!studentId) {
        setHasNoPlacement(true);
        setLoading(false);
        return;
      }

      // Fetch active or most recent placement
      try {
        const activePlacement = await getPlacementByStudent(studentId);
        setPlacement(activePlacement);
      } catch (pErr) {
        if (pErr.response?.status === 404) {
          setHasNoPlacement(true);
        } else {
          throw pErr;
        }
      }

      // Fetch placement history
      try {
        const historyList = await getPlacementHistoryByStudent(studentId);
        setHistory(historyList);
      } catch (hErr) {
        console.warn('Failed to load placement history:', hErr);
      }
    } catch (err) {
      console.error('Failed to load student placement:', err);
      setError(
        err.response?.data?.message || err.message || 'Failed to retrieve your placement record.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPlacementData();
  }, [user?.email]);

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span
            className="badge-status"
            style={{ backgroundColor: '#fef3c7', color: '#92400e', fontWeight: 600 }}
          >
            CONFIRMED OFFER
          </span>
        );
      case 'JOINED':
        return (
          <span
            className="badge-status"
            style={{ backgroundColor: '#dcfce7', color: '#166534', fontWeight: 600, display: 'inline-flex', alignItems: 'center' }}
          >
            <CheckCircle2 size={12} className="inline-icon" />
            <span>ONBOARDED / JOINED</span>
          </span>
        );
      case 'CANCELLED':
        return (
          <span
            className="badge-status"
            style={{ backgroundColor: '#fee2e2', color: '#991b1b', fontWeight: 600 }}
          >
            CANCELLED
          </span>
        );
      default:
        return <span className="badge badge-info">{status}</span>;
    }
  };

  if (loading) {
    return <LoadingState message="Loading your official placement and offer record..." />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadPlacementData} />;
  }

  if (hasNoPlacement && !placement) {
    return (
      <div>
        <PageHeader
          title="My Placement & Offer Status"
          subtitle="Official campus placement records, offer details, and onboarded roles"
        />
        <EmptyState
          title="No Confirmed Placement Yet"
          message="You do not have any confirmed placement records on file. Continue tracking your submitted applications, attending interview evaluations, and exploring active recruitment drives."
          icon={<GraduationCap size={40} strokeWidth={1.5} />}
          action={
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem', justifyContent: 'center' }}>
              <Link to="/student/jobs" className="btn btn-primary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
                <Briefcase size={15} className="inline-icon" />
                <span>Browse Active Drives &rarr;</span>
              </Link>
              <Link to="/student/applications" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
                <FileText size={15} className="inline-icon" />
                <span>My Applications</span>
              </Link>
            </div>
          }
        />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="My Placement & Offer Status"
        subtitle="Official campus placement records, offer details, and onboarded roles"
        actions={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <Link to="/student/applications" className="btn btn-secondary" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center' }}>
              <FileText size={15} className="inline-icon" />
              <span>My Applications</span>
            </Link>
            <Button
              type="button"
              variant="secondary"
              onClick={loadPlacementData}
              style={{ display: 'inline-flex', alignItems: 'center' }}
            >
              <RotateCw size={14} className="inline-icon" />
              <span>Refresh</span>
            </Button>
          </div>
        }
      />

      {/* Primary Confirmed Placement Card */}
      {placement && (
        <div
          className="card"
          style={{
            marginBottom: '1.75rem',
            padding: '1.5rem',
            borderLeft: '5px solid #16a34a',
            backgroundColor: '#f0fdf4',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.35rem' }}>
                <Award size={28} color="#16a34a" />
                <h2 style={{ fontSize: '1.375rem', fontWeight: 700, color: 'var(--secondary-color)', margin: 0 }}>
                  Confirmed Placement Offer
                </h2>
              </div>
              <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                Placement Record <code>#{placement.placementId}</code> &bull; Student: <strong>{placement.studentName}</strong>
              </p>
            </div>
            <div>{renderStatusBadge(placement.status)}</div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              backgroundColor: '#ffffff',
              border: '1px solid #bbf7d0',
              borderRadius: 'var(--radius-sm)',
              padding: '1.25rem',
              marginBottom: '1.25rem',
            }}
          >
            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Placed Job Role
              </span>
              <strong style={{ fontSize: '1.125rem', color: 'var(--secondary-color)' }}>
                {placement.jobRole}
              </strong>
            </div>

            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Compensation Package
              </span>
              <strong style={{ fontSize: '1.25rem', color: '#15803d' }}>
                {placement.packageAmount != null ? `₹${placement.packageAmount} LPA` : 'As per policy'}
              </strong>
            </div>

            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Tentative Joining Date
              </span>
              <strong style={{ fontSize: '0.9375rem' }}>
                {placement.joiningDate ? new Date(placement.joiningDate).toLocaleDateString() : 'To be communicated'}
              </strong>
            </div>

            <div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Confirmation Date
              </span>
              <strong style={{ fontSize: '0.9375rem' }}>
                {placement.placedOn ? new Date(placement.placedOn).toLocaleDateString() : '—'}
              </strong>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Link
              to={`/student/applications/${placement.applicationId}`}
              className="btn btn-primary"
              style={{ textDecoration: 'none', fontSize: '0.8125rem', display: 'inline-flex', alignItems: 'center' }}
            >
              <FileText size={14} className="inline-icon" />
              <span>View Application #{placement.applicationId} &rarr;</span>
            </Link>
          </div>
        </div>
      )}

      {/* Placement History */}
      {history.length > 1 && (
        <div className="card" style={{ padding: '1.25rem' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--secondary-color)', margin: '0 0 1rem' }}>
            Placement History ({history.length})
          </h3>

          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ margin: 0, width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left', borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>ID</th>
                  <th style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Role</th>
                  <th style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Package</th>
                  <th style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Joining Date</th>
                  <th style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {history.map((h) => (
                  <tr key={h.placementId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem' }}>#{h.placementId}</td>
                    <td style={{ padding: '0.65rem 0.875rem', fontSize: '0.875rem', fontWeight: 500 }}>{h.jobRole}</td>
                    <td style={{ padding: '0.65rem 0.875rem', fontSize: '0.875rem', color: '#166534', fontWeight: 600 }}>
                      {h.packageAmount != null ? `₹${h.packageAmount} LPA` : '—'}
                    </td>
                    <td style={{ padding: '0.65rem 0.875rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {h.joiningDate ? new Date(h.joiningDate).toLocaleDateString() : '—'}
                    </td>
                    <td style={{ padding: '0.65rem 0.875rem' }}>{renderStatusBadge(h.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

export default StudentPlacementPage;
