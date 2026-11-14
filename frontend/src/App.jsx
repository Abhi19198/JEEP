import { useEffect, useState } from 'react'
import './App.css'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const SESSION_KEY = 'jeep-session'
const emptyAuth = { firstName: '', lastName: '', email: '', password: '', confirmPassword: '' }
const emptyOrganization = { name: '', description: '' }
const emptyProject = { name: '', description: '' }
const emptyTask = { title: '', description: '', priority: 'MEDIUM', status: 'TODO' }

async function requestJson(path, options = {}) {
  const { token, _retried, headers: extraHeaders, ...fetchOptions } = options
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(extraHeaders || {}) },
    ...fetchOptions,
  })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : null
  if (!response.ok) {
    const error = new Error(payload?.message || payload?.error || 'Request failed')
    error.status = response.status
    throw error
  }
  return payload
}

function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem(SESSION_KEY) || 'null'))
  const [isLogin, setIsLogin] = useState(true)
  const [authForm, setAuthForm] = useState(emptyAuth)
  const [activeView, setActiveView] = useState('Overview')
  const [organizations, setOrganizations] = useState([])
  const [projects, setProjects] = useState([])
  const [tasks, setTasks] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [selectedOrganization, setSelectedOrganization] = useState('')
  const [selectedProject, setSelectedProject] = useState('')
  const [organizationForm, setOrganizationForm] = useState(emptyOrganization)
  const [projectForm, setProjectForm] = useState(emptyProject)
  const [taskForm, setTaskForm] = useState(emptyTask)
  const [aiForm, setAiForm] = useState({ prompt: '', type: 'risk_review' })
  const [aiResponse, setAiResponse] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Authenticated requests go through here: on a 401 the access token is
  // refreshed once with the stored refresh token and the request is retried.
  // If the refresh also fails, the session is expired and the user is logged out.
  const apiRequest = async (path, options = {}) => {
    try {
      return await requestJson(path, { ...options, token: options.token || session?.token })
    } catch (reason) {
      if (reason.status !== 401 || !session?.refreshToken || options._retried) throw reason
      try {
        const refreshed = await requestJson('/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: session.refreshToken }),
        })
        const nextSession = { ...session, token: refreshed.accessToken, refreshToken: refreshed.refreshToken || session.refreshToken }
        localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))
        setSession(nextSession)
        return await requestJson(path, { ...options, token: nextSession.token, _retried: true })
      } catch (refreshReason) {
        logout()
        throw refreshReason
      }
    }
  }

  const loadOrganizations = async (token = session?.token) => {
    if (!token) return
    const result = await apiRequest('/organizations', { token })
    setOrganizations(result || [])
    if (!selectedOrganization && result?.[0]) setSelectedOrganization(result[0].id)
  }

  const loadProjects = async (organizationId = selectedOrganization) => {
    if (!organizationId || !session?.token) return
    const result = await apiRequest(`/projects/organization/${organizationId}`, { token: session.token })
    setProjects(result || [])
    setSelectedProject(result?.[0]?.id || '')
  }

  const loadTasks = async (projectId = selectedProject) => {
    if (!projectId || !session?.token) return
    setTasks(await apiRequest(`/tasks/project/${projectId}`, { token: session.token }))
  }

  useEffect(() => {
    if (session) loadOrganizations().catch((reason) => setError(reason.message))
  }, [session])

  useEffect(() => {
    if (selectedOrganization) loadProjects().catch((reason) => setError(reason.message))
  }, [selectedOrganization])

  useEffect(() => {
    if (selectedProject) loadTasks().catch((reason) => setError(reason.message))
    else setTasks([])
  }, [selectedProject])

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

  const submitResource = async (path, body, afterSave) => {
    setLoading(true)
    setError('')
    try {
      await apiRequest(path, { method: 'POST', token: session.token, body: JSON.stringify(body) })
      await afterSave()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const createOrganization = (event) => {
    event.preventDefault()
    submitResource('/organizations', organizationForm, async () => {
      setOrganizationForm(emptyOrganization)
      await loadOrganizations()
    })
  }

  const createProject = (event) => {
    event.preventDefault()
    submitResource('/projects', { ...projectForm, organizationId: selectedOrganization }, async () => {
      setProjectForm(emptyProject)
      await loadProjects()
    })
  }

  const createTask = (event) => {
    event.preventDefault()
    submitResource('/tasks', { ...taskForm, projectId: selectedProject }, async () => {
      setTaskForm(emptyTask)
      await loadTasks()
    })
  }

  const loadReport = async () => {
    if (!selectedOrganization) return
    setLoading(true)
    try {
      setAnalytics(await apiRequest(`/analytics/organizations/${selectedOrganization}`, { token: session.token }))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const getSuggestion = async (event) => {
    event.preventDefault()
    setLoading(true)
    try {
      setAiResponse(await apiRequest('/ai/suggest', { method: 'POST', token: session.token, body: JSON.stringify({ ...aiForm, projectId: selectedProject }) }))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    setOrganizations([])
    setProjects([])
    setTasks([])
  }

  if (!session) {
    return <div className="auth-shell"><div className="auth-card">
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
  }

  const openTasks = tasks.filter((task) => !['DONE', 'CANCELLED'].includes(task.status)).length
  const views = ['Overview', 'Organizations', 'Projects', 'Tasks', 'AI Assistant', 'Reports']

  return <div className="dashboard-shell">
    <aside className="sidebar"><Brand /><nav className="nav">{views.map((view) => <button key={view} className={`nav-item ${activeView === view ? 'active' : ''}`} onClick={() => setActiveView(view)}>{view}</button>)}</nav><div className="sidebar-card"><p className="eyebrow">Signed in</p><h3>{session.role}</h3><span>{session.email}</span><button type="button" className="logout-btn" onClick={logout}>Logout</button></div></aside>
    <main className="main-panel"><header className="topbar"><div><p className="eyebrow">JEEP workspace</p><h1>{activeView}</h1></div>{activeView === 'Reports' && <button className="primary-btn" onClick={loadReport} disabled={loading || !selectedOrganization}>Refresh report</button>}</header>
      {error && <div className="error-box">{error}</div>}
      {activeView === 'Overview' && <Overview analytics={analytics} organizations={organizations} projects={projects} openTasks={openTasks} />}
      {activeView === 'Reports' && <><Overview analytics={analytics} organizations={organizations} projects={projects} openTasks={openTasks} /><section className="panel"><PanelTitle title="Analytics" /><p>Select an organization and refresh the report to load live counts.</p><select value={selectedOrganization} onChange={(event) => setSelectedOrganization(event.target.value)}><option value="">Choose organization</option>{organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}</select></section></>}
      {activeView === 'Organizations' && <section className="panel"><PanelTitle title="Organizations" /><form className="inline-form" onSubmit={createOrganization}><input name="name" placeholder="Organization name" value={organizationForm.name} onChange={updateForm(setOrganizationForm)} required /><input name="description" placeholder="Description" value={organizationForm.description} onChange={updateForm(setOrganizationForm)} /><button className="primary-btn" disabled={loading}>Create</button></form><div className="org-list">{organizations.map((org) => <button className={`org-item ${selectedOrganization === org.id ? 'selected' : ''}`} key={org.id} onClick={() => setSelectedOrganization(org.id)}><strong>{org.name}</strong><span>{org.description || 'No description provided'}</span></button>)}</div></section>}
      {activeView === 'Projects' && <section className="panel"><PanelTitle title="Projects" /><select value={selectedOrganization} onChange={(event) => setSelectedOrganization(event.target.value)}><option value="">Choose organization</option>{organizations.map((org) => <option key={org.id} value={org.id}>{org.name}</option>)}</select><form className="inline-form" onSubmit={createProject}><input name="name" placeholder="Project name" value={projectForm.name} onChange={updateForm(setProjectForm)} required /><input name="description" placeholder="Description" value={projectForm.description} onChange={updateForm(setProjectForm)} /><button className="primary-btn" disabled={loading || !selectedOrganization}>Create</button></form><div className="org-list">{projects.map((project) => <button className={`org-item ${selectedProject === project.id ? 'selected' : ''}`} key={project.id} onClick={() => setSelectedProject(project.id)}><strong>{project.name}</strong><span>{project.status} · {project.description || 'No description provided'}</span></button>)}</div></section>}
      {activeView === 'Tasks' && <section className="panel"><PanelTitle title="Tasks" /><select value={selectedProject} onChange={(event) => setSelectedProject(event.target.value)}><option value="">Choose project</option>{projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select><form className="inline-form" onSubmit={createTask}><input name="title" placeholder="Task title" value={taskForm.title} onChange={updateForm(setTaskForm)} required /><select name="priority" value={taskForm.priority} onChange={updateForm(setTaskForm)}><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select><select name="status" value={taskForm.status} onChange={updateForm(setTaskForm)}><option>TODO</option><option>IN_PROGRESS</option><option>REVIEW</option><option>DONE</option></select><button className="primary-btn" disabled={loading || !selectedProject}>Create</button></form><div className="org-list">{tasks.map((task) => <div className="org-item" key={task.id}><strong>{task.title}</strong><span>{task.priority} · {task.status}{task.description ? ` · ${task.description}` : ''}</span></div>)}</div></section>}
      {activeView === 'AI Assistant' && <section className="panel"><PanelTitle title="AI Assistant" /><select value={selectedProject} onChange={(event) => setSelectedProject(event.target.value)}><option value="">Choose project</option>{projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select><form className="auth-form" onSubmit={getSuggestion}><label>Suggestion type<input name="type" value={aiForm.type} onChange={updateForm(setAiForm)} required /></label><label>Prompt<textarea name="prompt" value={aiForm.prompt} onChange={updateForm(setAiForm)} required placeholder="Describe the project risk or decision" /></label><button className="primary-btn" disabled={loading || !selectedProject}>Generate suggestion</button></form>{aiResponse && <div className="ai-card"><p>{aiResponse.suggestion}</p><div className="ai-stats"><div><strong>{Math.round(aiResponse.confidence * 100)}%</strong><span>confidence</span></div><div><strong>{aiResponse.type}</strong><span>type</span></div></div></div>}</section>}
    </main>
  </div>
}

function Brand() { return <div className="brand-block"><div className="brand-mark">J</div><div><p className="eyebrow">Enterprise</p><h2>JEEP</h2></div></div> }
function PanelTitle({ title }) { return <div className="panel-header"><h3>{title}</h3><span className="chip neutral">Live data</span></div> }
function Overview({ analytics, organizations, projects, openTasks }) {
  const stats = analytics ? [['Members', analytics.totalMembers], ['Projects', analytics.totalProjects], ['Open tasks', analytics.openTasks], ['Completed', analytics.completedTasks]] : [['Organizations', organizations.length], ['Projects', projects.length], ['Open tasks', openTasks], ['Status', 'Live']]
  return <><section className="metrics-grid">{stats.map(([label, value]) => <article className="metric-card" key={label}><span className="metric-label">{label}</span><strong>{value}</strong><small className="positive">From API</small></article>)}</section><section className="panel"><PanelTitle title="Workspace summary" /><p>Select an organization to load projects, create tasks, request AI guidance, and review analytics.</p></section></>
}

export default App
