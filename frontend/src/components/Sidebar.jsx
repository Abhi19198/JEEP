import { NavLink } from 'react-router-dom'
import { Brand } from './Brand.jsx'

const VIEWS = [
  { to: '/', label: 'Overview', end: true },
  { to: '/organizations', label: 'Organizations' },
  { to: '/projects', label: 'Projects' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/teams', label: 'Teams' },
  { to: '/repositories', label: 'Repositories' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/ai-assistant', label: 'AI Assistant' },
  { to: '/reports', label: 'Reports' },
]

export function Sidebar({ session, onLogout }) {
  return (
    <aside className="sidebar">
      <Brand />
      <nav className="nav">
        {VIEWS.map((view) => (
          <NavLink key={view.to} to={view.to} end={view.end} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            {view.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-card">
        <p className="eyebrow">Signed in</p>
        <h3>{session.role}</h3>
        <span>{session.email}</span>
        <button type="button" className="logout-btn" onClick={onLogout}>Logout</button>
      </div>
    </aside>
  )
}
