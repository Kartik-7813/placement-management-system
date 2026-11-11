import { FolderSearch } from 'lucide-react';

/**
 * EmptyState Component
 * Displays a clean, professional empty state when no records are available.
 *
 * @param {Object} props
 * @param {string} [props.title='No Records Found'] - Empty state heading
 * @param {string} props.message - Descriptive text explaining why no data is shown
 * @param {React.ReactNode} [props.icon] - Visual icon component or element
 * @param {React.ReactNode} [props.action] - Optional action button
 */
function EmptyState({
  title = 'No Records Found',
  message,
  icon,
  action,
}) {
  const renderedIcon = icon || <FolderSearch size={40} strokeWidth={1.5} color="var(--text-muted)" />;

  return (
    <div className="state-container">
      <div className="state-icon">{renderedIcon}</div>
      <h3 className="state-title">{title}</h3>
      {message && <p className="state-description">{message}</p>}
      {action && <div>{action}</div>}
    </div>
  );
}

export default EmptyState;
