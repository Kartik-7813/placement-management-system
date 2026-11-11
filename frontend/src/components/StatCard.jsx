/**
 * StatCard Component
 * Displays a clean statistical metric card with label, value, context, and icon.
 *
 * @param {Object} props
 * @param {string} props.label - Metric name/title
 * @param {string|number} props.value - Metric figure/count
 * @param {string} [props.context] - Supporting context or description
 * @param {string} [props.icon] - Visual icon or emoji
 * @param {string} [props.className=''] - Additional CSS classes
 */
function StatCard({ label, value, context, icon, className = '' }) {
  return (
    <div className={`stat-card ${className}`.trim()}>
      <div className="stat-card-header">
        <span className="stat-card-label">{label}</span>
        {icon && <span className="stat-card-icon">{icon}</span>}
      </div>
      <div className="stat-card-value">{value ?? '—'}</div>
      {context && <div className="stat-card-context">{context}</div>}
    </div>
  );
}

export default StatCard;
