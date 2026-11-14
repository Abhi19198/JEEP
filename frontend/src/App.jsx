import { useCallback, useEffect, useRef, useState } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import './App.css'
import { SESSION_KEY, createApiClient, requestJson } from './api.js'
import { Brand } from './components/Brand.jsx'
import { Overview } from './components/Overview.jsx'
import { Sidebar } from './components/Sidebar.jsx'
import { OrganizationsPage } from './pages/OrganizationsPage.jsx'
import { ProjectsPage } from './pages/ProjectsPage.jsx'
import { TasksPage } from './pages/TasksPage.jsx'
import { TeamsPage } from './pages/TeamsPage.jsx'
import { RepositoriesPage } from './pages/RepositoriesPage.jsx'
import { NotificationsPage } from './pages/NotificationsPage.jsx'
import { AiAssistantPage } from './pages/AiAssistantPage.jsx'
import { ReportsPage } from './pages/ReportsPage.jsx'

const emptyAuth = { firstName: '', lastName: '', email: '', password: '', confirmPassword: '' }
const TITLES = {
  '/': 'Overview',
  '/organizations': 'Organizations',
  '/projects': 'Projects',
  '/tasks': 'Tasks',
  '/teams': 'Teams',
  '/repositories': 'Repositories',
  '/notifications': 'Notifications',
  '/ai-assistant': 'AI Assistant',
  '/reports': 'Reports',
}

function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem(SESSION_KEY) || 'null'))
  const [isLogin, setIsLogin] = useState(true)
  const [authForm, setAuthForm] = useState(emptyAuth)
  const [organizations, setOrganizations] = useState([])
  const [projects, setProjects] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [selectedOrganization, setSelectedOrganization] = useState('')
  const [selectedProject, setSelectedProject] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const location = useLocation()

  // The API client closes over the current session via a ref so pages keep a
  // stable `api` function while refreshes still see the latest tokens.
  const sessionRef = useRef(session)
  sessionRef.current = session

  const logout = useCallback(() => {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    setOrganizations([])
    setProjects([])
    setAnalytics(null)
    setSelectedOrganization('')
    setSelectedProject('')
  }, [])

  const api = useCallback(createApiClient({
    getSession: () => sessionRef.current,
    onSessionRefreshed: (nextSession) => {
      localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))
      setSession(nextSession)
    },
    onSessionExpired: logout,
  }), [logout])

  const loadOrganizations = useCallback(async () => {
    if (!sessionRef.current?.token) return
    const result = await api('/organizations')
    setOrganizations(result || [])
    // Auto-select the first organization when nothing is selected, or when
    // the selected one is gone (e.g. just deleted).
    const current = selectedOrganizationRef.current
    if ((!current || !result?.some((org) => org.id === current)) && result?.[0]) setSelectedOrganization(result[0].id)
  }, [api])
  const selectedOrganizationRef = useRef(selectedOrganization)
  selectedOrganizationRef.current = selectedOrganization

  // Project dropdown list (all pages share it). Paginated lists live in the
  // Projects/Tasks pages; the dropdown fetches a large first page.
  const loadProjects = useCallback(async (organizationId = selectedOrganizationRef.current) => {
    if (!organizationId || !sessionRef.current?.token) return
    const result = await api(`/projects/organization/${organizationId}?page=0&size=100`)
    const items = Array.isArray(result) ? result : (result?.content || [])
    setProjects(items)
    setSelectedProject((current) => (items.some((project) => project.id === current) ? current : (items[0]?.id || '')))
  }, [api])

  useEffect(() => {
    if (session) loadOrganizations().catch((reason) => setError(reason.message))
  }, [session, loadOrganizations])

  useEffect(() => {
    if (selectedOrganization) loadProjects().catch((reason) => setError(reason.message))
    else { setProjects([]); setSelectedProject('') }
  }, [selectedOrganization, loadProjects])

  const updateForm = (setter) => (event) => setter((current) => ({ ...current, [event.target.name]: event.target.value }))

  const submitAuth = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const endpoint = isLogin ? '/auth/login' : '/auth/register'
      const payload = isLogin ? { email: authForm.email, password: authForm.password } : authForm
      const response = await requestJson(endpoint, { method: 'POST', body: JSON.stringify(payload) })
      const nextSession = { email: response.email, role: response.role, token: response.accessToken, refreshToken: response.refreshToken }
      localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))
      setSession(nextSession)
      setAuthForm(emptyAuth)
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const loadReport = async () => {
    if (!selectedOrganization) return
    setLoading(true)
    try {
      setAnalytics(await api(`/analytics/organizations/${selectedOrganization}`))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  if (!session) {
    return (
      <div className="auth-shell"><div className="auth-card">
        <Brand />
        <div className="auth-toggle"><button type="button" className={isLogin ? 'active' : ''} onClick={() => setIsLogin(true)}>Login</button><button type="button" className={!isLogin ? 'active' : ''} onClick={() => setIsLogin(false)}>Register</button></div>
        <form className="auth-form" onSubmit={submitAuth}>
          {!isLogin && <><label>First name<input name="firstName" value={authForm.firstName} onChange={updateForm(setAuthForm)} required /></label><label>Last name<input name="lastName" value={authForm.lastName} onChange={updateForm(setAuthForm)} required /></label></>}
          <label>Email<input type="email" name="email" value={authForm.email} onChange={updateForm(setAuthForm)} required /></label>
          <label>Password<input type="password" name="password" value={authForm.password} onChange={updateForm(setAuthForm)} required /></label>
          {!isLogin && <label>Confirm password<input type="password" name="confirmPassword" value={authForm.confirmPassword} onChange={updateForm(setAuthForm)} required /></label>}
          {error && <div className="error-box">{error}</div>}
          <button className="primary-btn auth-submit" type="submit" disabled={loading}>{loading ? 'Please wait...' : isLogin ? 'Login' : 'Create account'}</button>
        </form>
      </div></div>
    )
  }

  const ws = {
    api, session, organizations, selectedOrganization, setSelectedOrganization,
    projects, selectedProject, setSelectedProject, analytics, setAnalytics,
    loading, setLoading, error, setError,
    refreshOrganizations: loadOrganizations, refreshProjects: loadProjects, loadReport,
  }

  return (
    <div className="dashboard-shell">
      <Sidebar session={session} onLogout={logout} />
      <main className="main-panel">
        <header className="topbar">
          <div><p className="eyebrow">JEEP workspace</p><h1>{TITLES[location.pathname] || 'Overview'}</h1></div>
        </header>
        {error && <div className="error-box">{error}</div>}
        <Routes>
          <Route path="/" element={<Overview analytics={analytics} organizations={organizations} projects={projects} openTasks={analytics?.openTasks ?? '–'} />} />
          <Route path="/organizations" element={<OrganizationsPage ws={ws} />} />
          <Route path="/projects" element={<ProjectsPage ws={ws} />} />
          <Route path="/tasks" element={<TasksPage ws={ws} />} />
          <Route path="/teams" element={<TeamsPage ws={ws} />} />
          <Route path="/repositories" element={<RepositoriesPage ws={ws} />} />
          <Route path="/notifications" element={<NotificationsPage ws={ws} />} />
          <Route path="/ai-assistant" element={<AiAssistantPage ws={ws} />} />
          <Route path="/reports" element={<ReportsPage ws={ws} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
