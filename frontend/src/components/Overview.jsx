import { PanelTitle } from './Brand.jsx'

export function Overview({ analytics, organizations, projects, openTasks }) {
  const stats = analytics
    ? [['Members', analytics.totalMembers], ['Projects', analytics.totalProjects], ['Open tasks', analytics.openTasks], ['Completed', analytics.completedTasks]]
    : [['Organizations', organizations.length], ['Projects', projects.length], ['Open tasks', openTasks], ['Status', 'Live']]
  return (
    <>
      <section className="metrics-grid">
        {stats.map(([label, value]) => (
          <article className="metric-card" key={label}>
            <span className="metric-label">{label}</span>
            <strong>{value}</strong>
            <small className="positive">From API</small>
          </article>
        ))}
      </section>
      <section className="panel">
        <PanelTitle title="Workspace summary" />
        <p>Select an organization to load projects, create tasks, request AI guidance, and review analytics.</p>
      </section>
    </>
  )
}
