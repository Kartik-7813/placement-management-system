/**
 * PageHeader Component
 * Consistent header for dashboard views featuring page title, subtitle, and action buttons.
 *
 * @param {Object} props
 * @param {string} props.title - Main page title
 * @param {string} [props.subtitle] - Supporting description or context
 * @param {React.ReactNode} [props.actions] - Action buttons or controls
 */
function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header-container">
      <div>
        <h1 className="page-header-title">{title}</h1>
        {subtitle && <p className="page-header-subtitle">{subtitle}</p>}
      </div>
      {actions && <div>{actions}</div>}
    </div>
  );
}

export default PageHeader;
