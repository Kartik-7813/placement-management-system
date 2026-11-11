import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';

/**
 * NotFoundPage Component
 * Rendered when the browser navigates to an unmatched client route.
 */
function NotFoundPage() {
  return (
    <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', maxWidth: '500px', margin: '3rem auto' }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
        <Compass size={48} strokeWidth={1.5} color="var(--primary-color)" />
      </div>
      <h1 style={{ fontSize: '2.5rem', color: 'var(--primary-color)', marginBottom: '0.5rem', lineHeight: 1 }}>404</h1>
      <h2 style={{ marginBottom: '1rem', fontSize: '1.25rem' }}>Page Not Found</h2>
      <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
        The page you are looking for does not exist or has been moved.
      </p>
      <Link to="/" className="btn btn-primary">
        Return to Home
      </Link>
    </div>
  );
}

export default NotFoundPage;
