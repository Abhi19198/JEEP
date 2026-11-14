import { PanelTitle } from '../components/Brand.jsx'
import { Overview } from '../components/Overview.jsx'

export function ReportsPage({ ws }) {
  const { analytics, organizations, projects, selectedOrganization, setSelectedOrganization, loadReport } = ws
  const openTasks = analytics ? analytics.openTasks : 0

  return (
    <>
      <div className="topbar-action">
        <button className="primary-btn" onClick={loadReport} disabled={ws.loading || !selectedOrganization}>Refresh report</button>
      </div>
      <Overview analytics={analytics} organizations={organizations} projects={projects} openTasks={openTasks} />
      <section className="panel">
        <PanelTitle title="Analytics" />
        <p>Select an organization and refresh the report to load live counts.</p>
        <select value={selectedOrganization} onChange={(event) => setSelectedOrganization(event.target.value)}>
          <option value="">Choose organization</option>
          {organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}
        </select>
      </section>
    </>
  )
}
